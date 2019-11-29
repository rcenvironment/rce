/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing;

import java.util.List;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * A service for performing request/response calls to remote nodes, routing across intermediate nodes if necessary.
 * 
 * @author Robert Mischke
 */
public interface MessageRoutingService {

    /**
     * Performs a {@link NetworkRequest} to a remote node and returns a {@link NetworkResponse}, using the default timeout. The destination
     * node does not have to be adjacent within the network; messages are routed across other nodes if necessary.
     * 
     * @param payload the serialized message body to transport to the destination
     * @param messageType the message type id; see {@link ProtocolConstants}
     * @param receiver the final recipient's node id.
     * @return the {@link NetworkResponse}
     */
    NetworkResponse performRoutedRequest(byte[] payload, String messageType, InstanceNodeSessionId receiver);

    /**
     * Performs a {@link NetworkRequest} to a remote node and returns a {@link NetworkResponse}, using a custom timeout. The destination
     * node does not have to be adjacent within the network; messages are routed across other nodes if necessary.
     * 
     * @param payload the serialized message body to transport to the destination
     * @param messageType the message type id; see {@link ProtocolConstants}
     * @param receiver the final recipient's node id.
     * @param timeoutMsec the timeout in msec
     * @return the {@link NetworkResponse}
     */
    NetworkResponse performRoutedRequest(byte[] payload, String messageType, InstanceNodeSessionId receiver, int timeoutMsec);

    /**
     * Sends the given request towards the destination node contained in its metadata. The request is not modified anymore; this method only
     * determines the proper {@link MessageChannel} to use and performs the request.
     * 
     * @param forwardingRequest the ready-to-send forwarding request
     * @return the {@link NetworkResponse} resulting from the request
     */
    NetworkResponse forwardAndAwait(NetworkRequest forwardingRequest);

    /**
     * Determines the shortest/cheapest route to the given node, or null if the target node is unreachable.
     * 
     * @param destination the node to find a route to
     * @return the generated route, or null if no route could be constructed
     */
    List<? extends NetworkGraphLink> getRouteTo(InstanceNodeSessionId destination);

}
