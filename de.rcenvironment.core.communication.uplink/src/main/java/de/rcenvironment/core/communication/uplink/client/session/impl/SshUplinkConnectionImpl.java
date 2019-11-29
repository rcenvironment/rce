/*
 * Copyright 2019 DLR, Germany
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
 * @author Robert Mischke
 */
public class SshUplinkConnectionImpl implements UplinkConnection {

    private final Session sshSession;

    private ChannelExec executionChannel; // non-null when this connection is open/active

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
    public synchronized OutputStream open(Consumer<InputStream> incomingStreamConsumer, Consumer<String> errorConsumer)
        throws IOException {
        if (executionChannel != null) {
            throw new IllegalStateException("Cannot be started while already running");
        }
        try {
            executionChannel = (ChannelExec) sshSession.openChannel("exec");
            final OutputStream outputStreamForInput = executionChannel.getOutputStream();
            executionChannel.setCommand(SshUplinkConnectionConstants.VIRTUAL_CONSOLE_COMMAND);
            final InputStream incomingStream = executionChannel.getInputStream();
            final InputStream errorStream = executionChannel.getErrStream();
            // TODO also monitor extInputStream?
            ConcurrencyUtils.getAsyncTaskService().execute("SSH Uplink: forward incoming data stream", () -> {
                incomingStreamConsumer.accept(incomingStream);
            });
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
            return outputStreamForInput;
        } catch (JSchException e) {
            // FIXME ensure that everything is actually closed before resetting this
            executionChannel = null;
            throw new IOException(e);
        }

    }

    @Override
    public synchronized void close() {
        if (executionChannel == null) {
            return;
        }
        executionChannel.disconnect();
    }
}
