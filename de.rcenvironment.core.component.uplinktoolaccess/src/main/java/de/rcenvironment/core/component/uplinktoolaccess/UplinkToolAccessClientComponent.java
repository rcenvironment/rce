/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.uplink.client.execution.api.DataTransferUtils;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryDownloadReceiver;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionClientSideSetup;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionService;
import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionSetup;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolExecutionHandle;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Component which runs a remote tool over a SSH connection.
 *
 * @author Brigitte Boden
 * @author Robert Mischke (API adaptations)
 */
public class UplinkToolAccessClientComponent extends DefaultComponent {

    private static final Log LOG = LogFactory.getLog(UplinkToolAccessClientComponent.class);

    private ObjectMapper mapper;

    private SshUplinkConnectionService uplinkService;

    private TempFileService tempFileService;

    private ComponentDataManagementService datamanagementService;

    private ComponentContext componentContext;

    private String toolId;

    private String toolVersion;

    private String destinationId;

    private String connectionId;

    private String authGroupId;

    private ComponentLog componentLog;

    private TypedDatumSerializer serializer;

    private ToolExecutionHandle toolExecutionHandle;

    private boolean componentFailed;

    private boolean componentCancelled;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        mapper = JsonUtils.getDefaultObjectMapper();
        tempFileService = TempFileServiceAccess.getInstance();
        datamanagementService = componentContext.getService(ComponentDataManagementService.class);
        uplinkService = componentContext.getService(SshUplinkConnectionService.class);
        serializer = componentContext.getService(TypedDatumService.class).getSerializer();
        toolId = componentContext.getConfigurationValue(UplinkToolAccessConstants.KEY_TOOL_ID);
        destinationId = componentContext.getConfigurationValue(UplinkToolAccessConstants.KEY_DESTINATION_ID);
        toolVersion = componentContext.getConfigurationValue(UplinkToolAccessConstants.KEY_TOOL_VERSION);
        connectionId = componentContext.getConfigurationValue(UplinkToolAccessConstants.KEY_CONNECTION);
        authGroupId = componentContext.getConfigurationValue(UplinkToolAccessConstants.KEY_AUTH_GROUP_ID);

        if (toolId == null || toolVersion == null || connectionId == null) {
            throw new ComponentException("Configuration for remote tool is not valid.");
        }
        SshUplinkConnectionSetup connection = uplinkService.getConnectionSetup(connectionId);
        if (connection == null) {
            throw new ComponentException("The uplink connection for this tool does not exist.");
        }
        componentLog.componentInfo(
            "Started on logical node " + componentContext.getServiceCallContext().getReceivingNode());

        if (treatStartAsComponentRun()) {
            processInputs();
        }
    }

    @Override
    public boolean treatStartAsComponentRun() {
        return componentContext.getInputs().isEmpty();
    }

    @Override
    public void processInputs() throws ComponentException {

        // Create temp directory
        File tempRootDir;
        // Directory containing all the inputs of the tool
        File uploadDir;
        Map<String, String> inputsMap = new HashMap<>();

        try {
            tempRootDir = tempFileService.createManagedTempDir();
            uploadDir = prepareUploadDirectory(tempRootDir, inputsMap);

        } catch (IOException e1) {
            throw new ComponentException("Upload directory could not be created.", e1);
        }

        // Get Uplink session
        Optional<ClientSideUplinkSession> optionalUplinkSession = uplinkService.getActiveSshUplinkSession(connectionId);
        if (!optionalUplinkSession.isPresent()) {
            throw new ComponentException(
                "Ready to process inputs, but the Uplink connection towards this remote tool is not active anymore");
        }
        ClientSideUplinkSession uplinkSession = optionalUplinkSession.get();

        Map<String, String> properties = getProperties();

        ToolExecutionClientSideSetup setup =
            // note: if you have an existing ToolMetadata object, use builder.copyFrom() in
            // actual code
            ToolExecutionClientSideSetup.newBuilder().toolId(toolId).toolVersion(toolVersion)
                .authGroupId(authGroupId).destinationId(destinationId)
                .nonRequiredInputs(createNotRequiredInputsSet())
                .dynamicInputs(createDynamicInputsSet(inputsMap)).dynamicOutputs(createDynamicOutputsSet())
                .properties(properties).build();

        // Directory for collecting all of the outputs of the tool
        File downloadDir = new File(tempRootDir, "output");

        final Lock remoteExecutionLock = new ReentrantLock();
        final Condition remoteExecutionFinished = remoteExecutionLock.newCondition();

        ToolExecutionEventHandler eventHandler =
            createToolExecutionEventHandler(uploadDir, downloadDir, remoteExecutionLock, remoteExecutionFinished);

        final Optional<ToolExecutionHandle> initiationResult = uplinkSession.initiateToolExecution(setup, eventHandler);
        if (initiationResult.isPresent()) {
            toolExecutionHandle = initiationResult.get();
        } else {
            throw new ComponentException(
                "Error while initiating remote tool execution; check the log output for details");
        }

        // Wait until context is closed
        remoteExecutionLock.lock();
        try {
            remoteExecutionFinished.await();
            // extractLog(downloadDir);
            extractAndSendOutputs(downloadDir);
        } catch (InterruptedException e) {
            componentLog.componentError("Waiting for remote execution failed. " + e.getMessage());
        } finally {
            remoteExecutionLock.unlock();
        }

        if (componentFailed) {
            throw new ComponentException("Remote tool execution failed.");
        }

    }

    private ToolExecutionEventHandler createToolExecutionEventHandler(File uploadDir, File downloadDir, final Lock remoteExecutionLock,
        final Condition remoteExecutionFinished) {
        return new ToolExecutionEventHandler() {

            @Override
            public DirectoryUploadProvider getInputDirectoryProvider() {
                return new DirectoryUploadProvider() {

                    @Override
                    public List<String> provideDirectoryListing() throws IOException {
                        List<String> listOfDirs = new ArrayList<String>();
                        DataTransferUtils.getDirectoryListing(uploadDir, listOfDirs, "");
                        return listOfDirs;
                    }

                    @Override
                    public void provideFiles(DirectoryUploadContext uploadContext) throws IOException {
                        DataTransferUtils.uploadDirectory(uploadDir, uploadContext, "");
                    }

                };
            }

            @Override
            public DirectoryDownloadReceiver getOutputDirectoryReceiver() {
                return new DirectoryDownloadReceiver() {

                    @Override
                    public void receiveDirectoryListing(List<String> relativePaths) throws IOException {
                        DataTransferUtils.receiveDirectoryListing(relativePaths, downloadDir);
                    }

                    @Override
                    public void receiveFile(FileDataSource dataSource) throws IOException {
                        try {
                            DataTransferUtils.receiveFile(dataSource, downloadDir);
                        } catch (IOException e) {
                            // TODO Throw correct exception type
                            LOG.error("Failed to receive output file: " + e.getMessage());
                        }
                    }

                };
            }

            @Override
            public void onOutputDownloadsStarting() {
                componentLog.componentInfo("Outputs download started.");
            }

            @Override
            public void onOutputDownloadsFinished() {
                componentLog.componentInfo("Outputs download finished.");
            }

            @Override
            public void onInputUploadsStarting() {
                componentLog.componentInfo("Inputs upload started.");
            }

            @Override
            public void onInputUploadsFinished() {
                componentLog.componentInfo("Inputs upload finished.");
            }

            @Override
            public void onExecutionStarting() {
                componentLog.componentInfo("Remote tool execution started.");
            }

            @Override
            public void processToolExecutionEvent(String type, String data) {
                ConsoleRow.Type consoleRowType = ConsoleRow.Type.valueOf(type);
                switch (consoleRowType) {
                case TOOL_OUT:
                    componentLog.toolStdout(data);
                    break;
                case TOOL_ERROR:
                    componentLog.toolStderr(data);
                    break;
                case COMPONENT_ERROR:
                    componentLog.componentError(data);
                    break;
                case COMPONENT_WARN:
                    componentLog.componentWarn(data);
                    break;
                case COMPONENT_INFO:
                    componentLog.componentInfo(data);
                    break;
                case LIFE_CYCLE_EVENT:
                    break;
                default:
                    componentLog.componentInfo("Tool event " + type + ": \"" + data + "\"");
                }

            }

            @Override
            public void onExecutionFinished(ToolExecutionResult executionResult) {
                componentLog.componentInfo("Remote tool execution finished.");
                if (!executionResult.successful && !(componentCancelled && executionResult.cancelled)) {
                    componentFailed = true;
                }
            }

            @Override
            public void onError(String message) {
                componentLog.componentError("Tool execution failed: " + message);
                componentFailed = true;
            }

            @Override
            public void onContextClosing() {
                remoteExecutionLock.lock();
                try {
                    remoteExecutionFinished.signal();
                } finally {
                    remoteExecutionLock.unlock();
                }
            }
        };
    }

    private Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        // get properties!
        for (String configKey : componentContext.getConfigurationKeys()) {
            String value = componentContext.getConfigurationValue(configKey);
            properties.put(configKey, componentContext.getConfigurationValue(configKey));
        }
        return properties;
    }

    @Override
    public synchronized void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
        componentLog.componentInfo("Cancelling tool...");
        componentCancelled = true;
        toolExecutionHandle.requestCancel();
    }

    private Set<Map<String, Object>> createDynamicOutputsSet() throws ComponentException {
        Set<Map<String, Object>> dynoutputsSet = new HashSet<>();
        for (String outputName : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(outputName)) {
                Map<String, Object> outputData = new HashMap<>();
                outputData.put(EndpointDefinitionConstants.KEY_IDENTIFIER,
                    componentContext.getDynamicOutputIdentifier(outputName));
                outputData.put(EndpointDefinitionConstants.KEY_NAME, outputName);
                outputData.put(UplinkToolAccessConstants.KEY_ENDPOINT_DATA_TYPE,
                    componentContext.getOutputDataType(outputName));
                Map<String, String> metaData = new HashMap<>();
                for (String metaDataKey : componentContext.getOutputMetaDataKeys(outputName)) {
                    metaData.put(metaDataKey, componentContext.getOutputMetaDataValue(outputName, metaDataKey));
                }
                outputData.put(UplinkToolAccessConstants.KEY_ENDPOINT_META_DATA, metaData);
                dynoutputsSet.add(outputData);
            }
        }

        return dynoutputsSet;
    }

    private Set<Map<String, Object>> createDynamicInputsSet(Map<String, String> inputsMap) throws ComponentException {
        Set<Map<String, Object>> dynInputsSet = new HashSet<>();
        for (String inputName : inputsMap.keySet()) {
            if (componentContext.isDynamicInput(inputName)) {
                Map<String, Object> inputData = new HashMap<>();
                inputData.put(EndpointDefinitionConstants.KEY_IDENTIFIER,
                    componentContext.getDynamicInputIdentifier(inputName));
                inputData.put(EndpointDefinitionConstants.KEY_NAME, inputName);
                inputData.put(UplinkToolAccessConstants.KEY_ENDPOINT_DATA_TYPE,
                    componentContext.getInputDataType(inputName));
                Map<String, String> metaData = new HashMap<>();
                for (String metaDataKey : componentContext.getInputMetaDataKeys(inputName)) {
                    metaData.put(metaDataKey, componentContext.getInputMetaDataValue(inputName, metaDataKey));
                }
                inputData.put(UplinkToolAccessConstants.KEY_ENDPOINT_META_DATA, metaData);
                dynInputsSet.add(inputData);
            }
        }

        return dynInputsSet;
    }

    private Set<String> createNotRequiredInputsSet() throws ComponentException {
        // Not required inputs are either not connected or connected and "not required"
        Set<String> nonReqInputsSet = new HashSet<String>();
        nonReqInputsSet.addAll(componentContext.getInputsNotConnected());

        for (String input : componentContext.getInputs()) {
            if (componentContext.getInputMetaDataValue(input,
                ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                EndpointDefinition.InputExecutionContraint exeConstraint = EndpointDefinition.InputExecutionContraint
                    .valueOf(componentContext.getInputMetaDataValue(input,
                        ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
                if (exeConstraint.equals(EndpointDefinition.InputExecutionContraint.NotRequired)) {
                    nonReqInputsSet.add(input);
                }
            }
        }

        return nonReqInputsSet;
    }

    private File prepareUploadDirectory(File tempRootDir, Map<String, String> inputsMap) throws IOException {
        File uploadDir;
        File inputsFile;
        uploadDir = new File(tempRootDir, "input");
        uploadDir.mkdir();
        inputsFile = new File(uploadDir, "inputs.json");

        // Read the inputs and put them in the uploadDir
        // The uploadDir contains the file inputs.json which contains the inputs as
        // typed datum
        // For files or directories the file contains references to compressed files in
        // the upload folder

        if (componentContext != null && componentContext.getInputsWithDatum() != null) {
            for (String inputName : componentContext.getInputsWithDatum()) {
                final TypedDatum input = componentContext.readInput(inputName);
                switch (input.getDataType()) {
                case DirectoryReference:
                    File inputFile = new File(uploadDir, ((DirectoryReferenceTD) input).getDirectoryReference());
                    datamanagementService.copyReferenceTDToLocalCompressedFile(componentContext, input, inputFile);
                    break;
                case FileReference:
                    File inputDir = new File(uploadDir, ((FileReferenceTD) input).getFileReference());
                    datamanagementService.copyReferenceTDToLocalCompressedFile(componentContext, input, inputDir);
                    break;
                default:
                }
                inputsMap.put(inputName, serializer.serialize(input));
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(inputsFile, inputsMap);
        return uploadDir;
    }

    private void extractAndSendOutputs(File downloadDir) throws ComponentException {

        // Get json file with outputs/references from download directory
        File outputsFile = new File(downloadDir, "outputs.json");
        if (!outputsFile.exists()) {
            throw new ComponentException("Downloaded directory does not contain file outputs.json");
        }

        // Get entries from json file and write them to component output
        try {
            @SuppressWarnings("unchecked") Map<String, List<String>> outputsMap = mapper.readValue(outputsFile, HashMap.class);
            for (Map.Entry<String, List<String>> entry : outputsMap.entrySet()) {
                String outputName = entry.getKey();
                if (!componentContext.getOutputs().contains(outputName)) {
                    throw new ComponentException("No output with name " + outputName + " defined.");
                }
                for (String output : entry.getValue()) {
                    extractAndSendSingleOutput(downloadDir, outputName, output);
                }

            }
        } catch (IOException e1) {
            throw new ComponentException("Could not parse file outputs.json: " + e1);
        }
    }

    /*
     * private void extractLog(File downloadDir) throws ComponentException {
     * 
     * File logFile = new File(downloadDir, "console.log"); if (!logFile.exists()) { LOG.warn("Did not receive log file"); } try {
     * List<String> lines = FileUtils.readLines(logFile); for (String line : lines) { componentLog.toolStdout(line); } } catch (IOException
     * e) { LOG.warn("Failed to read log file"); } }
     */

    private void extractAndSendSingleOutput(File downloadDir, String outputName, String output)
        throws ComponentException, IOException {
        TypedDatum outputTD = serializer.deserialize(output);
        if (!outputTD.getDataType().equals(componentContext.getOutputDataType(outputName))) {
            // Sanity check, should never happen
            throw new ComponentException("Data type of output " + outputName + " does not match the defined type.");
        }
        switch (outputTD.getDataType()) {
        case DirectoryReference:
            TypedDatum newDirTD = datamanagementService.createDirectoryReferenceTDFromLocalCompressedFile(
                componentContext, new File(downloadDir, ((DirectoryReferenceTD) outputTD).getDirectoryReference()),
                ((DirectoryReferenceTD) outputTD).getDirectoryName());
            componentContext.writeOutput(outputName, newDirTD);
            break;
        case FileReference:
            TypedDatum newFileTD = datamanagementService.createFileReferenceTDFromLocalCompressedFile(componentContext,
                new File(downloadDir, ((FileReferenceTD) outputTD).getFileReference()),
                ((FileReferenceTD) outputTD).getFileName());
            componentContext.writeOutput(outputName, newFileTD);
            break;
        default:
            componentContext.writeOutput(outputName, outputTD);
        }
    }

}
