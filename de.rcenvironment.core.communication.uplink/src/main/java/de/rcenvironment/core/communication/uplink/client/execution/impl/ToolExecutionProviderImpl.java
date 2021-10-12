/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.client.execution.api.DataTransferUtils;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryDownloadReceiver;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionProviderEventCollector;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.DestinationIdUtils;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.api.UserComponentIdMappingService;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowExecutionService.DisposalBehavior;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescriptionPersistenceHandler;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Implementation of ToolExecutionProvider.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke (API adaptations)
 */
public class ToolExecutionProviderImpl implements ToolExecutionProvider {

    private static final String DE_RCENVIRONMENT_SCPOUTPUTCOLLECTOR = "de.rcenvironment.scpoutputcollector/";

    private static final String DE_RCENVIRONMENT_SCPINPUTLOADER = "de.rcenvironment.scpinputloader/";

    private static final String KEY_DATA_TYPE = "dataType";

    private static final String KEY_META_DATA = "metaData";

    private static final int NUMBER_600 = 600;

    private static final int NUMBER_200 = 200;

    private static final int NUMBER_400 = 400;

    private ToolExecutionRequest request;

    private File tempDir;

    private File inputDir;

    private File outputDir;

    private File createdWorkflowFile;

    private WorkflowExecutionInformation wfExecInf;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private UserComponentIdMappingService userComponentIdMappingService;

    private HeadlessWorkflowExecutionService workflowExecutionService;

    private final TempFileService tempFileService = TempFileServiceAccess.getInstance();

    private final Log log = LogFactory.getLog(getClass());

    public ToolExecutionProviderImpl(ToolExecutionRequest request) {
        this.request = request;
        try {
            tempDir = tempFileService.createManagedTempDir();
            inputDir = new File(tempDir, "input");
            outputDir = new File(tempDir, "output");
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        componentKnowledgeService = ServiceRegistry.createAccessFor(this).getService(DistributedComponentKnowledgeService.class);
        userComponentIdMappingService = ServiceRegistry.createAccessFor(this).getService(UserComponentIdMappingService.class);
        workflowExecutionService = ServiceRegistry.createAccessFor(this).getService(HeadlessWorkflowExecutionService.class);
    }

    @Override
    public DirectoryDownloadReceiver getInputDirectoryReceiver() {
        return new DirectoryDownloadReceiver() {

            @Override
            public void receiveDirectoryListing(List<String> relativePaths) throws IOException {
                DataTransferUtils.receiveDirectoryListing(relativePaths, inputDir);
            }

            @Override
            public void receiveFile(FileDataSource dataSource) throws IOException {
                DataTransferUtils.receiveFile(dataSource, inputDir);
            }
        };
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionProviderEventCollector eventCollector) throws OperationFailureException {
        prepareWorkflowFile();
        FinalWorkflowState state = executeConfiguredWorkflow(eventCollector);
        ToolExecutionResult result = new ToolExecutionResult();
        result.successful = state.equals(FinalWorkflowState.FINISHED);
        result.cancelled = state.equals(FinalWorkflowState.CANCELLED);
        return result; // dummy
    }

    @Override
    public void requestCancel() {
        if (wfExecInf != null) {
            try {
                workflowExecutionService.cancel(wfExecInf.getWorkflowExecutionHandle());
            } catch (ExecutionControllerException | RemoteOperationException e) {
                log.warn(StringUtils.format("Failed to cancel workflow '%s'; cause: %s",
                    wfExecInf.getExecutionIdentifier(), e.getMessage()));
            }
        } else {
            log.debug("Failed to cancel workflow; it was not running or already finished.");
        }
    }

    @Override
    public DirectoryUploadProvider getOutputDirectoryProvider() {
        return new DirectoryUploadProvider() {

            @Override
            public List<String> provideDirectoryListing() throws IOException {
                List<String> listOfDirs = new ArrayList<String>();
                DataTransferUtils.getDirectoryListing(outputDir, listOfDirs, "");
                return listOfDirs;
            }

            @Override
            public void provideFiles(DirectoryUploadContext uploadContext) throws IOException {
                DataTransferUtils.uploadDirectory(outputDir, uploadContext, "", ""); // TODO proper log prefix
                // TODO Remove this call as soon as onContextClosing is actually called
                cleanupTempFiles();
            }

        };
    }

    @Override
    // NEVER CALLED IN CURRENT CODE
    public void onContextClosing() {
        cleanupTempFiles();
    }

    private void cleanupTempFiles() {
        try {
            tempFileService.disposeManagedTempDirOrFile(createdWorkflowFile);
            tempFileService.disposeManagedTempDirOrFile(tempDir);
        } catch (IOException e) {
            log.warn("Could not delete temporary files");
        }
    }

    private void prepareWorkflowFile() throws OperationFailureException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        final String timestampString = dateFormat.format(new Date());

        // Build workflow description containing the tool, an scp input loader and an
        // scp output collector
        WorkflowDescription workflowDesc = new WorkflowDescription(UUID.randomUUID().toString());
        workflowDesc.setWorkflowVersion(5);
        workflowDesc.setName("Remote_Tool_Access-" + timestampString + "-" + request.getToolId());

        String internalToolId = userComponentIdMappingService.fromExternalToInternalId(request.getToolId());

        final DistributedComponentEntry matchingComponentEntry = getMatchingComponentInstallationForTool(
            internalToolId, request.getToolVersion(), DestinationIdUtils.getNodeIdFromQualifiedDestinationId(request.getDestinationId()));
        WorkflowNode tool = new WorkflowNode(
            new ComponentDescription(matchingComponentEntry.getComponentInstallation()));
        WorkflowNode inputloader = new WorkflowNode(new ComponentDescription(getInputLoaderComponentInstallation()));
        WorkflowNode outputcollector = new WorkflowNode(
            new ComponentDescription(getOutputCollectorComponentInstallation()));
        tool.setName(request.getToolId());
        tool.setLocation(NUMBER_400, NUMBER_200);
        inputloader.setName("Scp Input Loader");
        inputloader.getConfigurationDescription().setConfigurationValue("UploadDirectory",
            inputDir.getAbsolutePath());
        inputloader.getConfigurationDescription().setConfigurationValue("UncompressedUpload",
            Boolean.toString(false));
        inputloader.getConfigurationDescription().setConfigurationValue("SimpleDescriptionFormat",
            Boolean.toString(false));
        inputloader.setLocation(NUMBER_200, NUMBER_200);
        outputcollector.setName("Scp output collector");
        outputcollector.getConfigurationDescription().setConfigurationValue("DownloadDirectory",
            outputDir.getAbsolutePath());
        outputcollector.getConfigurationDescription().setConfigurationValue("UncompressedDownload",
            Boolean.toString(false));
        outputcollector.getConfigurationDescription().setConfigurationValue("SimpleDescriptionFormat",
            Boolean.toString(false));
        outputcollector.setLocation(NUMBER_600, NUMBER_200);
        workflowDesc.addWorkflowNode(inputloader);
        workflowDesc.addWorkflowNode(tool);
        workflowDesc.addWorkflowNode(outputcollector);

        for (Map<String, Object> dynInput : request.getDynamicInputs()) {
            String inputName = (String) dynInput.get(EndpointDefinitionConstants.KEY_NAME);
            String identifier = (String) dynInput.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
            DataType type = DataType.valueOf((String) dynInput.get(KEY_DATA_TYPE));
            Map<String, String> metaData = (Map<String, String>) dynInput.get(KEY_META_DATA);
            tool.getInputDescriptionsManager().addDynamicEndpointDescription(identifier, inputName, type, metaData);
        }
        for (Map<String, Object> dynOutput : request.getDynamicOutputs()) {
            String outputName = (String) dynOutput.get(EndpointDefinitionConstants.KEY_NAME);
            String identifier = (String) dynOutput.get(EndpointDefinitionConstants.KEY_IDENTIFIER);
            DataType type = DataType.valueOf((String) dynOutput.get(KEY_DATA_TYPE));
            Map<String, String> metaData = (Map<String, String>) dynOutput.get(KEY_META_DATA);
            tool.getOutputDescriptionsManager().addDynamicEndpointDescription(identifier, outputName, type,
                metaData);
        }

        // Set configuration values for tool
        for (Entry<String, String> entry : request.getProperties().entrySet()) {
            tool.getConfigurationDescription().setConfigurationValue(entry.getKey(), entry.getValue());
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
            if (request.getNonRequiredInputs().contains(inputName)) {
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

        try {
            createdWorkflowFile = tempFileService.createTempFileFromPattern("rta-*.wf");
            WorkflowDescriptionPersistenceHandler persistenceHandler = new WorkflowDescriptionPersistenceHandler();
            ByteArrayOutputStream content = persistenceHandler.writeWorkflowDescriptionToStream(workflowDesc);
            FileUtils.writeByteArrayToFile(createdWorkflowFile, content.toByteArray());
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

    private FinalWorkflowState executeConfiguredWorkflow(ToolExecutionProviderEventCollector eventCollector)
        throws OperationFailureException {
        HeadlessWorkflowExecutionContextBuilder exeContextBuilder;

        // move the output directory if it already exists to avoid collisions
        if (outputDir.isDirectory()) {
            renameAsOld(outputDir);
        }
        // log dir for the workflow execution service, will be deleted on success.
        File logDir = new File(outputDir.getParent(), "logs");
        if (logDir.isDirectory()) {
            renameAsOld(logDir);
        }
        logDir.mkdirs();

        // File for collecting console output
        // File consoleLogFile = new File(outputDir, "console.log");

        try {
            exeContextBuilder = new HeadlessWorkflowExecutionContextBuilder(createdWorkflowFile).setLogDirectory(logDir);
            exeContextBuilder.setSingleConsoleRowsProcessor(new SingleConsoleRowsProcessor() {

                @Override
                public void onConsoleRow(ConsoleRow consoleRow) {
                    // as a first step, these events are simply forwarded with their internal event types;
                    // consider whether these should be mapped/filtered -- misc_ro
                    // For now, only filter out the life cycle events --bode_br
                    if (!consoleRow.getType().equals(ConsoleRow.Type.LIFE_CYCLE_EVENT)) {
                        eventCollector.submitEvent(consoleRow.getType().name(), consoleRow.getPayload());
                    }
                    /*
                     * try { ConsoleRow.Type type = consoleRow.getType(); switch (type) { case TOOL_OUT: case TOOL_ERROR: case
                     * COMPONENT_ERROR: case COMPONENT_WARN: case COMPONENT_INFO: FileUtils.writeStringToFile(consoleLogFile,
                     * consoleRow.getPayload() + "\n", true); break; case LIFE_CYCLE_EVENT: break; default: break; }
                     * 
                     * } catch (IOException e) { log.warn("Could not write console row to log file."); }
                     */

                }
            });
            // Delete workflows on success
            exeContextBuilder.setDeletionBehavior(DeletionBehavior.OnExpected);
            exeContextBuilder.setDisposalBehavior(DisposalBehavior.OnExpected);
        } catch (InvalidFilenameException e) {
            // This exception should never occur since the name of the workflow file used
            // here is generated by the
            // generateWorkflowExecutionSetup method and is always valid
            throw new IllegalStateException();
        }

        WorkflowExecutionException executionException = null;
        FinalWorkflowState finalState = FinalWorkflowState.FAILED;
        try {
            HeadlessWorkflowExecutionContext context = exeContextBuilder.buildExtended();
            wfExecInf = workflowExecutionService.startHeadlessWorkflowExecution(context);
            finalState = workflowExecutionService.waitForWorkflowTerminationAndCleanup(context);
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
        log.debug("Finished remote access workflow.");

        // move the input directory to avoid future collisions
        if (inputDir.isDirectory()) {
            File tempDestination = new File(inputDir.getParentFile(), "input.old." + System.currentTimeMillis());
            inputDir.renameTo(tempDestination);
            if (inputDir.isDirectory()) {
                log.warn("Tried to rename input directory " + inputDir.getAbsolutePath() + " to "
                    + tempDestination.getAbsolutePath() + ", but it is still present");
            }
        }

        if (executionException != null) {
            throw new OperationFailureException("Tool execution failed.", executionException);
        }

        return finalState;
    }

    private DistributedComponentEntry getMatchingComponentInstallationForTool(String toolId, String toolVersion,
        String toolNodeId) {
        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();
        DistributedComponentEntry matchingComponent = null;
        for (DistributedComponentEntry entry : compKnowledge.getKnownSharedInstallations()) {
            if (entry.getComponentInterface().getIdentifier().equals(toolId)
                && entry.getComponentInterface().getVersion().equals(toolVersion)
                && entry.getNodeId().equals(toolNodeId)) {
                matchingComponent = entry;
            }
        }
        return matchingComponent;
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

    private void renameAsOld(File outputFilesDir) {
        File tempDestination = new File(outputFilesDir.getParentFile(),
            outputFilesDir.getName() + ".old." + System.currentTimeMillis());
        outputFilesDir.renameTo(tempDestination);
        if (outputFilesDir.isDirectory()) {
            log.warn("Tried to move directory " + outputFilesDir.getAbsolutePath() + " to "
                + tempDestination.getAbsolutePath() + ", but it is still present");
        }
    }
}
