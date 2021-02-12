/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.management;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * Top-level service that coordinates actions like starting or stopping the whole communication
 * layer.
 * 
 * @author Robert Mischke
 */
public interface CommunicationManagementService {

    /**
     * Initializes/starts the communication layer. This includes starting to listen on the
     * "provided" (ie, server) {@link NetworkContactPoint}s and connecting to the configured
     * "remote" {@link NetworkContactPoint}s.
     * 
     * @throws CommunicationException on startup failure
     */
    // TODO rename to startUpCommunication[Layer]?
    void startUpNetwork() throws CommunicationException;

    /**
     * Synchronously connects to a network peer at the given contact point.
     * 
     * @param ncp the {@link NetworkContactPoint} to connect to
     * @return the resulting {@link MessageChannel}
     * @throws CommunicationException on connection errors
     */
    @Deprecated
    MessageChannel connectToRuntimePeer(NetworkContactPoint ncp) throws CommunicationException;

    /**
     * Asynchronously connects to a network peer at the given contact point. If the connection
     * fails, the exception is logged as a warning.
     * 
     * @param ncp the {@link NetworkContactPoint} to connect to
     */
    @Deprecated
    void asyncConnectToNetworkPeer(NetworkContactPoint ncp);

    /**
     * Shuts down the communication layer. This includes closing all outgoing connections, and
     * stopping to listen on the "provided" (ie, server) {@link NetworkContactPoint}s.
     */
    // TODO rename to shutDownCommunication[Layer]?
    void shutDownNetwork();

    /**
     * Intended for use by unit/integration tests; simulates a "hard"/unclean shutdown where the
     * node does not send any network notifications before shutting down.
     */
    void simulateUncleanShutdown();

}
