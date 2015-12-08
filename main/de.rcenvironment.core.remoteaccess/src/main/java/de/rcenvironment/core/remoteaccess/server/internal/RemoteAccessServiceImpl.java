/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.embedded.ssh.api.EmbeddedSshServerControl;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;

/**
 * Provides "remote access" operations. TODO outline RA concept
 * 
 * @author Robert Mischke
 */
// TODO @7.0.0: remove duplicate javadoc
// TODO @7.0.0: use better exception class than WorkflowExecutionException
public class RemoteAccessServiceImpl implements RemoteAccessService {

    private static final String INTERFACE_ENDPOINT_NAME_INPUT = "input";

    private static final String INTERFACE_ENDPOINT_NAME_PARAMETERS = "parameters";

    private static final String INTERFACE_ENDPOINT_NAME_OUTPUT = "output";

    private static final String WF_PLACEHOLDER_PARAMETERS = "##RUNTIME_PARAMETERS##";

    private static final String WF_PLACEHOLDER_INPUT_DIR = "##RUNTIME_INPUT_DIRECTORY##";

    private static final String WF_PLACEHOLDER_OUTPUT_PARENT_DIR = "##RUNTIME_OUTPUT_DIRECTORY##";

    private static final String WF_PLACEHOLDER_OUTPUT_FILES_FOLDER_NAME = "##OUTPUT_FILES_FOLDER_NAME##";

    private static final String WORKFLOW_TEMPLATE_RESOURCE_PATH = "/resources/template.wf";

    private static final String WF_PLACEHOLDER_TOOL_ID = "##TOOL_ID##";

    private static final String WF_PLACEHOLDER_TOOL_VERSION = "##TOOL_VERSION##";

    private static final String WF_PLACEHOLDER_TOOL_NODE_ID = "##TOOL_NODE_ID##";

    private static final String WF_PLACEHOLDER_TIMESTAMP = "##TIMESTAMP##";

    private static final String WORKFLOW_FILE_ENCODING = "UTF-8";

    private static final String PUBLISHED_WF_DATA_FILE_SUFFIX = ".wf.dat";

    private static final String PUBLISHED_WF_PLACEHOLDER_FILE_SUFFIX = ".ph.dat";

    private static final String OUTPUT_INDENT = "    ";

    private final Log log = LogFactory.getLog(getClass());

    private final Map<String, String> publishedWorkflowTemplates = new HashMap<>();

    private final Map<String, String> publishedWorkflowTemplatePlaceholders = new HashMap<>();

    private final TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private ConfigurationService configurationService;

    private File publishedWfStorageDir;

    private EmbeddedSshServerControl embeddedSshServerControl;

    /**
     * Simple holder for execution parameters, including the workflow template file.
     * 
     * @author Robert Mischke
     */
    private static final class ExecutionSetup {

        private File workflowFile;

        private File placeholdersFile;

        private File inputFilesDir;

        private File outputFilesDir;

        public ExecutionSetup(File wfFile, File placeholdersFile, File inputFilesDir, File outputFilesDir) {
            this.workflowFile = wfFile;
            this.placeholdersFile = placeholdersFile;
            this.inputFilesDir = inputFilesDir;
            this.outputFilesDir = outputFilesDir;
        }

        public File getWorkflowFile() {
            return workflowFile;
        }

        public File getPlaceholderFile() {
            return placeholdersFile;
        }

        public File getInputFilesDir() {
            return inputFilesDir;
        }

        public File getOutputFilesDir() {
            return outputFilesDir;
        }

    }

    /**
     * Simple boolean holder with "yes/no" formatting.
     * 
     * @author Robert Mischke
     */
    private static final class MutableYesNoFlag {

        private boolean value;

        public boolean getValue() {
            return value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            if (value) {
                return "yes";
            } else {
                return "no";
            }
        }
    }

    /**
     * OSGi life-cycle method.
     */
    public void activate() {
        initAndRestoreFromPublishedWfStorage();
        embeddedSshServerControl.setAnnouncedVersionOrProperty("RA", RemoteAccessConstants.PROTOCOL_VERSION);
    }

    @Override
    public void printListOfAvailableTools(TextOutputReceiver outputReceiver, String format) {
        List<ComponentInstallation> components = getMatchingPublishedTools();

        if ("csv".equals(format)) {
            printComponentsListAsCsv(components, outputReceiver);
        } else if ("token-stream".equals(format)) {
            printComponentsListAsTokens(components, outputReceiver);
        } else {
            throw new IllegalArgumentException("Unrecognized output format: " + format);
        }
    }

    @Override
    public void printListOfAvailableWorkflows(TextOutputReceiver outputReceiver, String format) {
        if (!"token-stream".equals(format)) {
            throw new IllegalArgumentException("Unrecognized output format: " + format);
        }
        SortedSet<String> wfIds = new TreeSet<String>(publishedWorkflowTemplates.keySet());

        outputReceiver.addOutput(Integer.toString(wfIds.size())); // number of entries
        outputReceiver.addOutput("4"); // number of tokens per entry
        for (String publishId : wfIds) {
            // NOTE: the output format is made to match printListOfAvailableTools(); most fields are not used yet
            if (!checkIdOrVersionString(publishId)) {
                // for backwards compatility
                log.error("Not listing the previously published remote access workflow " + publishId
                    + "; the name contains characters that are not allowed anymore");
                continue;
            }
            outputReceiver.addOutput(publishId);
            // TODO apply filtering too when versions are added
            outputReceiver.addOutput("1"); // version; hardcoded for now
            outputReceiver.addOutput(""); // node id; not used yet
            outputReceiver.addOutput(""); // node id; not used yet
        }
    }

    /**
     * Creates a workflow file from an internal template and the given parameters, and executes it.
     * 
     * @param toolId the id of the integrated tool to run (see CommonToolIntegratorComponent)
     * @param toolVersion the version of the integrated tool to run
     * @param toolNodeId the node id of the instance to run the tool on; must NOT be null (resolve+validate this first)
     * @param parameterString an optional string containing tool-specific parameters
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    @Override
    public FinalWorkflowState runSingleToolWorkflow(String toolId, String toolVersion, String toolNodeId, String parameterString,
        File inputFilesDir, File outputFilesDir, SingleConsoleRowsProcessor consoleRowReceiver) throws IOException,
        WorkflowExecutionException {
        validateIdOrVersionString(toolId);
        validateIdOrVersionString(toolVersion);
        ExecutionSetup executionSetup =
            generateSingleToolExecutionSetup(toolId, toolVersion, toolNodeId, parameterString, inputFilesDir, outputFilesDir);
        return executeConfiguredWorkflow(executionSetup, consoleRowReceiver);
    }

    /**
     * Executes a previously published workflow template.
     * 
     * @param workflowId the id of the published workflow template
     * @param parameterString an optional string containing tool-specific parameters
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    @Override
    public FinalWorkflowState runPublishedWorkflowTemplate(String workflowId, String parameterString, File inputFilesDir,
        File outputFilesDir, SingleConsoleRowsProcessor consoleRowReceiver) throws IOException, WorkflowExecutionException {
        validateIdOrVersionString(workflowId);
        // TODO validate version once added
        ExecutionSetup executionSetup =
            generateWorkflowExecutionSetup(workflowId, parameterString, inputFilesDir, outputFilesDir);
        return executeConfiguredWorkflow(executionSetup, consoleRowReceiver);
    }

    /**
     * Checks if the given workflow file can be used with the "wf-run" console command, and if this check is positive, the workflow file is
     * published under the given id.
     * 
     * @param wfFile the workflow file
     * @param placeholdersFile TODO
     * @param publishId the id by which the workflow file should be made available
     * @param outputReceiver receiver for user feedback
     * @param persistent make the publishing persistent
     * @throws WorkflowExecutionException on failure to load/parse the workflow file
     */
    @Override
    public void checkAndPublishWorkflowFile(File wfFile, File placeholdersFile, String publishId, TextOutputReceiver outputReceiver,
        boolean persistent) throws WorkflowExecutionException {

        validateIdOrVersionString(publishId);

        WorkflowDescription wd;
        try {
            wd = workflowExecutionService.loadWorkflowDescriptionFromFileConsideringUpdates(wfFile,
                new HeadlessWorkflowDescriptionLoaderCallback(outputReceiver));
        } catch (WorkflowFileException e) { // review migration code, which was introduced due to changed exception type
            throw new WorkflowExecutionException("Failed to load workflow file: " + wfFile.getAbsolutePath(), e);
        }

        if (placeholdersFile != null) {
            try {
                workflowExecutionService.validatePlaceholdersFile(placeholdersFile);
            } catch (WorkflowFileException e) { // review migration code, which was introduced due to changed exception type
                throw new WorkflowExecutionException("Failed to validate placeholders file: " + wfFile.getAbsolutePath(), e);
            }
        }

        File workflowStorageFile = getWorkflowStorageFile(publishId);
        File placeholderStorageFile = getPlaceholderStorageFile(publishId);

        // sanity check / user accident prevention
        if (!persistent && workflowStorageFile.exists()) {
            throw new WorkflowExecutionException(
                "You are trying to overwrite a persistently published workflow with a temporary/transient one; "
                    + "if this is what you want to do, unpublish the old workflow first, then publish the new one again");
        }

        outputReceiver.addOutput(StringUtils.format("Checking workflow file \"%s\"", wfFile.getAbsolutePath()));
        if (validateWorkflowFileAsTemplate(wd, outputReceiver)) {
            try {
                String wfFileContent = readFile(wfFile);
                String replaced = publishedWorkflowTemplates.put(publishId, wfFileContent);
                // store wf file if told to persist
                if (persistent) {
                    FileUtils.writeStringToFile(workflowStorageFile, wfFileContent);
                }

                if (placeholdersFile != null) {
                    String placeholdersFileContent = readFile(placeholdersFile);
                    publishedWorkflowTemplatePlaceholders.put(publishId, placeholdersFileContent);
                    // store placeholder file if told to persist
                    if (persistent) {
                        FileUtils.writeStringToFile(placeholderStorageFile, placeholdersFileContent);
                    }
                } else {
                    // remove any pre-existing placeholder file's content
                    publishedWorkflowTemplatePlaceholders.put(publishId, null);
                }

                if (replaced == null) {
                    outputReceiver.addOutput(StringUtils.format("Successfully published workflow \"%s\"", publishId));
                } else {
                    outputReceiver.addOutput(StringUtils.format("Successfully updated the published workflow \"%s\"", publishId));
                }
            } catch (IOException e) {
                // avoid dangling, undefined workflow files on failure
                publishedWorkflowTemplates.remove(publishId);
                FileUtils.deleteQuietly(workflowStorageFile);
                throw new WorkflowExecutionException("Error publishing workflow file " + wfFile.getAbsolutePath());
            }
        }
    }

    @Override
    public void unpublishWorkflowForId(String publishId, TextOutputReceiver outputReceiver) throws WorkflowExecutionException {

        validateIdOrVersionString(publishId);

        String removed = publishedWorkflowTemplates.remove(publishId);
        publishedWorkflowTemplatePlaceholders.remove(publishId);

        // always try to delete the storage files; if publishing was temporary, they are simply not found
        File workflowStorageFile = getWorkflowStorageFile(publishId);
        if (workflowStorageFile.isFile()) {
            try {
                Files.delete(workflowStorageFile.toPath());
            } catch (IOException e) {
                throw new WorkflowExecutionException("Failed to unpublish the specified workflow; its storage file may be write-protected");
            }
        }
        File placeholderStorageFile = getPlaceholderStorageFile(publishId);
        if (placeholderStorageFile.isFile()) {
            try {
                Files.delete(placeholderStorageFile.toPath());
            } catch (IOException e) {
                throw new WorkflowExecutionException("Failed to unpublish the published placeholder file "
                    + "for the specified workflow; its storage file may be write-protected");
            }
        }

        if (removed != null) {
            outputReceiver.addOutput(StringUtils.format("Successfully unpublished workflow \"%s\"", publishId));
        } else {
            outputReceiver.addOutput(StringUtils.format("ERROR: There is no workflow with id \"%s\" to unpublish", publishId));
        }
    }

    /**
     * Prints human-readable information about all published workflows.
     * 
     * @param outputReceiver the receiver for the generated output
     */
    @Override
    public void printSummaryOfPublishedWorkflows(TextOutputReceiver outputReceiver) {
        if (publishedWorkflowTemplates.isEmpty()) {
            outputReceiver.addOutput("There are no workflows published for remote execution");
            return;
        }
        outputReceiver.addOutput("Workflows published for remote execution:");
        for (String publishId : publishedWorkflowTemplates.keySet()) {
            String placeholders = "no";
            if (publishedWorkflowTemplatePlaceholders.get(publishId) != null) {
                placeholders = "yes";
            }
            outputReceiver.addOutput(StringUtils.format("- %s (using placeholders: %s)", publishId, placeholders));
        }
    }

    @Override
    // TODO add unit test
    public String validateToolParametersAndGetFinalNodeId(String toolId, String toolVersion, String nodeId)
        throws WorkflowExecutionException {
        List<ComponentInstallation> availableTools = getMatchingPublishedTools();

        // note: not strictly necessary, but gives more consistent error messages instead of "tool not found"
        validateIdOrVersionString(toolId);
        validateIdOrVersionString(toolVersion);

        // only needed for nodeId == null to detect ambiguous matches
        ComponentInstallation nodeMatch = null;

        // TODO once components are cached, optimize with map lookup
        for (ComponentInstallation compInst : availableTools) {
            ComponentInterface compInterface = compInst.getComponentRevision().getComponentInterface();
            // TODO "display name" sounds odd here, but seems to be the public id; check
            if (toolId.equals(compInterface.getDisplayName())) {
                if (toolVersion.equals(compInterface.getVersion())) {
                    if (nodeId != null) {
                        // specific node id: exit on first match
                        if (nodeId.equals(compInst.getNodeId())) {
                            return compInst.getNodeId();
                        }
                    } else {
                        if (nodeMatch == null) {
                            nodeMatch = compInst;
                        } else {
                            throw new WorkflowExecutionException(StringUtils.format("Tool selection is ambiguous without a node id; "
                                + "tool '%s', version '%s' is provided by more than one node", toolId, toolVersion));
                        }
                    }
                }
            }
        }

        if (nodeId == null) {
            if (nodeMatch != null) {
                // success; single node match
                return nodeMatch.getNodeId();
            } else {
                throw new WorkflowExecutionException(StringUtils.format("No matching tool for tool '%s' in version '%s'", toolId,
                    toolVersion, nodeId));
            }
        } else {
            throw new WorkflowExecutionException(StringUtils.format("No matching tool for tool '%s' in version '%s', "
                + "running on a node with id '%s'", toolId, toolVersion, nodeId));
        }

    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindWorkflowExecutionService(HeadlessWorkflowExecutionService newInstance) {
        this.workflowExecutionService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        this.componentKnowledgeService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindEmbeddedSshServerControl(EmbeddedSshServerControl newInstance) {
        this.embeddedSshServerControl = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    public void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

    private void initAndRestoreFromPublishedWfStorage() {
        // initialize the storage location for published workflows and placeholder data
        publishedWfStorageDir =
            new File(configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA), "ra/published-wf");
        publishedWfStorageDir.mkdirs();
        if (!publishedWfStorageDir.isDirectory()) {
            log.error("Failed to create Remote Access workflow storage directory " + publishedWfStorageDir.getAbsolutePath());
            publishedWfStorageDir = null;
            return;
        }

        // restore persisted data
        for (File f : publishedWfStorageDir.listFiles()) {
            String filename = f.getName();
            if (filename.endsWith(PUBLISHED_WF_DATA_FILE_SUFFIX)) {
                String wfId = filename.substring(0, filename.length() - PUBLISHED_WF_DATA_FILE_SUFFIX.length());
                try {
                    publishedWorkflowTemplates.put(wfId, FileUtils.readFileToString(f));
                } catch (IOException e) {
                    log.error("Failed to restore data of published RemoteAccess workflow from storage file " + f.getAbsolutePath(), e);
                }
            } else if (filename.endsWith(PUBLISHED_WF_PLACEHOLDER_FILE_SUFFIX)) {
                String wfId = filename.substring(0, filename.length() - PUBLISHED_WF_PLACEHOLDER_FILE_SUFFIX.length());
                try {
                    publishedWorkflowTemplatePlaceholders.put(wfId, FileUtils.readFileToString(f));
                } catch (IOException e) {
                    log.error("Failed to restore placeholder data of published RemoteAccess workflow "
                        + "from storage file " + f.getAbsolutePath(), e);
                }
            } else {
                log.error("Unexpected file in RemoteAccess storage directory, ignoring: " + f.getAbsolutePath());
            }
        }

        // TODO check for placeholder data without a workflow file? only sanity check; no actual harm in them - misc_ro
    }

    private File getWorkflowStorageFile(String id) throws WorkflowExecutionException {
        if (publishedWfStorageDir == null) {
            throw new WorkflowExecutionException(
                "The workflow storage directory was not properly initialized; cannot execute this command");
        }
        File file = new File(publishedWfStorageDir, id + PUBLISHED_WF_DATA_FILE_SUFFIX);
        log.debug("Resolved workflow publish id to storage filename " + file.getAbsolutePath());
        return file;
    }

    private File getPlaceholderStorageFile(String id) throws WorkflowExecutionException {
        if (publishedWfStorageDir == null) {
            throw new WorkflowExecutionException(
                "The workflow storage directory was not properly initialized; cannot execute this command");
        }
        return new File(publishedWfStorageDir, id + PUBLISHED_WF_PLACEHOLDER_FILE_SUFFIX);
    }

    private boolean isComponentSuitableAsRemoteAccessTool(ComponentInstallation compInst) {
        ComponentInterface compInterf = compInst.getComponentRevision().getComponentInterface();
        EndpointDefinition endpoint;
        // validate id and version
        if (!checkIdOrVersionString(compInterf.getDisplayName()) || !checkIdOrVersionString(compInterf.getVersion())) {
            return false;
        }
        // do not allow more that two static inputs, as this may block execution
        if (compInterf.getInputDefinitionsProvider().getStaticEndpointDefinitions().size() != 2) {
            return false;
        }
        endpoint = compInterf.getInputDefinitionsProvider().getStaticEndpointDefinition(INTERFACE_ENDPOINT_NAME_INPUT);
        if (endpoint == null || !endpoint.getPossibleDataTypes().contains(DataType.DirectoryReference)) {
            return false;
        }
        endpoint = compInterf.getInputDefinitionsProvider().getStaticEndpointDefinition(INTERFACE_ENDPOINT_NAME_PARAMETERS);
        if (endpoint == null || !endpoint.getPossibleDataTypes().contains(DataType.ShortText)) {
            return false;
        }
        // additional outputs are allowed for now
        endpoint = compInterf.getOutputDefinitionsProvider().getStaticEndpointDefinition(INTERFACE_ENDPOINT_NAME_OUTPUT);
        if (endpoint == null || endpoint.getDefaultDataType() != DataType.DirectoryReference) {
            return false;
        }
        return true;
    }

    private List<ComponentInstallation> getMatchingPublishedTools() {
        List<ComponentInstallation> components = new ArrayList<>();
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
        for (ComponentInstallation ci : compKnowledge.getAllPublishedInstallations()) {
            if (isComponentSuitableAsRemoteAccessTool(ci)) {
                components.add(ci);
            }
        }
        // TODO sort?
        return components;
    }

    private void printComponentsListAsCsv(List<ComponentInstallation> components, TextOutputReceiver outputReceiver) {
        SortedSet<String> lines = new TreeSet<>();
        final CSVFormat csvFormat = CSVFormat.newFormat(' ').withQuote('"').withQuoteMode(QuoteMode.ALL);
        for (ComponentInstallation ci : components) {
            ComponentInterface compInterface = ci.getComponentRevision().getComponentInterface();
            String nodeId = ci.getNodeId();
            String nodeName = NodeIdentifierFactory.fromNodeId(nodeId).getAssociatedDisplayName();
            lines.add(csvFormat.format(compInterface.getDisplayName(), compInterface.getVersion(), nodeId, nodeName));
        }
        for (String line : lines) {
            outputReceiver.addOutput(line);
        }
    }

    private void printComponentsListAsTokens(List<ComponentInstallation> components, TextOutputReceiver outputReceiver) {
        outputReceiver.addOutput(Integer.toString(components.size())); // number of entries
        outputReceiver.addOutput("4"); // number of tokens per entry
        for (ComponentInstallation ci : components) {
            ComponentInterface compInterface = ci.getComponentRevision().getComponentInterface();
            String nodeId = ci.getNodeId();
            String nodeName = NodeIdentifierFactory.fromNodeId(nodeId).getAssociatedDisplayName();
            outputReceiver.addOutput(compInterface.getDisplayName());
            outputReceiver.addOutput(compInterface.getVersion());
            outputReceiver.addOutput(nodeId);
            outputReceiver.addOutput(nodeName);
        }
    }

    private boolean validateWorkflowFileAsTemplate(WorkflowDescription wd, TextOutputReceiver outputReceiver)
        throws WorkflowExecutionException {
        validateEquals(Integer.valueOf(4), wd.getWorkflowVersion(), "Invalid workflow file version");
        MutableYesNoFlag foundInputDirSource = new MutableYesNoFlag();
        MutableYesNoFlag foundParametersSource = new MutableYesNoFlag();
        MutableYesNoFlag foundOutputReceiver = new MutableYesNoFlag();
        for (WorkflowNode node : wd.getWorkflowNodes()) {
            outputReceiver.addOutput(OUTPUT_INDENT + "Checking component \"" + node.getName() + "\"  [" + node.getIdentifier() + "]");
            final ComponentDescription compDesc = node.getComponentDescription();
            final String compId = compDesc.getIdentifier();
            final String compVersion = compDesc.getVersion();
            if (compId.startsWith("de.rcenvironment.inputprovider/")) {
                validateEquals("3.2", compVersion, "Invalid component version");
                for (EndpointDescription outputEndpoint : node.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    checkEndpoint(outputEndpoint, "input directory source", INTERFACE_ENDPOINT_NAME_INPUT, DataType.DirectoryReference,
                        WF_PLACEHOLDER_INPUT_DIR,
                        foundInputDirSource, outputReceiver);
                    checkEndpoint(outputEndpoint, "input parameters source", INTERFACE_ENDPOINT_NAME_PARAMETERS, DataType.ShortText,
                        WF_PLACEHOLDER_PARAMETERS,
                        foundParametersSource, outputReceiver);
                }
            } else if (compId.startsWith("de.rcenvironment.outputwriter/")) {
                validateEquals("1.1", compVersion, "Invalid component version");
                Map<String, String> compConfig = compDesc.getConfigurationDescription().getConfiguration();
                String selectedRoot = compConfig.get("SelectedRoot");
                if (WF_PLACEHOLDER_OUTPUT_PARENT_DIR.equals(selectedRoot)) {
                    for (EndpointDescription inputEndpoint : node.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                        checkEndpoint(inputEndpoint, "output directory receiver", INTERFACE_ENDPOINT_NAME_OUTPUT,
                            DataType.DirectoryReference, null,
                            foundOutputReceiver, outputReceiver);
                    }
                    validateEquals("false", compConfig.get("SelectRootOnWorkflowStart"), "Invalid \"Select at workflow start\" setting");
                } else {
                    printEndpointValidationMessage(outputReceiver, StringUtils.format(
                        "Ignoring this Output Writer as its \"Root folder\" setting is not the \"%s\" marker",
                        WF_PLACEHOLDER_OUTPUT_PARENT_DIR));
                }
            }
        }
        // check for completeness
        if (foundInputDirSource.getValue() && foundParametersSource.getValue() && foundOutputReceiver.getValue()) {
            outputReceiver.addOutput("Validation successful");
            return true;
        } else {
            outputReceiver.addOutput("Validation failed:");
            outputReceiver.addOutput(OUTPUT_INDENT + "Found input directory source: " + foundInputDirSource);
            outputReceiver.addOutput(OUTPUT_INDENT + "Found input parameters source: " + foundParametersSource);
            outputReceiver.addOutput(OUTPUT_INDENT + "Found output receiver: " + foundOutputReceiver);
            return false;
        }
    }

    private void checkEndpoint(EndpointDescription endpoint, String description, String expectedName, DataType expectedDataType,
        String placeholderMarker, MutableYesNoFlag detectionFlag, TextOutputReceiver outputReceiver) throws WorkflowExecutionException {

        final String actualName = endpoint.getName();
        final DataType actualDataType = endpoint.getDataType();
        final boolean dataTypeMatches = expectedDataType == actualDataType;
        final boolean nameMatches = expectedName.equals(actualName);

        boolean allMatched = false;
        // minor hack to satisfy the CheckStyle "<= 6 parameters" rule: derive "isInputSide" value from the fact if a placeholderMarker is
        // set
        if (placeholderMarker != null) {
            final boolean hasMarkerValue = placeholderMarker.equals(endpoint.getMetaDataValue("startValue"));
            if (!nameMatches && !hasMarkerValue) {
                // neither name nor marker matches -> ignore silently
                return;
            }
            if (nameMatches && dataTypeMatches && hasMarkerValue) {
                allMatched = true;
            } else {
                printEndpointValidationMessage(outputReceiver, StringUtils.format(
                    "Output \"%s\" is a candidate for the %s, but it does not quite match: ", expectedName, description));
                if (!nameMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        StringUtils.format("  - Unexpected name \"%s\" instead of \"%s\"", actualName, expectedName));
                }
                if (!dataTypeMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        StringUtils.format("  - Unexpected data type \"%s\" instead of \"%s\"", actualDataType.getDisplayName(),
                            expectedDataType.getDisplayName()));
                }
                if (!hasMarkerValue) {
                    printEndpointValidationMessage(outputReceiver,
                        StringUtils.format("  - Marker value \"%s\" not found", placeholderMarker));
                }
                return;
            }
        } else {
            if (!nameMatches || !dataTypeMatches) {
                printEndpointValidationMessage(outputReceiver, StringUtils.format(
                    "Input \"%s\" is a candidate for the %s, but it does not quite match: ", actualName, description));
                if (!nameMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        StringUtils.format("  - Unexpected name \"%s\" instead of \"%s\"", actualName, expectedName));
                }
                if (!dataTypeMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        StringUtils.format("  - Unexpected data type \"%s\" instead of \"%s\"", actualDataType.getDisplayName(),
                            expectedDataType.getDisplayName()));
                }
                return;
            }
            // note: "placeholder" parameter is not used for output receiver
            validateEquals("output", endpoint.getMetaDataValue("filename"),
                "Invalid \"Target name\" setting in Output Writer; must be \"output\"");
            validateEquals("[root]", endpoint.getMetaDataValue("folderForSaving"),
                "Invalid \"Target folder\" setting in Output Writer; must be \"[root]\"");
            allMatched = true;
        }

        // check against accidental fall-through
        if (!allMatched) {
            throw new IllegalStateException("Internal error: Expected flag not set");
        }

        // check for duplicate and set flag if not
        if (detectionFlag.getValue()) {
            throw new WorkflowExecutionException("Found more than one " + description + " provider");
        } else {
            printEndpointValidationMessage(outputReceiver, StringUtils.format("Found %s \"%s\"", description, actualName));
            detectionFlag.setValue(true);
        }
    }

    private void printEndpointValidationMessage(TextOutputReceiver outputReceiver, String message) {
        outputReceiver.addOutput(OUTPUT_INDENT + OUTPUT_INDENT + message);
    }

    private void validateEquals(Object expected, Object actual, String message) throws WorkflowExecutionException {
        if (!expected.equals(actual)) {
            throw new WorkflowExecutionException(StringUtils.format("%s: Expected \"%s\", but found \"%s\"", message, expected, actual));
        }
    }

    // returns boolean result
    private boolean checkIdOrVersionString(String id) {
        return StringUtils.checkAgainstCommonInputRules(id) == null;
    }

    // throws exception on failure
    private void validateIdOrVersionString(String id) throws WorkflowExecutionException {
        // TODO add integration for high-level commands using this
        String valdationErrorMessage = StringUtils.checkAgainstCommonInputRules(id);
        if (valdationErrorMessage != null) {
            throw new WorkflowExecutionException("Invalid tool id, workflow id, or version \"" + id + "\": " + valdationErrorMessage);
        }
    }

    private String readFile(File placeholdersFile) throws IOException {
        return FileUtils.readFileToString(placeholdersFile, WORKFLOW_FILE_ENCODING);
    }

    private void renameAsOld(File outputFilesDir) {
        File tempDestination =
            new File(outputFilesDir.getParentFile(), outputFilesDir.getName() + ".old." + System.currentTimeMillis());
        outputFilesDir.renameTo(tempDestination);
        if (outputFilesDir.isDirectory()) {
            log.warn("Tried to move directory " + outputFilesDir.getAbsolutePath() + " to "
                + tempDestination.getAbsolutePath()
                + ", but it is still present");
        }
    }

    private ExecutionSetup generateSingleToolExecutionSetup(String toolId, String toolVersion, String toolNodeId, String parameterString,
        File inputFilesDir, File outputFilesDir) throws IOException {
        InputStream templateStream = getClass().getResourceAsStream(WORKFLOW_TEMPLATE_RESOURCE_PATH);
        if (templateStream == null) {
            throw new IOException("Failed to read remote tool access template");
        }
        String template = IOUtils.toString(templateStream, WORKFLOW_FILE_ENCODING);
        if (template == null || template.isEmpty()) {
            throw new IOException("Found remote tool access template, but had empty content after loading it");
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String timestampString = dateFormat.format(new Date());

        String workflowContent = template
            .replace(WF_PLACEHOLDER_TOOL_ID, toolId)
            .replace(WF_PLACEHOLDER_TOOL_VERSION, toolVersion) // guarded by validation; no escaping necessary
            .replace(WF_PLACEHOLDER_TOOL_NODE_ID, toolNodeId) // guarded by validation; no escaping necessary
            .replace(WF_PLACEHOLDER_PARAMETERS, StringUtils.escapeAsJsonStringContent(parameterString, false))
            .replace(WF_PLACEHOLDER_TIMESTAMP, timestampString)
            .replace(WF_PLACEHOLDER_INPUT_DIR, formatPathForWorkflowFile(inputFilesDir))
            // note: the name splitting is needed due to OutputWriter constraints - misc_ro
            // FIXME 5.1: recheck after placeholder change
            .replace(WF_PLACEHOLDER_OUTPUT_PARENT_DIR, formatPathForWorkflowFile(outputFilesDir.getParentFile()))
            .replace(WF_PLACEHOLDER_OUTPUT_FILES_FOLDER_NAME, outputFilesDir.getName());
        File wfFile = tempFileService.createTempFileFromPattern("rta-*.wf");
        FileUtils.write(wfFile, workflowContent, WORKFLOW_FILE_ENCODING);
        return new ExecutionSetup(wfFile, null, inputFilesDir, outputFilesDir);
    }

    private ExecutionSetup generateWorkflowExecutionSetup(String workflowId, String parameterString, File inputFilesDir,
        File outputFilesDir) throws IOException, WorkflowExecutionException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String timestampString = dateFormat.format(new Date());
        final String template = publishedWorkflowTemplates.get(workflowId);
        final String placeholdersFileContent = publishedWorkflowTemplatePlaceholders.get(workflowId);

        if (template == null) {
            throw new WorkflowExecutionException("There is no published workflow for id " + workflowId);
        }
        File placeholdersFile = null;
        if (placeholdersFileContent != null) {
            placeholdersFile = tempFileService.createTempFileFromPattern("rwa-properties-*.json");
            FileUtils.writeStringToFile(placeholdersFile, placeholdersFileContent, WORKFLOW_FILE_ENCODING);
        }

        String workflowContent = template
            .replace(WF_PLACEHOLDER_PARAMETERS, parameterString) // FIXME 5.1: escaping?!
            .replace(WF_PLACEHOLDER_TIMESTAMP, timestampString)
            .replace(WF_PLACEHOLDER_INPUT_DIR, formatPathForWorkflowFile(inputFilesDir))
            .replace(WF_PLACEHOLDER_OUTPUT_PARENT_DIR, formatPathForWorkflowFile(outputFilesDir.getParentFile()));
        File wfFile = tempFileService.createTempFileFromPattern("rwa-*.wf");
        FileUtils.write(wfFile, workflowContent, WORKFLOW_FILE_ENCODING);
        return new ExecutionSetup(wfFile, placeholdersFile, inputFilesDir, outputFilesDir);
    }

    private FinalWorkflowState executeConfiguredWorkflow(ExecutionSetup executionSetup,
        SingleConsoleRowsProcessor customConsoleRowReceiver) throws WorkflowExecutionException {
        log.debug("Executing remote access workflow " + executionSetup.getWorkflowFile().getAbsolutePath());

        File inputFilesDir = executionSetup.getInputFilesDir();
        File outputFilesDir = executionSetup.getOutputFilesDir();

        // move the output directory if it already exists to avoid collisions
        if (outputFilesDir.isDirectory()) {
            renameAsOld(outputFilesDir);
        }
        File logDir = new File(outputFilesDir.getParent(), "logs");
        if (logDir.isDirectory()) {
            renameAsOld(logDir);
        }
        logDir.mkdirs();

        // TODO review >5.0.0: remove this output capture, as it is only used for debug output? - misc_ro
        CapturingTextOutReceiver outputReceiver = new CapturingTextOutReceiver("");

        // TODO specify log directory?
        HeadlessWorkflowExecutionContextBuilder exeContextBuilder =
            new HeadlessWorkflowExecutionContextBuilder(executionSetup.getWorkflowFile(), logDir);
        exeContextBuilder.setPlaceholdersFile(executionSetup.getPlaceholderFile());
        exeContextBuilder.setTextOutputReceiver(outputReceiver);
        exeContextBuilder.setSingleConsoleRowsProcessor(customConsoleRowReceiver);

        WorkflowExecutionException executionException = null;
        FinalWorkflowState finalState = FinalWorkflowState.FAILED;
        try {
            finalState = workflowExecutionService.executeWorkflowSync(exeContextBuilder.build());
        } catch (WorkflowExecutionException e) {
            executionException = e;
            File exceptionLogFile = new File(logDir, "error.log");
            // create a log file so the error cause is accessible via the log directory
            try {
                FileUtils.writeStringToFile(exceptionLogFile, "Workflow execution failed with an error: " + e.toString());
            } catch (IOException e1) {
                log.error("Failed to write exception log file " + exceptionLogFile.getAbsolutePath());
            }
        }
        log.debug("Finished remote access workflow; captured output:\n" + outputReceiver.getBufferedOutput());

        // move the input directory to avoid future collisions
        if (inputFilesDir.isDirectory()) {
            File tempDestination = new File(inputFilesDir.getParentFile(), "input.old." + System.currentTimeMillis());
            inputFilesDir.renameTo(tempDestination);
            if (inputFilesDir.isDirectory()) {
                log.warn("Tried to rename input directory " + inputFilesDir.getAbsolutePath() + " to " + tempDestination.getAbsolutePath()
                    + ", but it is still present");
            }
        }

        if (executionException != null) {
            throw executionException;
        }

        return finalState;
    }

    private CharSequence formatPathForWorkflowFile(File directory) {
        return directory.getAbsolutePath().replaceAll("\\\\", "/"); // double escaping for java+regexp; replaces "\"->"/"
    }

}
