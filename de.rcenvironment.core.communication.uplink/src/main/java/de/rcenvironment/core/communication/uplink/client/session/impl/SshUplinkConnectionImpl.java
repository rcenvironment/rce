/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.uplink.client.session.api.SshUplinkConnectionConstants;
import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;

/**
 * The implementation of an SSH {@link UplinkConnection}. Currently, this establishes a standard SSH connection to the embedded SSH server
 * implementation and executes a certain (virtual) remote command, and attaches provided input and output streams to it.
 * 
 * Currently, this wrapper takes ownership of the provided SSH {@link Session}, and closes it when {@link #close()} is called. If multiple
 * Uplink sessions should ever be performed over the same SSH connection (currently quite unlikely), this behavior has to be changed.
 *
 * @author Robert Mischke
 */
public class SshUplinkConnectionImpl implements UplinkConnection {

    private final Session sshSession;

    private ChannelExec executionChannel; // non-null when this connection is open/active

    private OutputStream outputStream;

    private InputStream inputStream;

    private boolean closed; // synchronized on "this"

    private final Log log = LogFactory.getLog(getClass());

    /**
     * @param sshSession an already-established JSch session
     */
    public SshUplinkConnectionImpl(Session sshSession) {
        this.sshSession = sshSession;
    }

    /**
     * Opens an uplink connection within an already-established JSch connection/session.
     * <p>
     * Parent documentation: {@inheritDoc}
     *
     * @see de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection#open(java.io.InputStream,
     *      java.util.function.Consumer)
     */
    @Override
    public synchronized void open(Consumer<String> errorConsumer)
        throws IOException {
        if (executionChannel != null) {
            throw new IllegalStateException("Cannot be started while already running");
        }
        try {
            executionChannel = (ChannelExec) sshSession.openChannel("exec");
            outputStream = executionChannel.getOutputStream();
            executionChannel.setCommand(SshUplinkConnectionConstants.VIRTUAL_CONSOLE_COMMAND);
            inputStream = executionChannel.getInputStream();
            final InputStream errorStream = executionChannel.getErrStream();
            // TODO also monitor extInputStream?
            ConcurrencyUtils.getAsyncTaskService().execute("SSH Uplink: monitor incoming error stream", () -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                String message;
                try {
                    while ((message = reader.readLine()) != null) {
                        errorConsumer.accept(message);
                    }
                } catch (IOException e) {
                    if (!(e instanceof EOFException)) {
                        errorConsumer.accept("Error stream watcher terminated unexpectedly: " + e.toString());
                    }
                }
            });
            executionChannel.connect();
        } catch (JSchException e) {
            // TODO ensure that everything is actually closed before resetting this; no problems so far, though
            executionChannel = null;
            throw new IOException(e);
        }

    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        synchronized (outputStream) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                } else {
                    log.debug("Unexpected null stream");
                }
            } catch (IOException e) {
                log.debug("Non-critical exception closing the connection output stream before shutdown: " + e);
            }
        }
        if (executionChannel != null && executionChannel.isConnected()) {
            executionChannel.disconnect();
        }
        // see class JavaDoc; this class takes control/ownership of the constructor-provided SSH session, so we close it here
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            // log.debug("SSH connection " + System.identityHashCode(sshSession) + " closed");
        }
        closed = true;
    }
}
