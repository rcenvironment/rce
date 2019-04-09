/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.utils.common.rpc.RemotableService;

/**
 * The main service for handling local aspects of a Reliable RPC Stream.
 *
 * @author Robert Mischke
 */
@RemotableService
public interface ReliableRPCStreamService {

    /**
     * After receiving the id of a rRPC session created on the remote node, this method creates the appropriate sender setup and returns a
     * handle for it.
     * 
     * @param resolvedTargetNodeId the remote node
     * @param streamId the stream id assigned by the remote node
     * @return the {@link ReliableRPCStreamHandle} that can be used as a general {@link NetworkDestination} in methods that support it
     */
    ReliableRPCStreamHandle createLocalSetupForRemoteStreamId(LogicalNodeSessionId resolvedTargetNodeId, String streamId);

    /**
     * Transparently performs a remote service call (RPC) in a reliable fashion; see the {@link ReliableRPCStreamSender} JavaDoc for
     * details.
     * 
     * @param serviceCallRequest the request to perform on a remote node, retrying the transmission if necessary
     * @return the final result of the call; if it represents a failure, then it that error was considered unrecoverable (e.g. the remote
     *         node having restarted)
     * @throws SerializationException if serializing the request body failed
     */
    ServiceCallResult performRequest(ServiceCallRequest serviceCallRequest) throws SerializationException;

    /**
     * Handles an incoming network request; see the {@link ReliableRPCStreamReceiver} JavaDoc for details.
     * 
     * @param serviceCallRequest the incoming request
     * @return the result to send back (which may be cached from an equivalent earlier request)
     * @throws InternalMessagingException on internal, unrecoverable errors
     */
    ServiceCallResult handleIncomingRequest(ServiceCallRequest serviceCallRequest) throws InternalMessagingException;

}
