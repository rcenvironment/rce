/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.executor.AbstractCommandLineExecutor;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.ssh.jsch.JschFileTransfer;

/**
 * A {@link CommandLineExecutor} that delegates execution over an established JSch connection. The
 * current implementation expects the remote system to provide a bash-like shell, and will therefore
 * not work with a standard Windows host.
 * 
 * Note that this class is not thread-safe.
 * 
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class JSchCommandLineExecutor extends AbstractCommandLineExecutor implements CommandLineExecutor {

    private static final String SLASH = "/";

    private static final int TERMINATION_POLLING_INTERVAL_MSEC = 1000;

    private static final String EXCEPTION_MESSAGE_NOT_RUNNING = "Not running";

    private static final String EXCEPTION_MESSAGE_ALREADY_RUNNING = "Already running";

    private Session jschSession;

    private String remoteWorkDir;

    private ChannelExec executionChannel;
    
    private InputStream stdOutStream;
    
    private InputStream stdErrStream;

    private Log log = LogFactory.getLog(getClass());

    /**
     * @param jschSession an established JSch session
     * @param remoteWorkDir the path of the remote work directory; this can be either absolute, or
     *        relative to the default SSH "home" directory
     */
    public JSchCommandLineExecutor(Session jschSession, String remoteWorkDir) {
        this.jschSession = jschSession;
        this.remoteWorkDir = remoteWorkDir;
    }

    @Override
    public InputStream getStderr() throws IOException {
        if (executionChannel == null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_NOT_RUNNING);
        }
        return stdErrStream;
    }

    @Override
    public InputStream getStdout() throws IOException {
        if (executionChannel == null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_NOT_RUNNING);
        }
        return stdOutStream;
    }

    @Override
    public String getWorkDirPath() {
        return remoteWorkDir;
    }

    @Override
    public void start(String commandString) throws IOException {
        start(commandString, null);
    }

    @Override
    public void start(String commandString, InputStream stdinStream) throws IOException {
        if (executionChannel != null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_ALREADY_RUNNING);
        }
        StringBuilder command = new StringBuilder();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            command.append("export ");
            command.append(entry.getKey());
            command.append("=");
            command.append(entry.getValue());
            command.append(" && ");
        }

        command.append("cd ");
        command.append(remoteWorkDir);
        command.append(" && ");
        command.append(commandString);

        try {
            executionChannel = (ChannelExec) jschSession.openChannel("exec");
            String fullCommand = command.toString();
            log.debug("Full invocation command: " + fullCommand);
            executionChannel.setCommand(fullCommand);
            if (stdinStream != null) {
                executionChannel.setInputStream(stdinStream);
            }
            // as stated in the JavaDoc of ChannelExec, 'getInputStream()' and 'getErrStream()' should be called before 'connect()'
            stdOutStream = executionChannel.getInputStream();
            stdErrStream = executionChannel.getExtInputStream();
            executionChannel.connect();
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int waitForTermination() throws IOException, InterruptedException {
        if (executionChannel == null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_NOT_RUNNING);
        }
        try {
            while (!executionChannel.isClosed()) {
                Thread.sleep(TERMINATION_POLLING_INTERVAL_MSEC);
            }
            return executionChannel.getExitStatus();
        } finally {
            // note: this is called AFTER getExitStatus during normal execution flow
            executionChannel.disconnect();
            executionChannel = null;
        }
    }
    
    @Override
    public void downloadWorkdir(File localDir) throws IOException {
        try {
            JschFileTransfer.downloadDirectory(jschSession, remoteWorkDir, localDir);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void uploadFileToWorkdir(File localFile, String remoteLocation) throws IOException {
        try {
            JschFileTransfer.uploadFile(jschSession, localFile, remoteWorkDir + SLASH + remoteLocation);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void downloadFileFromWorkdir(String remoteLocation, File localFile) throws IOException {
        try {
            JschFileTransfer.downloadFile(jschSession, remoteWorkDir + SLASH + remoteLocation, localFile);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void uploadDirectoryToWorkdir(File localDirectory, String remoteLocation) throws IOException {
        try {
            JschFileTransfer.uploadDirectory(jschSession, localDirectory, remoteWorkDir + SLASH + remoteLocation);
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void downloadDirectoryFromWorkdir(String remoteLocation, File localDirectory) throws IOException {
        try {
            JschFileTransfer.downloadDirectory(jschSession, remoteWorkDir + SLASH + remoteLocation, localDirectory);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void downloadFile(String remoteSource, File remoteTarget) throws IOException {
        try {
            JschFileTransfer.downloadFile(jschSession, remoteSource, remoteTarget);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void downloadDirectory(String remoteLocation, File localDirectory) throws IOException {
        try {
            JschFileTransfer.downloadDirectory(jschSession, remoteLocation, localDirectory);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void uploadFile(File localFile, String remoteLocation) throws IOException {
        try {
            JschFileTransfer.uploadFile(jschSession, localFile, remoteLocation);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void uploadDirectory(File localDirectory, String remoteLocation) throws IOException {
        try {
            JschFileTransfer.uploadDirectory(jschSession, localDirectory, remoteLocation);
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void remoteCopy(String remoteSource, String remoteTarget) throws IOException {
        try {
            JschFileTransfer.remoteToRemoteCopy(jschSession, remoteSource, remoteTarget);
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
