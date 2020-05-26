/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.transport.spi;

import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.channel.ServerContactPoint;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.InitialNodeInformation;

/**
 * Interface for pluggable transport providers.
 * 
 * TODO extend documentation
 * 
 * @author Robert Mischke
 */
public interface NetworkTransportProvider {

    /**
     * @return the unique transport id of this implementation; may be used to identify the OSGi bundle providing its implementation, so
     *         naming conventions should be observed
     */
    String getTransportId();

    /**
     * Connects to a remote node. This is a blocking call; all implementations must be thread-safe. After this call returns, it is expected
     * that the created {@link MessageChannel} contains the {@link InitialNodeInformation} of the remote node. This is usually achieved by
     * performing a transport-native handshake where the remote node delegates to an implementation of
     * {@link MessageChannelEndpointHandler#exchangeNodeInformation(InitialNodeInformation)}.
     * 
     * @param ncp the contact information for the remote server
     * @param ownNodeInformation the network-level information of the local node
     * @param ownProtocolVersion the protocol version of the local node
     * @param allowInverseConnection whether to allow the remote node to use the same physical link to initiate network messages to the
     *        local node
     * @param connectionEndpointHandler the {@link MessageChannelEndpointHandler} to use for remote-initiated connections from the
     *        perspective of the connection target; may be null if <code>allowInverseConnections</code> is false
     * @param brokenConnectionListener listener for unexpected connection failure
     * @return the initialized {@link MessageChannel}
     * @throws CommunicationException on connection or protocol failures
     */
    // TODO boolean parameter is redundant; remove -- misc_ro
    MessageChannel connect(NetworkContactPoint ncp, InitialNodeInformation ownNodeInformation, String ownProtocolVersion,
        boolean allowInverseConnection, MessageChannelEndpointHandler connectionEndpointHandler,
        BrokenMessageChannelListener brokenConnectionListener)
        throws CommunicationException;

    /**
     * @return whether this transport supports remote-initiated connections, i.e. logical message links that can initiate messages over a
     *         physical connection that was initiated by the remote node
     */
    boolean supportsRemoteInitiatedConnections();

    /**
     * Starts a server matching this transport. This is a blocking call. See {@link MessageChannelService#startServer(NetworkContactPoint)}
     * for more information.
     * 
     * @param scp the {@link ServerContactPoint} to start and get configuration information from
     * @throws CommunicationException on startup errors
     */
    void startServer(ServerContactPoint scp) throws CommunicationException;

    /**
     * Stops a server matching this transport. This is a blocking call. See {@link MessageChannelService#stopServer(NetworkContactPoint)}
     * for more information.
     * 
     * @param scp the {@link ServerContactPoint} to stop and get configuration information from
     */
    void stopServer(ServerContactPoint scp);

}
