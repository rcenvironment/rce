/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.toolaccess.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import de.rcenvironment.core.component.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;

/**
 * Provides "tool access" operations. TODO outline TA concept
 * 
 * @author Robert Mischke
 */
public class RemoteAccessServiceImpl implements RemoteAccessService {

    /** Placeholder for an input directory. */
    public static final String WF_PLACEHOLDER_INPUT_DIR = "##RUNTIME_INPUT_DIRECTORY##";

    private static final String INTERFACE_ENDPOINT_NAME_INPUT = "input";

    private static final String INTERFACE_ENDPOINT_NAME_PARAMETERS = "parameters";

    private static final String INTERFACE_ENDPOINT_NAME_OUTPUT = "output";

    private static final String WF_PLACEHOLDER_PARAMETERS = "##RUNTIME_PARAMETERS##";

    private static final String WF_PLACEHOLDER_OUTPUT_PARENT_DIR = "##RUNTIME_OUTPUT_DIRECTORY##";

    private static final String WF_PLACEHOLDER_OUTPUT_FILES_FOLDER_NAME = "##OUTPUT_FILES_FOLDER_NAME##";

    private static final String WORKFLOW_TEMPLATE_RESOURCE_PATH = "/resources/template.wf";

    private static final String WF_PLACEHOLDER_TOOL_ID = "##TOOL_ID##";

    private static final String WF_PLACEHOLDER_TOOL_VERSION = "##TOOL_VERSION##";

    private static final String WF_PLACEHOLDER_TIMESTAMP = "##TIMESTAMP##";

    private static final String WORKFLOW_FILE_ENCODING = "UTF-8";

    private static final String OUTPUT_INDENT = "    ";

    private final Log log = LogFactory.getLog(getClass());

    private final Map<String, String> publishedWorkflowTemplates = new HashMap<>();

    private final Map<String, String> publishedWorkflowTemplatePlaceholders = new HashMap<>();

    private final TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private HeadlessWorkflowExecutionService workflowExecutionService;

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

    @Override
    public void printListOfAvailableTools(TextOutputReceiver outputReceiver, String format) {
        List<ComponentInstallation> components = new ArrayList<>();

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentComponentKnowledge();
        for (ComponentInstallation ci : compKnowledge.getAllInstallations()) {
            if (isComponentSuitableAsRemoteAccessTool(ci)) {
                components.add(ci);
            }
        }

        if ("csv".equals(format)) {
            printComponentsListAsCsv(components, outputReceiver);
        } else if ("token-stream".equals(format)) {
            printComponentsListAsTokens(components, outputReceiver);
        } else {
            throw new IllegalArgumentException("Unrecognized output format: " + format);
        }
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
            outputReceiver.addOutput(publishId);
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
     * @param parameterString an optional string containing tool-specific parameters
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    @Override
    public FinalWorkflowState runSingleToolWorkflow(String toolId, String toolVersion, String parameterString, File inputFilesDir,
        File outputFilesDir, SingleConsoleRowsProcessor consoleRowReceiver) throws IOException, WorkflowExecutionException {
        ExecutionSetup executionSetup =
            generateSingleToolExecutionSetup(toolId, toolVersion, parameterString, inputFilesDir, outputFilesDir);
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
     * @throws WorkflowExecutionException on failure to load/parse the workflow file
     */
    @Override
    public void checkAndPublishWorkflowFile(File wfFile, File placeholdersFile, String publishId, TextOutputReceiver outputReceiver)
        throws WorkflowExecutionException {

        WorkflowDescription wd = workflowExecutionService.parseWorkflowFile(wfFile, outputReceiver);

        if (placeholdersFile != null) {
            workflowExecutionService.validatePlaceholdersFile(placeholdersFile);
        }

        outputReceiver.addOutput(String.format("Checking workflow file \"%s\"", wfFile.getAbsolutePath()));
        if (validateWorkflowFileAsTemplate(wd, outputReceiver)) {
            try {
                // TODO make persistent
                String replaced = publishedWorkflowTemplates.put(publishId, readFile(wfFile));
                if (placeholdersFile != null) {
                    publishedWorkflowTemplatePlaceholders.put(publishId, readFile(placeholdersFile));
                } else {
                    // remove any pre-existing placeholder file's content
                    publishedWorkflowTemplatePlaceholders.put(publishId, null);
                }
                if (replaced == null) {
                    outputReceiver.addOutput(String.format("Successfully published workflow \"%s\"", publishId));
                } else {
                    outputReceiver.addOutput(String.format("Successfully updated the published workflow \"%s\"", publishId));
                }
            } catch (IOException e) {
                throw new WorkflowExecutionException("Error publishing workflow file " + wfFile.getAbsolutePath());
            }
        }
    }

    /**
     * Makes the published workflow with the given id unavailable for remote invocation. If no such workflow exists, a text warning is
     * written to the output receiver.
     * 
     * @param publishId the id of the workflow to unpublish
     * @param outputReceiver the receiver for user feedback
     */
    @Override
    public void unpublishWorkflowForId(String publishId, TextOutputReceiver outputReceiver) {
        String replaced = publishedWorkflowTemplates.put(publishId, null);
        publishedWorkflowTemplatePlaceholders.put(publishId, null);

        if (replaced != null) {
            outputReceiver.addOutput(String.format("Successfully unpublished workflow \"%s\"", publishId));
        } else {
            outputReceiver.addOutput(String.format("ERROR: There is no workflow with id \"%s\" to unpublish", publishId));
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
            outputReceiver.addOutput(String.format("- %s (using placeholders: %s)", publishId, placeholders));
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

    private boolean isComponentSuitableAsRemoteAccessTool(ComponentInstallation compInst) {
        ComponentInterface compInterf = compInst.getComponentRevision().getComponentInterface();
        EndpointDefinition endpoint;
        // do not allow more that two static inputs, as this may block execution
        if (compInterf.getInputDefinitionsProvider().getStaticEndpointDefinitions().size() != 2)
        {
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
                    printEndpointValidationMessage(outputReceiver, String.format(
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
                printEndpointValidationMessage(outputReceiver, String.format(
                    "Output \"%s\" is a candidate for the %s, but it does not quite match: ", expectedName, description));
                if (!nameMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        String.format("  - Unexpected name \"%s\" instead of \"%s\"", actualName, expectedName));
                }
                if (!dataTypeMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        String.format("  - Unexpected data type \"%s\" instead of \"%s\"", actualDataType.getDisplayName(),
                            expectedDataType.getDisplayName()));
                }
                if (!hasMarkerValue) {
                    printEndpointValidationMessage(outputReceiver,
                        String.format("  - Marker value \"%s\" not found", placeholderMarker));
                }
                return;
            }
        } else {
            if (!nameMatches || !dataTypeMatches) {
                printEndpointValidationMessage(outputReceiver, String.format(
                    "Input \"%s\" is a candidate for the %s, but it does not quite match: ", actualName, description));
                if (!nameMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        String.format("  - Unexpected name \"%s\" instead of \"%s\"", actualName, expectedName));
                }
                if (!dataTypeMatches) {
                    printEndpointValidationMessage(outputReceiver,
                        String.format("  - Unexpected data type \"%s\" instead of \"%s\"", actualDataType.getDisplayName(),
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
            printEndpointValidationMessage(outputReceiver, String.format("Found %s \"%s\"", description, actualName));
            detectionFlag.setValue(true);
        }
    }

    private void printEndpointValidationMessage(TextOutputReceiver outputReceiver, String message) {
        outputReceiver.addOutput(OUTPUT_INDENT + OUTPUT_INDENT + message);
    }

    private void validateEquals(Object expected, Object actual, String message) throws WorkflowExecutionException {
        if (!expected.equals(actual)) {
            throw new WorkflowExecutionException(String.format("%s: Expected \"%s\", but found \"%s\"", message, expected, actual));
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

    private ExecutionSetup generateSingleToolExecutionSetup(String toolId, String toolVersion, String parameterString, File inputFilesDir,
        File outputFilesDir) throws IOException {
        InputStream templateStream = getClass().getResourceAsStream(WORKFLOW_TEMPLATE_RESOURCE_PATH);
        if (templateStream == null) {
            throw new IOException("Failed to read tool access template");
        }
        String template = IOUtils.toString(templateStream, WORKFLOW_FILE_ENCODING);
        if (template == null || template.isEmpty()) {
            throw new IOException("Found tool access template, but had empty content after loading it");
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String timestampString = dateFormat.format(new Date());

        String workflowContent = template
            .replace(WF_PLACEHOLDER_TOOL_ID, toolId)
            .replace(WF_PLACEHOLDER_TOOL_VERSION, toolVersion)
            .replace(WF_PLACEHOLDER_PARAMETERS, parameterString) // FIXME 5.1: escaping?!
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
        log.debug("Executing tool access workflow " + executionSetup.getWorkflowFile().getAbsolutePath());

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
        WorkflowExecutionException executionException = null;
        FinalWorkflowState finalState = FinalWorkflowState.FAILED;
        try {
            finalState =
                workflowExecutionService
                    .executeWorkflow(executionSetup.getWorkflowFile(), executionSetup.getPlaceholderFile(), logDir, outputReceiver,
                        customConsoleRowReceiver);
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
        log.debug("Finished tool access workflow; captured output:\n" + outputReceiver.getBufferedOutput());

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
