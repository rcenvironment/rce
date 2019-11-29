/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.server.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.integration.documentation.ToolIntegrationDocumentationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.spi.DistributedComponentKnowledgeListener;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.configuration.PersistentSettingsService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.embedded.ssh.api.EmbeddedSshServerControl;
import de.rcenvironment.core.monitoring.system.api.LocalSystemMonitoringAggregationService;
import de.rcenvironment.core.monitoring.system.api.model.AverageOfDoubles;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.remoteaccess.common.RemoteAccessConstants;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.CommonIdRules;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * Provides "remote access" operations. TODO outline RA concept
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
// TODO @7.0.0: remove duplicate javadoc
// TODO @7.0.0: use better exception class than WorkflowExecutionException
@Component
public class RemoteAccessServiceImpl implements RemoteAccessService {

    private static final String REQUIRED_INPUTLOADER_VERSION = "1.1";
    
    private static final String REQUIRED_OUTPUTCOLLECTOR_VERSION = "1.1";

    private static final String TAB = "\t";

    private static final String NAME_DATA_TYPE = "[name]\t[data type]";

    private static final String FORMAT_2F = "%.2f";

    private static final int PERCENT_MULTIPLIER = 100;

    private static final int INT_NO_DATA_PLACEHOLDER = -1;

    private static final double DOUBLE_NO_DATA_PLACEHOLDER = -1.0;

    private static final String DE_RCENVIRONMENT_SCPOUTPUTCOLLECTOR = "de.rcenvironment.scpoutputcollector/";

    private static final String DE_RCENVIRONMENT_SCPINPUTLOADER = "de.rcenvironment.scpinputloader/";

    private static final String KEY_META_DATA = "metaData";

    private static final String KEY_DEFAULT_DATA_TYPE = "defaultDataType";

    private static final String KEY_DATA_TYPE = "dataType";

    private static final String KEY_DATA_TYPES = "dataTypes";

    private static final String KEY_INPUT_HANDLING_OPTIONS = "inputHandlingOptions";

    private static final String KEY_DEFAULT_INPUT_HANDLING = "defaultInputHandling";

    private static final String KEY_EXECUTION_CONSTRAINT_OPTIONS = "inputExecutionConstraintOptions";

    private static final String KEY_DEFAULT_EXECUTION_CONSTRAINT = "defaultInputExecutionConstraint";

    private static final String KEY_DEFAULT_VALUE = "defaultValue";

    private static final String KEY_POSSIBLE_VALUES = "possibleValues";

    private static final int NUMBER_600 = 600;

    private static final int NUMBER_200 = 200;

    private static final int NUMBER_400 = 400;

    // private static final String WF_PLACEHOLDER_PARAMETERS = "##RUNTIME_PARAMETERS##";

    private static final String WF_PLACEHOLDER_INPUT_DIR = "##SCP_UPLOAD_DIRECTORY##";

    private static final String WF_PLACEHOLDER_OUTPUT_PARENT_DIR = "##SCP_DOWNLOAD_DIRECTORY##";

    private static final String WF_PLACEHOLDER_UNCOMPRESSED_UPLOAD = "##UNCOMPRESSED_UPLOAD_FLAG##";

    private static final String WF_PLACEHOLDER_UNCOMPRESSED_DOWNLOAD = "##UNCOMPRESSED_DOWNLOAD_FLAG##";

    private static final String WF_PLACEHOLDER_SIMPLE_DESCRIPTION_FORMAT = "##SIMPLE_FORMAT_FLAG##";

    private static final String WF_PLACEHOLDER_TIMESTAMP = "##TIMESTAMP##";

    private static final String WORKFLOW_FILE_ENCODING = "UTF-8";

    private static final String PUBLISHED_WF_DATA_FILE_SUFFIX = ".wf.dat";

    private static final String PUBLISHED_WF_PLACEHOLDER_FILE_SUFFIX = ".ph.dat";

    private static final String PUBLISHED_WF_GROUP_KEY_PREFIX = "WF_GROUP_";

    private static final String PUBLISHED_WF_KEEP_DATA_KEY_PREFIX = "WF_KEEP_DATA";

    private static final String OUTPUT_INDENT = "    ";

    private final Log log = LogFactory.getLog(getClass());

    private final Map<String, String> publishedWorkflowTemplates = new HashMap<>();

    private final Map<String, String> publishedWorkflowTemplatePlaceholders = new HashMap<>();

    private final TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private PlatformService platformService;

    private ConfigurationService configurationService;

    private LocalSystemMonitoringAggregationService localSystemMonitoringAggregationService;

    private PersistentSettingsService persistentSettingsService;

    private File publishedWfStorageDir;

    private EmbeddedSshServerControl embeddedSshServerControl;

    private ObjectMapper mapper = new ObjectMapper();

    private ServiceRegistryPublisherAccess serviceRegistryAccess;

    private List<String[]> toolTokens;

    private List<String[]> simpleToolTokens;

    private Map<String, String[]> wfTokens;

    private Map<String, String[]> simpleWfTokens;

    private Map<String, Map<String, String>> wfInputs;

    private Map<String, Map<String, String>> wfOutputs;

    // Maps the session tokens of running tools/workflows to their workflow execution id to enable cancelling
    private volatile Map<String, WorkflowExecutionInformation> sessionTokenToWfExecInf;

    private ToolIntegrationDocumentationService toolDocService;

    /**
     * Simple holder for execution parameters, including the workflow template file.
     * 
     * @author Robert Mischke
     */
    private static final class ExecutionSetup {

        private File workflowFile;

        private File placeholdersFile;

        private String sessionToken;

        private File inputFilesDir;

        private File outputFilesDir;

        ExecutionSetup(File wfFile, File placeholdersFile, String sessionToken, File inputFilesDir,
            File outputFilesDir) {
            this.workflowFile = wfFile;
            this.placeholdersFile = placeholdersFile;
            this.sessionToken = sessionToken;
            this.inputFilesDir = inputFilesDir;
            this.outputFilesDir = outputFilesDir;
        }

        public File getWorkflowFile() {
            return workflowFile;
        }

        public File getPlaceholderFile() {
            return placeholdersFile;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public File getInputFilesDir() {
            return inputFilesDir;
        }

        public File getOutputFilesDir() {
            return outputFilesDir;
        }

    }

    /**
     * OSGi life-cycle method.
     */
    public void activate() {
        serviceRegistryAccess = ServiceRegistry.createPublisherAccessFor(this);
        registerChangeListener();
        sessionTokenToWfExecInf = new HashMap<String, WorkflowExecutionInformation>();

        ConcurrencyUtils.getAsyncTaskService().execute("Server-Side Remote Access: Restore persistently published workflows",
            this::restoreWorkflowTemplatesFromPublishedWfStorage);

        embeddedSshServerControl.setAnnouncedVersionOrProperty("RemoteAccess",
            RemoteAccessConstants.PROTOCOL_VERSION_STRING);
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance to bind
     */
    @Reference
    public void bindToolIntegrationDocumentationService(ToolIntegrationDocumentationService newInstance) {
        this.toolDocService = newInstance;
    }

    private void registerChangeListener() {
        serviceRegistryAccess.registerService(DistributedComponentKnowledgeListener.class,
            new DistributedComponentKnowledgeListener() {

                @Override
                public void onDistributedComponentKnowledgeChanged(
                    final DistributedComponentKnowledge newKnowledge) {
                    updateToolTokens();
                }
            });
    }

    private void updateToolTokens() {
        toolTokens = new ArrayList<String[]>();
        simpleToolTokens = new ArrayList<String[]>();

        for (DistributedComponentEntry entry : getMatchingPublishedTools()) {
            ComponentInterface compInterface = entry.getComponentInterface();
            String nodeId = entry.getNodeId();
            String nodeName = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(nodeId)
                .getAssociatedDisplayName();
            try {
                Set<Map<String, Object>> nodeInputSet = new HashSet<>();
                for (EndpointDefinition ed : entry.getComponentInterface().getInputDefinitionsProvider()
                    .getStaticEndpointDefinitions()) {
                    Map<String, Object> rawEndpointData = new HashMap<String, Object>();

                    rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
                    rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
                    rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
                    rawEndpointData.put(KEY_INPUT_HANDLING_OPTIONS, ed.getInputDatumOptions());
                    rawEndpointData.put(KEY_DEFAULT_INPUT_HANDLING, ed.getDefaultInputDatumHandling());
                    rawEndpointData.put(KEY_EXECUTION_CONSTRAINT_OPTIONS, ed.getInputExecutionConstraintOptions());
                    rawEndpointData.put(KEY_DEFAULT_EXECUTION_CONSTRAINT, ed.getDefaultInputExecutionConstraint());
                    nodeInputSet.add(rawEndpointData);
                }
                for (EndpointDefinition ed : entry.getComponentInterface().getInputDefinitionsProvider()
                    .getDynamicEndpointDefinitions()) {
                    Map<String, Object> rawEndpointData = new HashMap<String, Object>();

                    rawEndpointData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, ed.getIdentifier());
                    rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
                    rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
                    rawEndpointData.put(KEY_INPUT_HANDLING_OPTIONS, ed.getInputDatumOptions());
                    rawEndpointData.put(KEY_DEFAULT_INPUT_HANDLING, ed.getDefaultInputDatumHandling());
                    rawEndpointData.put(KEY_EXECUTION_CONSTRAINT_OPTIONS, ed.getInputExecutionConstraintOptions());
                    rawEndpointData.put(KEY_DEFAULT_EXECUTION_CONSTRAINT, ed.getDefaultInputExecutionConstraint());
                    Map<String, Map<String, Object>> rawMetadata = extractRawMetadata(ed);
                    rawEndpointData.put(KEY_META_DATA, rawMetadata);
                    nodeInputSet.add(rawEndpointData);
                }
                String nodeInputs = mapper.writeValueAsString(nodeInputSet);

                Set<Map<String, Object>> nodeOutputSet = new HashSet<>();
                for (EndpointDefinition ed : entry.getComponentInterface().getOutputDefinitionsProvider()
                    .getStaticEndpointDefinitions()) {
                    Map<String, Object> rawEndpointData = new HashMap<String, Object>();

                    rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
                    rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
                    rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
                    nodeOutputSet.add(rawEndpointData);
                }
                for (EndpointDefinition ed : entry.getComponentInterface().getOutputDefinitionsProvider()
                    .getDynamicEndpointDefinitions()) {
                    Map<String, Object> rawEndpointData = new HashMap<String, Object>();

                    rawEndpointData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, ed.getIdentifier());
                    rawEndpointData.put(KEY_DATA_TYPES, ed.getPossibleDataTypes());
                    rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDefaultDataType());
                    Map<String, Map<String, Object>> rawMetadata = extractRawMetadata(ed);
                    rawEndpointData.put(KEY_META_DATA, rawMetadata);
                    nodeOutputSet.add(rawEndpointData);
                }
                String nodeOutputs = mapper.writeValueAsString(nodeOutputSet);

                // Compute hash and add to output line
                String toHash = compInterface.getDisplayName() + compInterface.getVersion() + nodeId + nodeName
                    + nodeInputs + nodeOutputs + compInterface.getGroupName();
                toolTokens.add(new String[] { compInterface.getDisplayName(), compInterface.getVersion(), nodeId,
                    nodeName, nodeInputs, nodeOutputs, compInterface.getGroupName(),
                    Integer.toString(toHash.hashCode()) });
                simpleToolTokens.add(new String[] { compInterface.getDisplayName(), compInterface.getVersion(), nodeId, nodeName });
            } catch (IOException e) {
                log.error("An error occured while creating descriptions of the available tools.");
            }
        }
    }

    private void addOrReplaceWorkflowInWfTokens(String publishId, String groupName, WorkflowDescription wd) {

        if (!checkIdString(publishId)) {
            // for backwards compatility
            log.error("Not listing the previously published remote access workflow " + publishId
                + "; the name contains characters that are not allowed anymore");
            return;

        }

        String nodeId = platformService.getLocalDefaultLogicalNodeId().getLogicalNodeIdString();
        String nodeName = platformService.getLocalDefaultLogicalNodeId().getAssociatedDisplayName();
        String version = "1"; // version; hardcoded for now

        if (groupName == null) {
            groupName = RemoteAccessConstants.DEFAULT_GROUP_NAME_WFS;
        }

        ComponentDescription inputLoaderDesc = null;
        ComponentDescription outputCollectorDesc = null;

        for (WorkflowNode node : wd.getWorkflowNodes()) {
            final ComponentDescription compDesc = node.getComponentDescription();
            String compId = compDesc.getIdentifier();
            // Component may not yet be available on RCE start (thus compId might contain
            // "missing_[compId]"), but this does not matter for
            // token generation
            if (compId.contains(DE_RCENVIRONMENT_SCPINPUTLOADER)) {
                inputLoaderDesc = compDesc;
            } else if (compId.contains(DE_RCENVIRONMENT_SCPOUTPUTCOLLECTOR)) {
                outputCollectorDesc = compDesc;
            }
        }

        if (inputLoaderDesc == null || outputCollectorDesc == null) {
            log.error("Error while parsing published workflow " + publishId);
            return;
        }

        try {
            // Get outputs of SCPInputLoader, these are the "inputs" of the published
            // workflow
            Set<Map<String, Object>> nodeInputSet = new HashSet<>();
            Map<String, String> simpleInputsMap = new HashMap<String, String>();
            for (EndpointDescription ed : inputLoaderDesc.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                Map<String, Object> rawEndpointData = new HashMap<String, Object>();
                ArrayList<DataType> dataTypes = new ArrayList<DataType>();
                dataTypes.add(ed.getDataType());
                rawEndpointData.put(KEY_DATA_TYPES, dataTypes);
                rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
                rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDataType());
                nodeInputSet.add(rawEndpointData);

                simpleInputsMap.put(ed.getName(), ed.getDataType().getDisplayName());
            }
            String nodeInputs = mapper.writeValueAsString(nodeInputSet);

            // Get inputs of SCPOutputCollector, these are the "outputs" of the published
            // workflow
            Set<Map<String, Object>> nodeOutputSet = new HashSet<>();
            Map<String, String> simpleOutputsMap = new HashMap<String, String>();
            for (EndpointDescription ed : outputCollectorDesc.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                Map<String, Object> rawEndpointData = new HashMap<String, Object>();

                rawEndpointData.put(EndpointDefinitionConstants.KEY_NAME, ed.getName());
                ArrayList<DataType> dataTypes = new ArrayList<DataType>();
                dataTypes.add(ed.getDataType());
                rawEndpointData.put(KEY_DATA_TYPES, dataTypes);
                rawEndpointData.put(KEY_DEFAULT_DATA_TYPE, ed.getDataType());
                nodeOutputSet.add(rawEndpointData);
            }
            String nodeOutputs = mapper.writeValueAsString(nodeOutputSet);

            // Compute hash and add to output tokens
            String toHash = publishId + version + nodeId + nodeName + nodeInputs + nodeOutputs;
            wfTokens.put(publishId, new String[] { publishId, version, groupName, nodeId, nodeName, nodeInputs,
                nodeOutputs, Integer.toString(toHash.hashCode()) });
            simpleWfTokens.put(publishId, new String[] { publishId, version, nodeId, nodeName });
            wfInputs.put(publishId, simpleInputsMap);
            wfOutputs.put(publishId, simpleOutputsMap);
        } catch (IOException e) {
            log.error("An error occured while updating descriptions of the available workflows.");
        }
    }

    @Override
    public void printListOfAvailableTools(TextOutputReceiver outputReceiver, String format, boolean includeLoadData,
        int timeSpanMsec, int timeLimitMsec) throws InterruptedException, ExecutionException, TimeoutException {

        final Map<LogicalNodeId, SystemLoadInformation> systemLoadData;
        if (includeLoadData) {
            final Set<LogicalNodeId> reachableInstanceNodes = new HashSet<>();
            for (Object[] tokens : toolTokens) {
                LogicalNodeId nodeIdObj = NodeIdentifierUtils
                    .parseLogicalNodeIdStringWithExceptionWrapping((String) tokens[2]);

                reachableInstanceNodes.add(nodeIdObj);
            }
            systemLoadData = localSystemMonitoringAggregationService
                .collectSystemMonitoringDataWithTimeLimit(reachableInstanceNodes, timeSpanMsec, timeLimitMsec);
        } else {
            systemLoadData = null;
        }

        if ("csv".equals(format)) {
            printComponentsListAsCsv(outputReceiver, systemLoadData);
        } else if ("token-stream".equals(format)) {
            printComponentsListAsTokens(outputReceiver, systemLoadData);
        } else if ("simple".equals(format)) {
            printSimpleComponentsListAsTokens(outputReceiver, systemLoadData);
        } else {
            throw new IllegalArgumentException("Unrecognized output format: " + format);
        }
    }

    private void printSimpleComponentsListAsTokens(TextOutputReceiver outputReceiver,
        Map<LogicalNodeId, SystemLoadInformation> systemLoadData) {
        outputReceiver.addOutput(Integer.toString(simpleToolTokens.size())); // number of entries
        // print number of tokens per entry
        if (systemLoadData != null) {
            outputReceiver.addOutput("9");
        } else {
            outputReceiver.addOutput("5");
        }

        for (String[] tokens : simpleToolTokens) {
            for (int i = 0; i < tokens.length; i++) {
                outputReceiver.addOutput(tokens[i]);
            }

            if (systemLoadData != null) {
                String nodeId = (String) tokens[2];
                LogicalNodeId nodeIdObj = NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(nodeId);
                // TODO (p3) extract common code with above method?
                final SystemLoadInformation loadDataEntry = systemLoadData.get(nodeIdObj);
                // two-step checking as there may be no load data available for that node
                final double cpuAvg;
                final int numSamples;
                final int timeSpan;
                final long availableRam;
                if (loadDataEntry != null) {
                    final AverageOfDoubles cpuLoadAvg = loadDataEntry.getCpuLoadAvg();
                    cpuAvg = cpuLoadAvg.getAverage() * PERCENT_MULTIPLIER;
                    numSamples = cpuLoadAvg.getNumSamples();
                    timeSpan = cpuLoadAvg.getNumSamples()
                        * LocalSystemMonitoringAggregationService.SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC;
                    availableRam = loadDataEntry.getAvailableRam();
                } else {
                    cpuAvg = DOUBLE_NO_DATA_PLACEHOLDER;
                    numSamples = INT_NO_DATA_PLACEHOLDER;
                    timeSpan = INT_NO_DATA_PLACEHOLDER;
                    availableRam = INT_NO_DATA_PLACEHOLDER;
                }

                outputReceiver.addOutput(StringUtils.format(FORMAT_2F, cpuAvg));
                outputReceiver.addOutput(Integer.toString(numSamples));
                outputReceiver.addOutput(Integer.toString(timeSpan));
                outputReceiver.addOutput(Long.toString(availableRam));
            }
        }

    }

    @Override
    public void printToolDetails(TextOutputReceiver outputReceiver, String toolId, String toolVersion, String nodeId, boolean template) {
        DistributedComponentEntry entry = getMatchingComponentInstallationForTool(toolId, toolVersion, nodeId);

        if (template) {
            Map<String, String> inputsMap = new HashMap<String, String>();
            for (EndpointDefinition ed : entry.getComponentInterface().getInputDefinitionsProvider()
                .getStaticEndpointDefinitions()) {
                inputsMap.put(ed.getName(), "<" + ed.getDefaultDataType().getDisplayName() + ">");
            }
            try {
                outputReceiver.addOutput(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputsMap));
            } catch (IOException e) {
                log.error("Writing template failed: " + e.getMessage());
            }
        } else {
            outputReceiver.addOutput("Inputs:");
            outputReceiver.addOutput(NAME_DATA_TYPE);
            for (EndpointDefinition ed : entry.getComponentInterface().getInputDefinitionsProvider()
                .getStaticEndpointDefinitions()) {

                outputReceiver.addOutput(ed.getName() + TAB + ed.getDefaultDataType());
            }

            outputReceiver.addOutput("\nOutputs:");
            outputReceiver.addOutput(NAME_DATA_TYPE);

            for (EndpointDefinition ed : entry.getComponentInterface().getOutputDefinitionsProvider()
                .getStaticEndpointDefinitions()) {
                outputReceiver.addOutput(ed.getName() + TAB + ed.getDefaultDataType());
            }
        }
    }

    @Override
    public void printWfDetails(TextOutputReceiver outputReceiver, String wfId, boolean template) {

        Map<String, String> simpleInputsMap = wfInputs.get(wfId);
        Map<String, String> simpleOutputsMap = wfInputs.get(wfId);
        if (template) {
            Map<String, String> inputsMap = new HashMap<String, String>();
            for (String inputName : simpleInputsMap.keySet()) {
                inputsMap.put(inputName, "<" + simpleInputsMap.get(inputName) + ">");
            }
            try {
                outputReceiver.addOutput(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputsMap));
            } catch (IOException e) {
                log.error("Writing template failed: " + e.getMessage());
            }
        } else {
            outputReceiver.addOutput("Inputs:");
            outputReceiver.addOutput(NAME_DATA_TYPE);
            for (String inputName : simpleInputsMap.keySet()) {
                outputReceiver.addOutput(inputName + TAB + simpleInputsMap.get(inputName));
            }

            outputReceiver.addOutput("\nOutputs:");
            outputReceiver.addOutput(NAME_DATA_TYPE);

            for (String outputName : simpleOutputsMap.keySet()) {
                outputReceiver.addOutput(outputName + TAB + simpleOutputsMap.get(outputName));
            }
        }
    }

    @Override
    public void printListOfAvailableWorkflows(TextOutputReceiver outputReceiver, String format) {
        if ("token-stream".equals(format)) {
            // NOTE: the output format is made to match printListOfAvailableTools(); most fields are not used yet
            outputReceiver.addOutput(Integer.toString(wfTokens.size())); // number of entries
            outputReceiver.addOutput("8"); // number of tokens per entry
            for (String[] tokens : wfTokens.values()) {
                for (int i = 0; i < tokens.length; i++) {
                    outputReceiver.addOutput(tokens[i]);
                }
            }
        } else if ("simple".equals(format)) {
            outputReceiver.addOutput(Integer.toString(simpleWfTokens.size())); // number of entries
            outputReceiver.addOutput("4"); // number of tokens per entry
            for (String[] tokens : simpleWfTokens.values()) {
                for (int i = 0; i < tokens.length; i++) {
                    outputReceiver.addOutput(tokens[i]);
                }
            }
        } else {
            throw new IllegalArgumentException("Unrecognized output format: " + format);
        }
    }

    /**
     * Creates a workflow file from an internal template and the given parameters, and executes it.
     * 
     * @param parameters the remote execution parameter object
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * 
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    @Override
    public FinalWorkflowState runSingleToolWorkflow(RemoteComponentExecutionParameter parameters,
        SingleConsoleRowsProcessor consoleRowReceiver) throws IOException, WorkflowExecutionException {
        validateIdString(parameters.getToolId());
        validateVersionString(parameters.getToolVersion());
        ExecutionSetup executionSetup = generateSingleToolExecutionSetup(parameters);
        return executeConfiguredWorkflow(executionSetup, consoleRowReceiver, false);
    }

    /**
     * Executes a previously published workflow template.
     * 
     * @param workflowId the id of the published workflow template
     * @param sessionToken the session token
     * @param inputFilesDir the local file system path to read input files from
     * @param outputFilesDir the local file system path to write output files to
     * @param consoleRowReceiver an optional listener for all received ConsoleRows; pass null to deactivate
     * @param uncompressedUpload whether the upload should be uncompressed
     * @param simpleDescription whether the simple description format should be used
     * @return the state the generated workflow finished in
     * @throws IOException on I/O errors
     * @throws WorkflowExecutionException on workflow execution errors
     */
    @Override
    public FinalWorkflowState runPublishedWorkflowTemplate(String workflowId, String sessionToken, File inputFilesDir, File outputFilesDir,
        SingleConsoleRowsProcessor consoleRowReceiver, boolean uncompressedUpload, boolean simpleDescription)
        throws IOException,
        WorkflowExecutionException {
        validateIdString(workflowId);
        // TODO validate version once added
        ExecutionSetup executionSetup =
            generateWorkflowExecutionSetup(workflowId, sessionToken, inputFilesDir, outputFilesDir, uncompressedUpload, simpleDescription);
        boolean neverDeleteExecutionData = false;
        String keepData = persistentSettingsService.readStringValue(PUBLISHED_WF_KEEP_DATA_KEY_PREFIX + workflowId);
        if (keepData != null) {
            neverDeleteExecutionData = Boolean.parseBoolean(keepData);
        }
        return executeConfiguredWorkflow(executionSetup, consoleRowReceiver, neverDeleteExecutionData);
    }

    /**
     * Checks if the given workflow file can be used with the "wf-run" console command, and if this check is positive, the workflow file is
     * published under the given id.
     * 
     * @param wfFile the workflow file
     * @param placeholdersFile the placeholders file
     * @param publishId the id by which the workflow file should be made available
     * @param groupName name of the palette group in which the workflow will be shown
     * @param outputReceiver receiver for user feedback
     * @param persistent make the publishing persistent
     * @param neverDeleteExecutionData never delete workflow execution data
     * @throws WorkflowExecutionException on failure to load/parse the workflow file
     */
    @Override
    public void checkAndPublishWorkflowFile(File wfFile, File placeholdersFile, String publishId, String groupName,
        TextOutputReceiver outputReceiver, boolean persistent, boolean neverDeleteExecutionData) throws WorkflowExecutionException {

        validateIdString(publishId);

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
            } catch (WorkflowFileException e) { // review migration code, which was introduced due to changed exception
                                                // type
                throw new WorkflowExecutionException(
                    "Failed to validate placeholders file: " + wfFile.getAbsolutePath(), e);
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
                    if (groupName != null) {
                        persistentSettingsService.saveStringValue(PUBLISHED_WF_GROUP_KEY_PREFIX + publishId, groupName);
                    }
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
                    outputReceiver.addOutput(
                        StringUtils.format("Successfully updated the published workflow \"%s\"", publishId));
                }
            } catch (IOException e) {
                // avoid dangling, undefined workflow files on failure
                publishedWorkflowTemplates.remove(publishId);
                FileUtils.deleteQuietly(workflowStorageFile);
                throw new WorkflowExecutionException("Error publishing workflow file " + wfFile.getAbsolutePath());
            }
        }

        persistentSettingsService.saveStringValue(PUBLISHED_WF_KEEP_DATA_KEY_PREFIX + publishId,
            Boolean.toString(neverDeleteExecutionData));

        addOrReplaceWorkflowInWfTokens(publishId, groupName, wd);
    }

    @Override
    public void unpublishWorkflowForId(String publishId, TextOutputReceiver outputReceiver)
        throws WorkflowExecutionException {

        validateIdString(publishId);

        String removed = publishedWorkflowTemplates.remove(publishId);
        publishedWorkflowTemplatePlaceholders.remove(publishId);

        // always try to delete the storage files; if publishing was temporary, they are
        // simply not found
        File workflowStorageFile = getWorkflowStorageFile(publishId);
        if (workflowStorageFile.isFile()) {
            try {
                Files.delete(workflowStorageFile.toPath());
            } catch (IOException e) {
                throw new WorkflowExecutionException(
                    "Failed to unpublish the specified workflow; its storage file may be write-protected");
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
            outputReceiver.addOutput(
                StringUtils.format("ERROR: There is no workflow with id \"%s\" to unpublish", publishId));
        }

        wfTokens.remove(publishId);
        simpleWfTokens.remove(publishId);
        
        persistentSettingsService.delete(PUBLISHED_WF_KEEP_DATA_KEY_PREFIX + publishId);
        persistentSettingsService.delete(PUBLISHED_WF_GROUP_KEY_PREFIX + publishId);
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
        List<DistributedComponentEntry> availableTools = getMatchingPublishedTools();

        // note: not strictly necessary, but gives more consistent error messages
        // instead of "tool not found"
        validateIdString(toolId);
        validateVersionString(toolVersion);

        // only needed for nodeId == null to detect ambiguous matches
        DistributedComponentEntry nodeMatch = null;

        // TODO once components are cached, optimize with map lookup
        for (DistributedComponentEntry compInst : availableTools) {
            ComponentInterface compInterface = compInst.getComponentInterface();
            // TODO (p2) "display name" sounds odd here, but seems to be the public id;
            // check
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
                            throw new WorkflowExecutionException(StringUtils.format(
                                "Tool selection is ambiguous without a node id; "
                                    + "tool '%s', version '%s' is provided by more than one node",
                                toolId, toolVersion));
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
                throw new WorkflowExecutionException(StringUtils
                    .format("No matching tool for tool '%s' in version '%s'", toolId, toolVersion, nodeId));
            }
        } else {
            throw new WorkflowExecutionException(StringUtils.format(
                "No matching tool for tool '%s' in version '%s', " + "running on a node with id '%s'", toolId,
                toolVersion, nodeId));
        }

    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindWorkflowExecutionService(HeadlessWorkflowExecutionService newInstance) {
        this.workflowExecutionService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindPersistentSettingsService(PersistentSettingsService newInstance) {
        this.persistentSettingsService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindPlatformService(PlatformService newInstance) {
        this.platformService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindDistributedComponentKnowledgeService(DistributedComponentKnowledgeService newInstance) {
        this.componentKnowledgeService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindLocalSystemMonitoringAggregationService(LocalSystemMonitoringAggregationService newInstance) {
        this.localSystemMonitoringAggregationService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindEmbeddedSshServerControl(EmbeddedSshServerControl newInstance) {
        this.embeddedSshServerControl = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindConfigurationService(ConfigurationService newInstance) {
        this.configurationService = newInstance;
    }

    // Restores workflow templates from storage. Does NOT initialize the workflow
    // tokens, because for that the
    // workflow description has to be loaded, which would fail at this point as the
    // components are not yet available.
    private void restoreWorkflowTemplatesFromPublishedWfStorage() {
        // initialize the storage location for published workflows and placeholder data
        publishedWfStorageDir = new File(
            configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTERNAL_DATA), "ra/published-wf");
        publishedWfStorageDir.mkdirs();
        if (!publishedWfStorageDir.isDirectory()) {
            log.error("Failed to create Remote Access workflow storage directory "
                + publishedWfStorageDir.getAbsolutePath());
            publishedWfStorageDir = null;
            return;
        }

        wfTokens = new HashMap<String, String[]>();
        simpleWfTokens = new HashMap<String, String[]>();
        wfInputs = new HashMap<String, Map<String, String>>();
        wfOutputs = new HashMap<String, Map<String, String>>();
        // restore persisted data
        for (File f : publishedWfStorageDir.listFiles()) {
            String filename = f.getName();
            if (filename.endsWith(PUBLISHED_WF_DATA_FILE_SUFFIX)) {
                String wfId = filename.substring(0, filename.length() - PUBLISHED_WF_DATA_FILE_SUFFIX.length());
                if (initWorkflowTokens(f, wfId)) {
                    try {
                        publishedWorkflowTemplates.put(wfId, FileUtils.readFileToString(f));
                    } catch (IOException e) {
                        log.error("Failed to restore data of published RemoteAccess workflow from storage file " + f.getAbsolutePath(), e);
                    }
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

        // TODO check for placeholder data without a workflow file? only sanity check;
        // no actual harm in them - misc_ro
    }

    // returns true if successful
    private boolean initWorkflowTokens(File f, String wfId) {
        // Restore tokens for remote workflow listing
        WorkflowDescription wd = null;
        try {
            TextOutputReceiver outputReceiver = new TextOutputReceiver() {

                @Override
                public void addOutput(String line) {
                    log.debug("Output of command-line batch command(s): " + line);
                }

                @Override
                public void onStart() {}

                @Override
                public void onFatalError(Exception e) {
                    log.error(e.getMessage());
                }

                @Override
                public void onFinished() {}
            };

            wd = workflowExecutionService.loadWorkflowDescriptionFromFileConsideringUpdates(f,
                new HeadlessWorkflowDescriptionLoaderCallback(outputReceiver), true);
            if (validateWorkflowFileAsTemplate(wd, outputReceiver)) {
                String groupName = persistentSettingsService.readStringValue(PUBLISHED_WF_GROUP_KEY_PREFIX + wfId);
                addOrReplaceWorkflowInWfTokens(wfId, groupName, wd);
                return true;
            } else {
                log.error("Failed to restore permanently published workflow " + wfId + ". Probably it was published with an older version"
                    + " of RCE. Please publish the workflow again with the current RCE version.");
                return false;
            }
        } catch (WorkflowFileException | WorkflowExecutionException e) { // review migration code, which was introduced due to changed
                                                                         // exception type
            log.error("Failed to load published workflow file: " + f.getAbsolutePath(), e);
            return false;
        }
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

    private boolean isComponentSuitableAsRemoteAccessTool(DistributedComponentEntry entry) {
        ComponentInterface compInterf = entry.getComponentInterface();
        // validate id and version
        if (!checkIdString(compInterf.getDisplayName()) || !checkVersionString(compInterf.getVersion())) {
            return false;
        }

        // Only user-integrated tools are published via remote access, not RCE standard
        // components
        if (!compInterf.getIdentifierAndVersion().startsWith("de.rcenvironment.integration")) {
            return false;
        }

        if (!entry.getDeclaredPermissionSet().isPublic()) {
            return false;
        }

        return true;
    }

    private List<DistributedComponentEntry> getMatchingPublishedTools() {
        List<DistributedComponentEntry> components = new ArrayList<>();
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        for (DistributedComponentEntry entry : compKnowledge.getKnownSharedInstallations()) {
            if (isComponentSuitableAsRemoteAccessTool(entry)) {
                components.add(entry);
            }
        }
        return components;
    }

    private DistributedComponentEntry getMatchingComponentInstallationForTool(String toolName, String toolVersion,
        String toolNodeId) {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        DistributedComponentEntry matchingComponent = null;
        for (DistributedComponentEntry entry : compKnowledge.getKnownSharedInstallations()) {
            if (entry.getComponentInterface().getDisplayName().equals(toolName)
                && entry.getComponentInterface().getVersion().equals(toolVersion)
                && entry.getNodeId().equals(toolNodeId)) {
                matchingComponent = entry;
            }
        }
        return matchingComponent;
    }

    private String getFullIdentifierForTool(String toolNameAndVersion) {
        String[] splitToolId = toolNameAndVersion.split("/");
        String toolName = splitToolId[0];
        String toolVersion = splitToolId[1];
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        String fullId = null;
        for (DistributedComponentEntry entry : compKnowledge.getKnownSharedInstallations()) {
            if (entry.getComponentInterface().getDisplayName().equals(toolName)
                && entry.getComponentInterface().getVersion().equals(toolVersion)) {
                fullId = entry.getComponentInterface().getIdentifierAndVersion();
            }
        }
        return fullId;
    }

    private ComponentInstallation getInputLoaderComponentInstallation() {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        ComponentInstallation component = null;
        for (DistributedComponentEntry entry : compKnowledge.getAllLocalInstallations()) {
            if (entry.getComponentInterface().getIdentifierAndVersion().startsWith(DE_RCENVIRONMENT_SCPINPUTLOADER)) {
                component = entry.getComponentInstallation();
            }
        }
        return component;
    }

    private ComponentInstallation getOutputCollectorComponentInstallation() {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        ComponentInstallation component = null;
        for (DistributedComponentEntry entry : compKnowledge.getAllLocalInstallations()) {
            if (entry.getComponentInterface().getIdentifierAndVersion()
                .startsWith(DE_RCENVIRONMENT_SCPOUTPUTCOLLECTOR)) {
                component = entry.getComponentInstallation();
            }
        }
        return component;
    }

    private void printComponentsListAsCsv(TextOutputReceiver outputReceiver,
        Map<LogicalNodeId, SystemLoadInformation> systemLoadData) {
        final CSVFormat csvFormat = CSVFormat.newFormat(' ').withQuote('"').withQuoteMode(QuoteMode.ALL);
        if (toolTokens != null) {
            for (Object[] tokens : toolTokens) {
                if (systemLoadData != null) {
                    String nodeId = (String) tokens[2];
                    LogicalNodeId nodeIdObj = NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(nodeId);

                    final SystemLoadInformation loadDataEntry = systemLoadData.get(nodeIdObj);
                    // two-step checking as there may be no load data available for that node
                    final double cpuAvg;
                    final int numSamples;
                    final int timeSpan;
                    final long availableRam;
                    if (loadDataEntry != null) {
                        final AverageOfDoubles cpuLoadAvg = loadDataEntry.getCpuLoadAvg();
                        cpuAvg = cpuLoadAvg.getAverage() * PERCENT_MULTIPLIER;
                        numSamples = cpuLoadAvg.getNumSamples();
                        timeSpan = cpuLoadAvg.getNumSamples()
                            * LocalSystemMonitoringAggregationService.SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC;
                        availableRam = loadDataEntry.getAvailableRam();
                    } else {
                        cpuAvg = DOUBLE_NO_DATA_PLACEHOLDER;
                        numSamples = INT_NO_DATA_PLACEHOLDER;
                        timeSpan = INT_NO_DATA_PLACEHOLDER;
                        availableRam = INT_NO_DATA_PLACEHOLDER;
                    }
                    String line = csvFormat.format(tokens, StringUtils.format(FORMAT_2F, cpuAvg), numSamples, timeSpan, availableRam);
                    outputReceiver.addOutput(line);
                } else {
                    String line = csvFormat.format(tokens);
                    outputReceiver.addOutput(line);
                }
            }
        }
    }

    private Map<String, Map<String, Object>> extractRawMetadata(EndpointDefinition ed) {
        Map<String, Map<String, Object>> rawMetadata = new HashMap<String, Map<String, Object>>();
        for (String key : ed.getMetaDataDefinition().getMetaDataKeys()) {
            Map<String, Object> metaDataForKey = new HashMap<String, Object>();
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUI_NAME, ed.getMetaDataDefinition().getGuiName(key));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUI_POSITION,
                Integer.toString(ed.getMetaDataDefinition().getGuiPosition(key)));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_GUIGROUP, ed.getMetaDataDefinition().getGuiGroup(key));
            metaDataForKey.put(KEY_POSSIBLE_VALUES, ed.getMetaDataDefinition().getPossibleValues(key));
            metaDataForKey.put(KEY_DEFAULT_VALUE, ed.getMetaDataDefinition().getDefaultValue(key));
            metaDataForKey.put(EndpointDefinitionConstants.KEY_VISIBILITY,
                ed.getMetaDataDefinition().getVisibility(key));
            rawMetadata.put(key, metaDataForKey);
        }
        return rawMetadata;
    }

    private void printComponentsListAsTokens(TextOutputReceiver outputReceiver,
        Map<LogicalNodeId, SystemLoadInformation> systemLoadData) {
        outputReceiver.addOutput(Integer.toString(toolTokens.size())); // number of entries
        // print number of tokens per entry
        if (systemLoadData != null) {
            outputReceiver.addOutput("11");
        } else {
            outputReceiver.addOutput("7");
        }

        for (String[] tokens : toolTokens) {
            for (int i = 0; i < tokens.length; i++) {
                outputReceiver.addOutput(tokens[i]);
            }

            if (systemLoadData != null) {
                String nodeId = (String) tokens[2];
                LogicalNodeId nodeIdObj = NodeIdentifierUtils.parseLogicalNodeIdStringWithExceptionWrapping(nodeId);
                // TODO (p3) extract common code with above method?
                final SystemLoadInformation loadDataEntry = systemLoadData.get(nodeIdObj);
                // two-step checking as there may be no load data available for that node
                final double cpuAvg;
                final int numSamples;
                final int timeSpan;
                final long availableRam;
                if (loadDataEntry != null) {
                    final AverageOfDoubles cpuLoadAvg = loadDataEntry.getCpuLoadAvg();
                    cpuAvg = cpuLoadAvg.getAverage() * PERCENT_MULTIPLIER;
                    numSamples = cpuLoadAvg.getNumSamples();
                    timeSpan = cpuLoadAvg.getNumSamples()
                        * LocalSystemMonitoringAggregationService.SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC;
                    availableRam = loadDataEntry.getAvailableRam();
                } else {
                    cpuAvg = DOUBLE_NO_DATA_PLACEHOLDER;
                    numSamples = INT_NO_DATA_PLACEHOLDER;
                    timeSpan = INT_NO_DATA_PLACEHOLDER;
                    availableRam = INT_NO_DATA_PLACEHOLDER;
                }

                outputReceiver.addOutput(StringUtils.format(FORMAT_2F, cpuAvg));
                outputReceiver.addOutput(Integer.toString(numSamples));
                outputReceiver.addOutput(Integer.toString(timeSpan));
                outputReceiver.addOutput(Long.toString(availableRam));
            }
        }
    }

    private boolean validateWorkflowFileAsTemplate(WorkflowDescription wd, TextOutputReceiver outputReceiver)
        throws WorkflowExecutionException {
        validateEquals(WorkflowConstants.CURRENT_WORKFLOW_VERSION_NUMBER, wd.getWorkflowVersion(),
            "Invalid workflow file version");
        int foundInputLoaders = 0;
        int foundOutputCollectors = 0;
        for (WorkflowNode node : wd.getWorkflowNodes()) {
            outputReceiver.addOutput(OUTPUT_INDENT + "Checking component \"" + node.getName() + "\"  ["
                + node.getIdentifierAsObject().toString() + "]");
            final ComponentDescription compDesc = node.getComponentDescription();
            final String compId = compDesc.getIdentifier();
            final String compVersion = compDesc.getVersion();
            if (compId.contains(DE_RCENVIRONMENT_SCPINPUTLOADER)) {
                validateEquals(REQUIRED_INPUTLOADER_VERSION, compVersion, "Invalid component version");
                foundInputLoaders++;
            } else if (compId.contains(DE_RCENVIRONMENT_SCPOUTPUTCOLLECTOR)) {
                validateEquals(REQUIRED_OUTPUTCOLLECTOR_VERSION, compVersion, "Invalid component version");
                foundOutputCollectors++;
            }
        }
        // check for completeness
        if (foundInputLoaders == 1 && foundOutputCollectors == 1) {
            outputReceiver.addOutput("Validation successful.");
            return true;
        } else {
            outputReceiver.addOutput(
                "Validation failed: The workflow has to contain exactly one input loader and exactly one output collector.");
            return false;
        }
    }

    private void validateEquals(Object expected, Object actual, String message) throws WorkflowExecutionException {
        if (!expected.equals(actual)) {
            throw new WorkflowExecutionException(
                StringUtils.format("%s: Expected \"%s\", but found \"%s\"", message, expected, actual));
        }
    }

    // returns boolean result
    private boolean checkIdString(String id) {
        return !CommonIdRules.validateCommonIdRules(id).isPresent();
    }
    
 // returns boolean result
    private boolean checkVersionString(String id) {
        return !CommonIdRules.validateCommonVersionStringRules(id).isPresent();
    }

    // throws exception on failure
    private void validateIdString(String id) throws WorkflowExecutionException {
        // TODO add integration for high-level commands using this
        Optional<String> valdationErrorMessage = CommonIdRules.validateCommonIdRules(id);
        if (valdationErrorMessage.isPresent()) {
            throw new WorkflowExecutionException(
                "Invalid tool id or workflow id \"" + id + "\": " + valdationErrorMessage);
        }
    }
    
    // throws exception on failure
    private void validateVersionString(String id) throws WorkflowExecutionException {
        // TODO add integration for high-level commands using this
        Optional<String> valdationErrorMessage = CommonIdRules.validateCommonVersionStringRules(id);
        if (valdationErrorMessage.isPresent()) {
            throw new WorkflowExecutionException(
                "Invalid version \"" + id + "\": " + valdationErrorMessage);
        }
    }

    private String readFile(File placeholdersFile) throws IOException {
        return FileUtils.readFileToString(placeholdersFile, WORKFLOW_FILE_ENCODING);
    }

    private void renameAsOld(File outputFilesDir) {
        File tempDestination = new File(outputFilesDir.getParentFile(),
            outputFilesDir.getName() + ".old." + System.currentTimeMillis());
        outputFilesDir.renameTo(tempDestination);
        if (outputFilesDir.isDirectory()) {
            log.warn("Tried to move directory " + outputFilesDir.getAbsolutePath() + " to "
                + tempDestination.getAbsolutePath() + ", but it is still present");
        }
    }

    private ExecutionSetup generateSingleToolExecutionSetup(RemoteComponentExecutionParameter parameterObject)
        throws IOException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String timestampString = dateFormat.format(new Date());

        // Build workflow description containing the tool, an scp input loader and an
        // scp
        // output collector
        WorkflowDescription workflowDesc = new WorkflowDescription(UUID.randomUUID().toString());
        workflowDesc.setWorkflowVersion(5);
        workflowDesc.setName("Remote_Tool_Access-" + timestampString + "-" + parameterObject.getToolId());

        final DistributedComponentEntry matchingComponentEntry = getMatchingComponentInstallationForTool(
            parameterObject.getToolId(), parameterObject.getToolVersion(), parameterObject.getToolNodeId());
        WorkflowNode tool = new WorkflowNode(
            new ComponentDescription(matchingComponentEntry.getComponentInstallation()));
        WorkflowNode inputloader = new WorkflowNode(new ComponentDescription(getInputLoaderComponentInstallation()));
        WorkflowNode outputcollector = new WorkflowNode(
            new ComponentDescription(getOutputCollectorComponentInstallation()));
        tool.setName(parameterObject.getToolId());
        tool.setLocation(NUMBER_400, NUMBER_200);
        inputloader.setName("Scp Input Loader");
        inputloader.getConfigurationDescription().setConfigurationValue("UploadDirectory",
            parameterObject.getInputFilesDir().getAbsolutePath());
        inputloader.getConfigurationDescription().setConfigurationValue("UncompressedUpload",
            Boolean.toString(parameterObject.isUncompressedUpload()));
        inputloader.getConfigurationDescription().setConfigurationValue("SimpleDescriptionFormat",
            Boolean.toString(parameterObject.isSimpleDescriptionFormat()));
        inputloader.setLocation(NUMBER_200, NUMBER_200);
        outputcollector.setName("Scp output collector");
        outputcollector.getConfigurationDescription().setConfigurationValue("DownloadDirectory",
            parameterObject.getOutputFilesDir().getAbsolutePath());
        outputcollector.getConfigurationDescription().setConfigurationValue("UncompressedDownload",
            Boolean.toString(parameterObject.isUncompressedUpload()));
        outputcollector.getConfigurationDescription().setConfigurationValue("SimpleDescriptionFormat",
            Boolean.toString(parameterObject.isSimpleDescriptionFormat()));
        outputcollector.setLocation(NUMBER_600, NUMBER_200);
        workflowDesc.addWorkflowNode(inputloader);
        workflowDesc.addWorkflowNode(tool);
        workflowDesc.addWorkflowNode(outputcollector);

        if (parameterObject.getDynInputDesc() != null) {
            // Parse dynamic inputs and outputs from the parameter string and add them to
            // the tools workflow node
            Set<Map<String, Object>> dynInputsSet = mapper.readValue(parameterObject.getDynInputDesc(),
                new HashSet<Map<String, Object>>().getClass());
            for (Map<String, Object> dynInput : dynInputsSet) {
                String inputName = (String) dynInput.get(EndpointDefinitionConstants.KEY_NAME);
                String identifier = (String) dynInput.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
                DataType type = DataType.valueOf((String) dynInput.get(KEY_DATA_TYPE));
                Map<String, String> metaData = (Map<String, String>) dynInput.get(KEY_META_DATA);
                tool.getInputDescriptionsManager().addDynamicEndpointDescription(identifier, inputName, type, metaData);
            }
        }
        if (parameterObject.getDynOutputDesc() != null) {
            Set<Map<String, Object>> dynOutputsSet = mapper.readValue(parameterObject.getDynOutputDesc(),
                new HashSet<Map<String, Object>>().getClass());
            for (Map<String, Object> dynOutput : dynOutputsSet) {
                String outputName = (String) dynOutput.get(EndpointDefinitionConstants.KEY_NAME);
                String identifier = (String) dynOutput.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
                DataType type = DataType.valueOf((String) dynOutput.get(KEY_DATA_TYPE));
                Map<String, String> metaData = (Map<String, String>) dynOutput.get(KEY_META_DATA);
                tool.getOutputDescriptionsManager().addDynamicEndpointDescription(identifier, outputName, type,
                    metaData);
            }
        }

        Set<String> nonReqInputsSet = new HashSet<String>();

        // Read set of non-required inputs ("Required" or "RequiredIfConnected" on
        // client side)
        if (parameterObject.getNotRequiredInputs() != null) {
            nonReqInputsSet = mapper.readValue(parameterObject.getNotRequiredInputs(),
                new HashSet<String>().getClass());
        }

        // Configure InputLoader with the tools inputs and create connections
        EndpointDescriptionsManager inputLoaderOutputs = inputloader.getOutputDescriptionsManager();
        for (EndpointDescription inputDesc : tool.getInputDescriptionsManager().getEndpointDescriptions()) {
            String inputName = inputDesc.getName();
            DataType dataType = inputDesc.getDataType();
            EndpointDescription outputDesc = inputLoaderOutputs.addDynamicEndpointDescription("default", inputName,
                dataType, new HashMap<String, String>());
            Connection inputConnection = new Connection(inputloader, outputDesc, tool, inputDesc);
            workflowDesc.addConnection(inputConnection);
            // Add execution constraint "NotRequired" if necessary
            if (nonReqInputsSet.contains(inputName)) {
                Map<String, String> metaData = inputDesc.getMetaData();
                metaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                    EndpointDefinition.InputExecutionContraint.NotRequired.toString());
                tool.getInputDescriptionsManager().editStaticEndpointDescription(inputName, dataType, metaData);
                inputDesc.setMetaDataValue(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                    EndpointDefinition.InputExecutionContraint.NotRequired.toString());
            }
        }
        // Configure OutputCollector with the tools outputs and create connections
        EndpointDescriptionsManager outputCollectorInputs = outputcollector.getInputDescriptionsManager();
        for (EndpointDescription outputDesc : tool.getOutputDescriptionsManager().getEndpointDescriptions()) {
            String inputName = outputDesc.getName();
            DataType dataType = outputDesc.getDataType();
            EndpointDescription inputDesc = outputCollectorInputs.addDynamicEndpointDescription("default", inputName,
                dataType, new HashMap<String, String>());
            Connection outputConnection = new Connection(tool, outputDesc, outputcollector, inputDesc);
            workflowDesc.addConnection(outputConnection);
        }

        // TODO Determine on which (logical) node the tool should run (in case several
        // tools with same id are available).

        File wfFile = tempFileService.createTempFileFromPattern("rta-*.wf");
        WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
        ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(workflowDesc);
        FileUtils.writeByteArrayToFile(wfFile, content.toByteArray());
        return new ExecutionSetup(wfFile, null, parameterObject.getSessionToken(), parameterObject.getInputFilesDir(),
            parameterObject.getOutputFilesDir());
    }

    private ExecutionSetup generateWorkflowExecutionSetup(String workflowId, String sessionToken, File inputFilesDir, File outputFilesDir,
        boolean uncompressedUpload, boolean simpleDescriptionFormat) throws IOException, WorkflowExecutionException {
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
            // .replace(WF_PLACEHOLDER_PARAMETERS, StringUtils.escapeAsJsonStringContent(parameterString, false)) // prevent injection
            .replace(WF_PLACEHOLDER_TIMESTAMP, timestampString)
            .replace(WF_PLACEHOLDER_INPUT_DIR, formatPathForWorkflowFile(inputFilesDir))
            .replace(WF_PLACEHOLDER_OUTPUT_PARENT_DIR, formatPathForWorkflowFile(outputFilesDir))
            .replace(WF_PLACEHOLDER_SIMPLE_DESCRIPTION_FORMAT, Boolean.toString(simpleDescriptionFormat))
            .replace(WF_PLACEHOLDER_UNCOMPRESSED_UPLOAD, Boolean.toString(uncompressedUpload))
            .replace(WF_PLACEHOLDER_UNCOMPRESSED_DOWNLOAD, Boolean.toString(uncompressedUpload));
        File wfFile = tempFileService.createTempFileFromPattern("rwa-*.wf");
        FileUtils.write(wfFile, workflowContent, WORKFLOW_FILE_ENCODING);
        return new ExecutionSetup(wfFile, placeholdersFile, sessionToken, inputFilesDir, outputFilesDir);
    }

    private FinalWorkflowState executeConfiguredWorkflow(ExecutionSetup executionSetup,
        SingleConsoleRowsProcessor customConsoleRowReceiver, boolean neverDeleteExecutionData) throws WorkflowExecutionException {
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

        // TODO review >5.0.0: remove this output capture, as it is only used for debug
        // output? - misc_ro
        CapturingTextOutReceiver outputReceiver = new CapturingTextOutReceiver();

        // TODO specify log directory?
        HeadlessWorkflowExecutionContextBuilder exeContextBuilder;
        try {
            exeContextBuilder = new HeadlessWorkflowExecutionContextBuilder(executionSetup.getWorkflowFile(),
                logDir);
        } catch (InvalidFilenameException e) {
            // This exception should never occur since the name of the workflow file used
            // here is generated by the
            // generateWorkflowExecutionSetup method and is always valid
            throw new IllegalStateException();
        }
        exeContextBuilder.setPlaceholdersFile(executionSetup.getPlaceholderFile());
        exeContextBuilder.setTextOutputReceiver(outputReceiver);
        exeContextBuilder.setSingleConsoleRowsProcessor(customConsoleRowReceiver);
        exeContextBuilder.setAbortIfWorkflowUpdateRequired(true); // fail on out-of-date templates

        if (neverDeleteExecutionData) {
            exeContextBuilder.setDeletionBehavior(DeletionBehavior.Never);
            exeContextBuilder.setDisposalBehavior(DisposalBehavior.Never);
        }

        WorkflowExecutionException executionException = null;
        FinalWorkflowState finalState = FinalWorkflowState.FAILED;
        try {
            HeadlessWorkflowExecutionContext context = exeContextBuilder.buildExtended();
            WorkflowExecutionInformation execInf = workflowExecutionService.startHeadlessWorkflowExecution(context);
            sessionTokenToWfExecInf.put(executionSetup.getSessionToken(), execInf);
            finalState = workflowExecutionService.waitForWorkflowTerminationAndCleanup(context);
            sessionTokenToWfExecInf.remove(executionSetup.getSessionToken());
        } catch (WorkflowExecutionException e) {
            executionException = e;
            File exceptionLogFile = new File(logDir, "error.log");
            // create a log file so the error cause is accessible via the log directory
            try {
                FileUtils.writeStringToFile(exceptionLogFile,
                    "Workflow execution failed with an error: " + e.toString());
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
                log.warn("Tried to rename input directory " + inputFilesDir.getAbsolutePath() + " to "
                    + tempDestination.getAbsolutePath() + ", but it is still present");
            }
        }

        if (executionException != null) {
            throw executionException;
        }

        try {
            tempFileService.disposeManagedTempDirOrFile(executionSetup.getWorkflowFile());
        } catch (IOException e) {
            log.warn("Could not delete temporary workflow file");
        }

        return finalState;
    }

    private CharSequence formatPathForWorkflowFile(File directory) {
        return directory.getAbsolutePath().replaceAll("\\\\", "/"); // double escaping for java+regexp; replaces
                                                                    // "\"->"/"
    }

    @Override
    public void cancelToolOrWorkflow(String sessionToken) {
        WorkflowExecutionInformation wfExecInf = sessionTokenToWfExecInf.get(sessionToken);

        if (wfExecInf != null) {
            try {
                workflowExecutionService.cancel(wfExecInf.getWorkflowExecutionHandle());
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.warn(StringUtils.format("Failed to cancel workflow '%s'; cause: %s",
                    wfExecInf.getExecutionIdentifier(), e.getMessage()));
            }
        } else {
            log.debug(
                StringUtils.format("Failed to cancel workflow for session token '%s'; it was not running or already finished.",
                    sessionToken));
        }

    }

    @Override
    public void getToolDocumentation(TextOutputReceiver outputReceiver, String toolId, String nodeId, String hashValue,
        File outputFilePath) {
        String fullToolId = getFullIdentifierForTool(toolId);
        try {
            File doc = toolDocService.getToolDocumentation(fullToolId, nodeId, hashValue);
            FileUtils.copyDirectory(doc, outputFilePath);
        } catch (RemoteOperationException | IOException e) {
            log.warn("Failed to download tool documentaion; cause; " + e.getMessage());
        }
    }

    @Override
    public void getToolDocumentationList(TextOutputReceiver outputReceiver, String toolId) {
        String fullToolId = getFullIdentifierForTool(toolId);
        //fullToolID is null if the id belongs to a published workflow
        if (fullToolId != null) {
            Map<String, String> toolDocMap = toolDocService.getComponentDocumentationList(fullToolId);
            outputReceiver.addOutput(Integer.toString(toolDocMap.size()));
            // For each entry, output the hash value and node id
            for (Entry<String, String> docEntry : toolDocMap.entrySet()) {
                outputReceiver.addOutput(docEntry.getKey());
                outputReceiver.addOutput(docEntry.getValue());
            } 
        }
    }

}
