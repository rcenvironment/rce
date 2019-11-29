/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.virtual;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.channel.MessageChannelIdFactory;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.InitialNodeInformation;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.BrokenMessageChannelListener;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.MessageChannelEndpointHandler;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;

/**
 * A JVM-internal pseudo transport intended for unit testing.
 * 
 * @author Robert Mischke
 */
public class VirtualNetworkTransportProvider implements NetworkTransportProvider {

    /**
     * The transport id of this provider.
     */
    public static final String TRANSPORT_ID = "virtual";

    private Map<NetworkContactPoint, ServerContactPoint> virtualServices =
        new HashMap<NetworkContactPoint, ServerContactPoint>();

    private Map<InstanceNodeSessionId, MessageChannelEndpointHandler> remoteInitiatedConnectionEndpointHandlerMap =
        new HashMap<InstanceNodeSessionId, MessageChannelEndpointHandler>();

    private boolean supportRemoteInitiatedConnections;

    private MessageChannelIdFactory connectionIdFactory;

    /**
     * Constructor.
     * 
     * @param supportRemoteInitiatedConnections whether the transport should simulate support for passive/inverse connections or not
     */
    public VirtualNetworkTransportProvider(boolean supportRemoteInitiatedConnections, MessageChannelIdFactory connectionIdFactory) {
        this.supportRemoteInitiatedConnections = supportRemoteInitiatedConnections;
        this.connectionIdFactory = connectionIdFactory;
    }

    @Override
    public String getTransportId() {
        return TRANSPORT_ID;
    }

    @Override
    public synchronized MessageChannel connect(NetworkContactPoint ncp, InitialNodeInformation initiatingNodeInformation,
        String ownProtocolVersion, boolean allowDuplex,
        MessageChannelEndpointHandler initiatingEndpointHandler, BrokenMessageChannelListener brokenConnectionListener)
        throws CommunicationException {
        // FIXME handle case of no matching server instance; causes a NPE in current implementation
        ServerContactPoint receivingSCP = virtualServices.get(ncp);
        if (receivingSCP == null) {
            throw new IllegalStateException("No matching SCP found for NCP " + ncp + "; was the server stated before connecting to it?");
        }
        if (receivingSCP.isSimulatingBreakdown()) {
            // remote server (in integration tests) is simulating a crash
            throw new CommunicationException("Failed to open connection: " + receivingSCP + " is simulating breakdown");
        }
        MessageChannelEndpointHandler receivingEndpointHandler = receivingSCP.getEndpointHandler();

        MessageChannel newChannel =
            new VirtualNetworkMessageChannel(initiatingNodeInformation, ownProtocolVersion, receivingEndpointHandler, receivingSCP);
        InitialNodeInformation receivingNodeInformation = receivingEndpointHandler.exchangeNodeInformation(initiatingNodeInformation);
        newChannel.setRemoteNodeInformation(receivingNodeInformation);
        newChannel.setChannelId(connectionIdFactory.generateId(true));

        // TODO use brokenConnectionListener

        if (allowDuplex && supportRemoteInitiatedConnections) {
            MessageChannel remoteChannel =
                new VirtualNetworkMessageChannel(receivingNodeInformation, receivingSCP.getExpectedProtocolVersion(),
                    initiatingEndpointHandler, receivingSCP);
            remoteChannel.setRemoteNodeInformation(initiatingNodeInformation);
            remoteChannel.setChannelId(connectionIdFactory.generateId(false));
            remoteChannel.setInitiatedByRemote(true);

            // cross-link "associated mirror channel" ids
            remoteChannel.setAssociatedMirrorChannelId(newChannel.getChannelId());
            newChannel.setAssociatedMirrorChannelId(remoteChannel.getChannelId());

            remoteChannel.markAsEstablished();
            receivingEndpointHandler.onRemoteInitiatedChannelEstablished(remoteChannel, receivingSCP);
        }

        newChannel.markAsEstablished();
        return newChannel;
    }

    @Override
    public boolean supportsRemoteInitiatedConnections() {
        return supportRemoteInitiatedConnections;
    }

    @Override
    public synchronized void startServer(ServerContactPoint scp) {
        // TODO naive implementation; check for collisions etc.
        virtualServices.put(scp.getNetworkContactPoint(), scp);
    }

    @Override
    public synchronized void stopServer(ServerContactPoint scp) {
        ServerContactPoint removed = virtualServices.remove(scp.getNetworkContactPoint());
        if (removed == null) {
            throw new IllegalStateException("No matching SCP registered: " + scp);
        }
    }
}
