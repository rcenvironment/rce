/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.channel;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.configuration.ConnectionFilter;
import de.rcenvironment.core.communication.messaging.RawMessageChannelEndpointHandler;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Extension of a {@link NetworkContactPoint} to represent transport-specific implementations that accept incoming connections. For example,
 * a TCP-based {@link ServerContactPoint} might listen on a TCP port and accept inbound connections.
 * 
 * @author Robert Mischke
 */
public class ServerContactPoint {

    private NetworkContactPoint networkContactPoint;

    private RawMessageChannelEndpointHandler endpointHandler;

    private NetworkTransportProvider transportProvider;

    private final ConnectionFilter connectionFilter;

    private volatile boolean acceptingMessages = false;

    private volatile boolean simulatingBreakdown;

    public ServerContactPoint(NetworkTransportProvider transportProvider, NetworkContactPoint ncp,
        RawMessageChannelEndpointHandler endpointHandler, ConnectionFilter connectionFilter) {
        this.transportProvider = transportProvider;
        this.networkContactPoint = ncp;
        this.endpointHandler = endpointHandler;
        this.connectionFilter = connectionFilter;
    }

    public NetworkContactPoint getNetworkContactPoint() {
        return networkContactPoint;
    }

    public void setNetworkContactPoint(NetworkContactPoint contactPoint) {
        this.networkContactPoint = contactPoint;
    }

    public RawMessageChannelEndpointHandler getEndpointHandler() {
        return endpointHandler;
    }

    public void setEndpointHandler(RawMessageChannelEndpointHandler endpointHandler) {
        this.endpointHandler = endpointHandler;
    }

    public ConnectionFilter getConnectionFilter() {
        return connectionFilter;
    }

    // TODO review: currently, only used by integration test
    public boolean isAcceptingMessages() {
        return acceptingMessages;
    }

    @Override
    public String toString() {
        return StringUtils.format("SCP (NCP='%s', acc=%s, simbr=%s)", networkContactPoint, acceptingMessages, simulatingBreakdown);
    }

    public String getTransportId() {
        return getNetworkContactPoint().getTransportId();
    }

    /**
     * Starts accepting connections at the configured {@link NetworkContactPoint}.
     * 
     * Note that this method could be refactored away, but is kept for API symmetry with shutDown().
     * 
     * @throws CommunicationException on startup failure
     */
    public void start() throws CommunicationException {
        transportProvider.startServer(this);
        acceptingMessages = true;
    }

    /**
     * Stops accepting connections at the configured {@link NetworkContactPoint}. Whether inbound connections are actively closed is
     * transport-specific.
     */
    public void shutDown() {
        acceptingMessages = false;
        transportProvider.stopServer(this);
    }

    public boolean isSimulatingBreakdown() {
        return simulatingBreakdown;
    }

    public void setSimulatingBreakdown(boolean simulatingBreakdown) {
        this.simulatingBreakdown = simulatingBreakdown;
    }

}
