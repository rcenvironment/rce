/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.sshconnection.api.SshConnectionSetup;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentDataManagementService;
import de.rcenvironment.core.component.execution.api.ComponentContext;
import de.rcenvironment.core.component.execution.api.ComponentLog;
import de.rcenvironment.core.component.model.spi.DefaultComponent;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.types.api.DirectoryReferenceTD;
import de.rcenvironment.core.datamodel.types.api.ShortTextTD;
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

    private SshConnectionService sshService;

    private SshConnectionSetup connection;

    private TempFileService tempFileService;

    private ComponentDataManagementService datamanagementService;

    private ComponentContext componentContext;

    private String toolName;

    private String toolVersion;

    private String connectionId;

    private ComponentLog componentLog;

    private boolean isWorkflow;

    @Override
    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
        componentLog = componentContext.getLog();
    }

    @Override
    public void start() throws ComponentException {
        tempFileService = TempFileServiceAccess.getInstance();
        datamanagementService = componentContext.getService(ComponentDataManagementService.class);
        sshService = componentContext.getService(SshConnectionService.class);
        toolName = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_TOOL_NAME);
        toolVersion = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_TOOL_VERSION);
        connectionId = componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_CONNECTION);
        isWorkflow = Boolean.parseBoolean(componentContext.getConfigurationValue(SshRemoteAccessConstants.KEY_IS_WORKFLOW));

        if (toolName == null || toolVersion == null || connectionId == null) {
            throw new ComponentException("Configuration for remote tool is not valid.");
        }
        connection = sshService.getConnectionSetup(connectionId);
        if (connection == null) {
            throw new ComponentException("The SSH connection for this tool does not exist.");
        }
    }

    @Override
    public void processInputs() throws ComponentException {

        // Create temp directory
        File tempRootDir;
        File inputDir;
        String inputShortText = null;

        try {
            tempRootDir = tempFileService.createManagedTempDir();

            // Read the inputs (Currently, the inputs are fixed, a short text and a directory)

            inputDir = new File(tempRootDir, "input");

            if (componentContext != null && componentContext.getInputsWithDatum() != null) {
                for (String inputName : componentContext.getInputsWithDatum()) {
                    if (inputName.equals(SshRemoteAccessConstants.INPUT_NAME_DIRECTORY)
                        && componentContext.getInputDataType(inputName).equals(DataType.DirectoryReference)) {
                        datamanagementService.copyDirectoryReferenceTDToLocalDirectory(componentContext,
                            (DirectoryReferenceTD) componentContext.readInput(inputName),
                            inputDir);
                    } else if (inputName.equals(SshRemoteAccessConstants.INPUT_NAME_SHORT_TEXT)
                        && componentContext.getInputDataType(inputName).equals(DataType.ShortText)) {
                        inputShortText = ((ShortTextTD) componentContext.readInput(inputName)).toString();
                    } else {
                        // Should never happen as the component has only static inputs.
                        LOG.warn("Invalid input " + inputName);
                    }
                }
            }
            if (inputShortText == null) {
                throw new ComponentException("Short text input is missing");
            }

        } catch (IOException e1) {
            throw new ComponentException("Temp directory for output could not be created.", e1);
        }
        // (Currently, the output of ssh tools is always a directory).
        File outputDir = new File(tempRootDir, "output");

        // Get SSH session
        Session session;
        session = sshService.getAvtiveSshSession(connectionId);

        // Initialize scp context
        JSchRCECommandLineExecutor rceExecutor = new JSchRCECommandLineExecutor(session);
        String sessionToken;

        try {
            rceExecutor.start("ra init --compact");
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {
                rceExecutor.waitForTermination();
                sessionToken = IOUtils.toString(stdoutStream).trim();
                LOG.info("Received session token " + sessionToken);

                // Currently, nothing is written to stderr by the server side. Just in case, log error messages here.
                String errStream = IOUtils.toString(stderrStream);
                if (!errStream.isEmpty()) {
                    LOG.error(errStream);
                }
            }
        } catch (IOException | InterruptedException e1) {
            throw new ComponentException("Executing SSH command failed", e1);
        }

        // Upload input directory
        try {
            JschFileTransfer.uploadDirectoryToRCEInstance(session, inputDir, StringUtils.format("/ra/%s/input", sessionToken));
        } catch (IOException | JSchException | InterruptedException e2) {
            throw new ComponentException("Uploading input directory via SCP failed", e2);
        }

        // Format Strings for SSH command, set them in Quotes and replace inner quotes by double quotes.
        String formattedToolName = QUOT + toolName.replace(QUOT, QUOT + QUOT) + QUOT;
        String formattedVersion = QUOT + toolVersion.replace(QUOT, QUOT + QUOT) + QUOT;

        String command;
        if (isWorkflow) {
            command = StringUtils.format("ra run-wf %s --show-output %s %s %s", sessionToken, formattedToolName, formattedVersion,
                inputShortText, inputDir.getName(), outputDir.getName());
        } else {
            command = StringUtils.format("ra run-tool %s --show-output %s %s %s", sessionToken, formattedToolName, formattedVersion,
                inputShortText, inputDir.getName(), outputDir.getName());
        }

        // Parse final state of component
        String state = "";

        // Run the tool
        try {
            rceExecutor.start(command);
            try (InputStream stdoutStream = rceExecutor.getStdout(); InputStream stderrStream = rceExecutor.getStderr();) {

                LineIterator it = IOUtils.lineIterator(stdoutStream, (String) null);
                while (it.hasNext()) {
                    String line = it.nextLine();
                    if (line.startsWith(StringUtils.format("[%s] StdOut: ", sessionToken))) {
                        componentLog.toolStdout(line.substring(line.indexOf(COLON) + 2));
                    } else if (line.startsWith(StringUtils.format("[%s] State: ", sessionToken))) {
                        componentLog.toolStdout(line.substring(line.indexOf("]") + 2));
                        // Parse state from line
                        state = line.substring(line.indexOf(COLON) + 2);
                    } else if (line.startsWith(StringUtils.format("[%s] StdErr: ", sessionToken))) {
                        componentLog.toolStderr(line.substring(line.indexOf(COLON) + 2));
                    } else {
                        LOG.error(line);
                    }
                }

                rceExecutor.waitForTermination();

                // Currently, nothing is written to stderr by the server side. Just in case, log error messages here.
                String errStream = IOUtils.toString(stderrStream);
                if (!errStream.isEmpty()) {
                    LOG.error(errStream);
                }
            }
        } catch (IOException | InterruptedException e1) {
            throw new ComponentException("Executing SSH command failed", e1);
        }

        // Check if final state was "FINISHED"
        if (!state.equals(FinalWorkflowState.FINISHED.toString())) {
            throw new ComponentException("Remote component execution failed.");
        }

        // Download output directory
        try {
            JschFileTransfer.downloadDirectory(session, StringUtils.format("/ra/%s/output", sessionToken),
                outputDir.getParentFile());
        } catch (IOException | JSchException e1) {
            throw new ComponentException("Downloading outputput directory via SCP failed", e1);
        }

        // Write the component output
        TypedDatum output;
        try {
            output = datamanagementService.createDirectoryReferenceTDFromLocalDirectory(componentContext, outputDir, outputDir.getName());
            componentContext.writeOutput(SshRemoteAccessConstants.OUTPUT_NAME, output);
        } catch (IOException e) {
            throw new ComponentException("Output directory reference could not be created. ", e);
        } finally {
            try {
                tempFileService.disposeManagedTempDirOrFile(tempRootDir);
            } catch (IOException e) {
                LOG.warn("Could not dispose managed temp dir " + tempRootDir);
            }
        }

    }

}
