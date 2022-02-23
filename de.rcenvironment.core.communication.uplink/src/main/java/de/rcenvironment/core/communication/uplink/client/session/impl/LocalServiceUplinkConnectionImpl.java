/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
import de.rcenvironment.core.utils.common.testutils.ThroughputLimiter;
import de.rcenvironment.core.utils.common.testutils.ThroughputLimitingInputStream;
import de.rcenvironment.core.utils.common.testutils.ThroughputLimitingOutputStream;
import de.rcenvironment.core.utils.testing.LocalTCPTestConnection;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * An {@link UplinkConnection} implementation that provides a direct connection to a local {@link ServerSideUplinkEndpointService}. This is
 * useful for integration tests to avoid setting up an actual network connection (e.g. for protocol flow tests), and for future use when an
 * instance should connect to a relay server it is running itself without setting up an unnecessary localhost connection.
 *
 * @author Robert Mischke
 */
public class LocalServiceUplinkConnectionImpl implements UplinkConnection {

    private final AsyncTaskService asyncTaskService;

    private final ServerSideUplinkSessionService sessionService;

    private LocalTCPTestConnection localTestConnection;

    private InputStream clientSideInputStream;

    private OutputStream serverSideOutputStream;

    private InputStream serverSideInputStream;

    private OutputStream clientSideOutputStream;

    private final AtomicInteger simulatedConnectionCounter = new AtomicInteger();

    private ThroughputLimiter outgoingThroughputLimiter;

    private ThroughputLimiter incomingThroughputLimiter;

    public LocalServiceUplinkConnectionImpl(AsyncTaskService asyncTaskService, ServerSideUplinkSessionService sessionService,
        ThroughputLimiter outgoingThroughputLimiter, ThroughputLimiter incomingThroughputLimiter) {
        this.asyncTaskService = asyncTaskService;
        this.sessionService = sessionService;
        this.outgoingThroughputLimiter = outgoingThroughputLimiter;
        this.incomingThroughputLimiter = incomingThroughputLimiter;
    }

    @Override
    public void open(Consumer<String> errorConsumer) throws IOException {

        localTestConnection = new LocalTCPTestConnection();

        clientSideInputStream = localTestConnection.getClientSideInputStream(); // *Client* In->Out Server/Relay
        clientSideOutputStream = localTestConnection.getClientSideOutputStream(); // *Client* Out<-In Server/Relay

        serverSideOutputStream = localTestConnection.getServerSideOutputStream(); // Client In->Out *Server/Relay*
        serverSideInputStream = localTestConnection.getServerSideInputStream(); // Client Out<-In *Server/Relay*

        // if throughput limiters were registered, this is the time to wrap them around the data streams
        if (outgoingThroughputLimiter != null) {
            clientSideOutputStream = new ThroughputLimitingOutputStream(clientSideOutputStream, outgoingThroughputLimiter);
        }
        if (incomingThroughputLimiter != null) {
            clientSideInputStream = new ThroughputLimitingInputStream(clientSideInputStream, incomingThroughputLimiter);
        }

        String testConnectionId = Integer.toString(simulatedConnectionCounter.incrementAndGet());
        String loginAccountName = "test"; // TODO add connection id? requires test adaptations, though
        String sessionContextInfoString = "local test connection #" + testConnectionId;

        // create the server-side session using the given service
        final ServerSideUplinkSession serverSideSession =
            sessionService.createServerSideSession(localTestConnection.getServerSideEndpoint(), loginAccountName,
                sessionContextInfoString);

        asyncTaskService.execute("Local service uplink connection: Running the server-side session", serverSideSession::runSession);
    }

    @Override
    public InputStream getInputStream() {
        return clientSideInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return clientSideOutputStream;
    }

    @Override
    public void close() {
        localTestConnection.getClientSideEndpoint().close();
    }

    public void simulateClientSideEOF() {
        try {
            clientSideInputStream.close();
        } catch (IOException e) {
            LogFactory.getLog(getClass()).debug("Caught an exception while closing the C->R stream to simulate EOF: " + e.toString());
        }
    }

}
