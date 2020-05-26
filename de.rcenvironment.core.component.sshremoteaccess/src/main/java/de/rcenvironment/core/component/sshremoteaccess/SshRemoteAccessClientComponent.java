/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.FileReferenceTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.JschFileTransfer;
import de.rcenvironment.core.utils.ssh.jsch.executor.JSchRCECommandLineExecutor;

/**
 * Component which runs a remote tool over a SSH connection.
 *
 * @author Brigitte Boden
 */
public class SshRemoteAccessClientComponent extends DefaultComponent {

    private static final String COLON = ":";

    private static final Log LOG = LogFactory.getLog(SshRemoteAccessClientComponent.class);

    private static final String QUOT = "\"";

    private ObjectMapper mapper;

    private SshConnectionService sshService;

    private TempFileService tempFileService;

    private ComponentDataManagementService datamanagementService;

    private ComponentContext componentContext;

    private String toolName;

    private String toolVersion;

    private String hostId;

    private String connectionId;

    private ComponentLog componentLog;

    private boolean isWorkflow;

    private TypedDatumSerializer serializer;

    private String currentSessionToken;

    private boolean componentCancelled;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        mapper = new ObjectMapper();
        tempFileService = TempFileServiceAccess.getInstance();
        datamanagementService = componentContext.getService(ComponentDataManagementService.class);
        sshService = componentContext.getService(SshConnectionService.class);
        serializer = componentContext.getService(TypedDatumService.class).getSerializer();
        toolName = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_TOOL_NAME);
        hostId = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_HOST_ID);
        toolVersion = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_TOOL_VERSION);
        connectionId = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_CONNECTION);
        isWorkflow = Boolean.parseBoolean(componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_IS_WORKFLOW));
        componentCancelled = false;

        if (toolName == null || toolVersion == null || connectionId == null) {
            throw new ComponentException("Configuration for remote tool is not valid.");
        }
        SshConnectionSetup connection = sshService.getConnectionSetup(connectionId);
        if (connection == null) {
            throw new ComponentException("The SSH connection for this tool does not exist.");
        }
        componentLog.componentInfo("Started on logical node " + componentContext.getServiceCallContext().getReceivingNode());

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

        // Get SSH session
        Session session;
        session = sshService.getAvtiveSshSession(connectionId);

        // Initialize scp context
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
        initializeRemoteExecutionContext(rceExecutor);

        // Directory containing all of the outputs of the tool
        File downloadDir = new File(tempRootDir, "output");

        // Upload input directory
        try {
            JschFileTransfer.uploadDirectoryToRCEInstance(session, uploadDir, StringUtils.format("/ra/%s/input", currentSessionToken));
        } catch (IOException | JSchException | InterruptedException e2) {
            throw new ComponentException("Uploading input directory via SCP failed", e2);
        }

        String command = prepareCommandStringForRemoteExecution(uploadDir, inputsMap, currentSessionToken, downloadDir);

        // Parse final state of component
        String state = "";

        // Run the tool
        try {
            rceExecutor.start(command);
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {

                state = waitForRemoteToolExecutionAndLogOutput(rceExecutor, currentSessionToken, stdoutStream, stderrStream);
            }
        } catch (IOException | InterruptedException e1) {
            throw new ComponentException("Executing SSH command failed", e1);
        }

        // Check if final state was "FINISHED" or "CANCELLED" if the component itself was cancelled.
        if (!(state.equals(FinalWorkflowState.FINISHED.toString())
            || (state.equals(FinalWorkflowState.CANCELLED.toString()) && componentCancelled))) {
            throw new ComponentException("Remote component execution failed.");
        }

        extractAndSendOutputs(session, currentSessionToken, downloadDir);

        try {
            rceExecutor.start(StringUtils.format("ra dispose %s", currentSessionToken));
        } catch (IOException e1) {
            throw new ComponentException("Disposing SCP context failed.", e1);
        }
    }

    @Override
    public synchronized void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
        Session session;
        session = sshService.getAvtiveSshSession(connectionId);
        componentCancelled = true;

        // Initialize scp context
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);

        try {
            rceExecutor.start(StringUtils.format("ra cancel %s", currentSessionToken));
        } catch (IOException e) {
            LOG.error("Cancelling remote tool failed", e);
            componentLog.componentError("Cancelling remote tool failed");
        }
    }

    private String waitForRemoteToolExecutionAndLogOutput(JSchRCECommandLineExecutor rceExecutor, String sessionToken,
        InputStream stdoutStream, InputStream stderrStream) throws IOException, ComponentException, InterruptedException {
        LineIterator it = IOUtils.lineIterator(stdoutStream, (String) null);
        String state = "";
        while (it.hasNext()) {
            String line = it.nextLine();
            if (line.equals("")) {
                throw new ComponentException("Could not execute the remote tool or workflow. Reason: "
                    + it.nextLine());
            } else {
                String newState = parseLogLine(sessionToken, line);
                if (!newState.equals("")) {
                    state = newState;
                }
            }
        }

        rceExecutor.waitForTermination();

        // Currently, nothing is written to stderr by the server side. Just in case, log error messages here.
        String errStream = IOUtils.toString(stderrStream);
        if (!errStream.isEmpty()) {
            LOG.error(errStream);
        }
        return state;
    }

    private String prepareCommandStringForRemoteExecution(File uploadDir, Map<String, String> inputsMap, String sessionToken,
        File downloadDir) throws ComponentException {
        // Format Strings for SSH command, set them in Quotes and replace inner quotes by double quotes.
        String formattedToolName = QUOT + toolName.replace(QUOT, QUOT + QUOT) + QUOT;
        String formattedVersion = QUOT + toolVersion.replace(QUOT, QUOT + QUOT) + QUOT;

        String command;
        if (isWorkflow) {
            command = StringUtils.format("ra run-wf %s --show-output %s %s", sessionToken, formattedToolName, formattedVersion,
                uploadDir.getName(), downloadDir.getName());
        } else {
            // Check for dynamic inputs and outputs, they have to be announced to the remote component
            String dynInputs = createDynamicInputsString(inputsMap);
            String dynoutputs = createDynamicOutputsString();

            // Check for not required inputs
            String notRequiredInputs = createNotRequiredInputsString();

            command =
                StringUtils.format("ra run-tool %s --show-output -n %s %s %s %s %s %s", sessionToken, hostId, formattedToolName,
                    formattedVersion, dynInputs, dynoutputs, notRequiredInputs);
        }
        return command;
    }

    private String createDynamicOutputsString() throws ComponentException {
        Set<Map<String, Object>> dynoutputsSet = new HashSet<>();
        for (String outputName : componentContext.getOutputs()) {
            if (componentContext.isDynamicOutput(outputName)) {
                Map<String, Object> outputData = new HashMap<>();
                outputData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, componentContext.getDynamicOutputIdentifier(outputName));
                outputData.put(EndpointDefinitionConstants.KEY_NAME, outputName);
                outputData.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPE, componentContext.getOutputDataType(outputName));
                Map<String, String> metaData = new HashMap<>();
                for (String metaDataKey : componentContext.getOutputMetaDataKeys(outputName)) {
                    metaData.put(metaDataKey, componentContext.getOutputMetaDataValue(outputName, metaDataKey));
                }
                outputData.put(SshRemoteAccessConstants.KEY_ENDPOINT_META_DATA, metaData);
                dynoutputsSet.add(outputData);
            }
        }
        String dynoutputs = "";
        if (!dynoutputsSet.isEmpty()) {
            try {
                dynoutputs = StringUtils.format("--dynOutputs %s", mapper.writeValueAsString(dynoutputsSet));
            } catch (IOException e) {
                throw new ComponentException("Could not transfer dynamic output descriptions to remote tool: " + e);
            }
        }
        return dynoutputs;
    }

    private String createDynamicInputsString(Map<String, String> inputsMap) throws ComponentException {
        Set<Map<String, Object>> dynInputsSet = new HashSet<>();
        for (String inputName : inputsMap.keySet()) {
            if (componentContext.isDynamicInput(inputName)) {
                Map<String, Object> inputData = new HashMap<>();
                inputData.put(EndpointDefinitionConstants.KEY_IDENTIFIER, componentContext.getDynamicInputIdentifier(inputName));
                inputData.put(EndpointDefinitionConstants.KEY_NAME, inputName);
                inputData.put(SshRemoteAccessConstants.KEY_ENDPOINT_DATA_TYPE, componentContext.getInputDataType(inputName));
                Map<String, String> metaData = new HashMap<>();
                for (String metaDataKey : componentContext.getInputMetaDataKeys(inputName)) {
                    metaData.put(metaDataKey, componentContext.getInputMetaDataValue(inputName, metaDataKey));
                }
                inputData.put(SshRemoteAccessConstants.KEY_ENDPOINT_META_DATA, metaData);
                dynInputsSet.add(inputData);
            }
        }
        String dynInputs = "";
        if (!dynInputsSet.isEmpty()) {
            try {
                dynInputs = StringUtils.format("--dynInputs %s", mapper.writeValueAsString(dynInputsSet));
            } catch (IOException e) {
                throw new ComponentException("Could not transfer dynamic input descriptions to remote tool: " + e);
            }
        }
        return dynInputs;
    }

    private String createNotRequiredInputsString() throws ComponentException {
        // Not required inputs are either not connected or connected and "not required"
        Set<String> nonReqInputsSet = new HashSet<String>();
        nonReqInputsSet.addAll(componentContext.getInputsNotConnected());

        for (String input : componentContext.getInputs()) {
            if (componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT) != null) {
                EndpointDefinition.InputExecutionContraint exeConstraint = EndpointDefinition.InputExecutionContraint.valueOf(
                    componentContext.getInputMetaDataValue(input, ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT));
                if (exeConstraint.equals(EndpointDefinition.InputExecutionContraint.NotRequired)) {
                    nonReqInputsSet.add(input);
                }
            }
        }

        String nonReqInputs = "";
        if (!nonReqInputsSet.isEmpty()) {
            try {
                nonReqInputs = StringUtils.format("--nonReqInputs %s", mapper.writeValueAsString(nonReqInputsSet));
            } catch (IOException e) {
                throw new ComponentException("Could not transfer not connected inputs to remote tool: " + e);
            }
        }
        return nonReqInputs;
    }

    private void initializeRemoteExecutionContext(JSchRCECommandLineExecutor rceExecutor) throws ComponentException {

        try {
            rceExecutor.start("ra init --compact");
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                rceExecutor.waitForTermination();
                currentSessionToken = IOUtils.toString(stdoutStream).trim();

                if (currentSessionToken.contains("Command ra init --compact not executed.")) {
                    throw new ComponentException("Could not initiate remote tool or workflow execution. Reason: " + currentSessionToken);
                }

                LOG.info("Received session token " + currentSessionToken);

                // Currently, nothing is written to stderr by the server side. Just in case, log error messages here.
                String errStream = IOUtils.toString(stderrStream);
                if (!errStream.isEmpty()) {
                    LOG.error(errStream);
                }
            }
        } catch (IOException | InterruptedException e1) {
            throw new ComponentException("Executing SSH command failed", e1);
        }
    }

    private File prepareUploadDirectory(File tempRootDir, Map<String, String> inputsMap) throws IOException {
        File uploadDir;
        File inputsFile;
        uploadDir = new File(tempRootDir, "input");
        uploadDir.mkdir();
        inputsFile = new File(uploadDir, "inputs.json");

        // Read the inputs and put them in the uploadDir
        // The uploadDir contains the file inputs.json which contains the inputs as typed datum
        // For files or directories the file contains references to compressed files in the upload folder

        if (componentContext != null && componentContext.getInputsWithDatum() != null) {
            for (String inputName : componentContext.getInputsWithDatum()) {
                final TypedDatum input = componentContext.readInput(inputName);
                switch (input.getDataType()) {
                case DirectoryReference:
                    File inputFile = new File(uploadDir, ((DirectoryReferenceTD) input).getDirectoryReference());
                    datamanagementService.copyReferenceTDToLocalCompressedFile(componentContext, input,
                        inputFile);
                    break;
                case FileReference:
                    File inputDir = new File(uploadDir, ((FileReferenceTD) input).getFileReference());
                    datamanagementService.copyReferenceTDToLocalCompressedFile(componentContext, input,
                        inputDir);
                    break;
                default:
                }
                inputsMap.put(inputName, serializer.serialize(input));
            }
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(inputsFile, inputsMap);
        return uploadDir;
    }

    private void extractAndSendOutputs(Session session, String sessionToken, File downloadDir) throws ComponentException {
        // Download output directory
        try {
            JschFileTransfer.downloadDirectory(session, StringUtils.format("/ra/%s/output", sessionToken),
                downloadDir.getParentFile());
        } catch (IOException | JSchException e1) {
            throw new ComponentException("Downloading output directory via SCP failed", e1);
        }

        // Get json file with outputs/references from download directory
        File outputsFile = new File(downloadDir, "outputs.json");
        if (!outputsFile.exists()) {
            throw new ComponentException("Downloaded directory does not contain file outputs.json");
        }

        // Get entries from json file and write them to component output
        try {
            @SuppressWarnings("unchecked") Map<String, List<String>> outputsMap =
                mapper.readValue(outputsFile, HashMap.class);
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

    private void extractAndSendSingleOutput(File downloadDir, String outputName, String output) throws ComponentException, IOException {
        TypedDatum outputTD = serializer.deserialize(output);
        if (!outputTD.getDataType().equals(componentContext.getOutputDataType(outputName))) {
            // Sanity check, should never happen
            throw new ComponentException("Data type of output " + outputName + " does not match the defined type.");
        }
        switch (outputTD.getDataType()) {
        case DirectoryReference:
            TypedDatum newDirTD = datamanagementService.createDirectoryReferenceTDFromLocalCompressedFile(componentContext,
                new File(downloadDir, ((DirectoryReferenceTD) outputTD).getDirectoryReference()),
                ((DirectoryReferenceTD) outputTD).getDirectoryName());
            componentContext.writeOutput(outputName, newDirTD);
            break;
        case FileReference:
            TypedDatum newFileTD =
                datamanagementService.createFileReferenceTDFromLocalCompressedFile(componentContext, new File(downloadDir,
                    ((FileReferenceTD) outputTD).getFileReference()), ((FileReferenceTD) outputTD).getFileName());
            componentContext.writeOutput(outputName, newFileTD);
            break;
        default:
            componentContext.writeOutput(outputName, outputTD);
        }
    }

    private String parseLogLine(String sessionToken, String line) {
        String state = "";
        if (line.startsWith(StringUtils.format("[%s] StdOut: ", sessionToken))) {
            componentLog.toolStdout(line.substring(line.indexOf(COLON) + 2));
        } else if (line.startsWith(StringUtils.format("[%s] State: ", sessionToken))) {
            // Parse state from line
            state = line.substring(line.indexOf(COLON) + 2);
            if (isWorkflow) {
                componentLog.toolStdout("Workflow state changed, new state: " + WorkflowState.valueOf(state).getDisplayName());
            } else {
                componentLog.toolStdout("Tool state changed, new state: " + WorkflowState.valueOf(state).getDisplayName());
            }
        } else if (line.startsWith(StringUtils.format("[%s] StdErr: ", sessionToken))) {
            componentLog.toolStderr(line.substring(line.indexOf(COLON) + 2));
        } else {
            LOG.error(line);
        }
        return state;
    }
}
