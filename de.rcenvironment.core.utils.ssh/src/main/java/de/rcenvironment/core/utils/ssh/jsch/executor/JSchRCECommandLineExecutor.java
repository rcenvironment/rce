/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.utils.ssh.jsch.executor;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.executor.RCECommandLineExecutor;

/**
 * A {@link CommandLineExecutor} that delegates execution over an established JSch connection to an RCE instance. The
 * current implementation expects the remote system to provide an RCE command console.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public class JSchRCECommandLineExecutor implements RCECommandLineExecutor {

    private static final String EXCEPTION_MESSAGE_NOT_RUNNING = "Not running";

    private static final String EXCEPTION_MESSAGE_ALREADY_RUNNING = "Already running";
    
    private static final int TERMINATION_POLLING_INTERVAL_MSEC = 1000;

    private Session jschSession;

    private ChannelExec executionChannel;

    private Log log = LogFactory.getLog(getClass());
    
    private InputStream stdoutStream;
    
    private InputStream stderrStream;

    /**
     * @param jschSession an established JSch session
     */
    public JSchRCECommandLineExecutor(Session jschSession) {
        this.jschSession = jschSession;
    }

    @Override
    public InputStream getStderr() throws IOException {
        if (executionChannel == null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_NOT_RUNNING);
        }
        return stderrStream;
    }

    @Override
    public InputStream getStdout() throws IOException {
        if (executionChannel == null) {
            throw new IllegalStateException(EXCEPTION_MESSAGE_NOT_RUNNING);
        }
        return stdoutStream;
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
        command.append(commandString);

        try {
            executionChannel = (ChannelExec) jschSession.openChannel("exec");
            String fullCommand = command.toString();
            log.debug("Full invocation command: " + fullCommand);
            executionChannel.setCommand(fullCommand);
            if (stdinStream != null) {
                executionChannel.setInputStream(stdinStream);
            }
            stdoutStream = executionChannel.getInputStream();
            stderrStream = executionChannel.getExtInputStream();
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
}
