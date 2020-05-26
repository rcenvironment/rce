/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Consumer;

import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkEndpointService;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSession;
import de.rcenvironment.core.communication.uplink.relay.api.ServerSideUplinkSessionService;
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

    private PipedInputStream r2cPipedInputStream;

    private PipedInputStream c2rPipedInputStream;

    public LocalServiceUplinkConnectionImpl(AsyncTaskService asyncTaskService, ServerSideUplinkSessionService sessionService) {
        this.asyncTaskService = asyncTaskService;
        this.sessionService = sessionService;
    }

    @Override
    public OutputStream open(Consumer<InputStream> incomingStreamConsumer, Consumer<String> errorConsumer) throws IOException {
        c2rPipedInputStream = new PipedInputStream();
        final PipedOutputStream c2rPipedOutputStream = new PipedOutputStream(c2rPipedInputStream);
        r2cPipedInputStream = new PipedInputStream();
        final PipedOutputStream r2cPipedOutputStream = new PipedOutputStream(r2cPipedInputStream);
        final ServerSideUplinkSession serverSideSession =
            sessionService.createServerSideSession("Local service uplink connection", "test", c2rPipedInputStream, r2cPipedOutputStream);
        asyncTaskService.execute("Local service uplink connection: Running the server-side session", serverSideSession::runSession);
        asyncTaskService.execute("Local service uplink connection: Providing the input stream",
            () -> incomingStreamConsumer.accept(r2cPipedInputStream));
        return c2rPipedOutputStream;
    }

    @Override
    public void close() {
        try {
            c2rPipedInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close the local service uplink connection's client-to-relay output stream");
        }
        try {
            r2cPipedInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close the local service uplink connection's relay-to-client output stream");
        }
    }

}
