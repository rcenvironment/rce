/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.protocol;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.impl.NetworkResponseImpl;
import de.rcenvironment.core.communication.utils.MessageUtils;

/**
 * Convenience factory for {@link NetworkResponse}s.
 * 
 * @author Robert Mischke
 */
public final class NetworkResponseFactory {

    private NetworkResponseFactory() {}

    /**
     * Creates a response using raw response bytes.
     * 
     * @param request the associated request
     * @param responseBody the byte array to send as response payload
     * @return the generated response
     */
    public static NetworkResponse generateSuccessResponse(NetworkRequest request, byte[] responseBody) {
        NetworkResponseImpl response =
            new NetworkResponseImpl(responseBody, request.getRequestId(), ProtocolConstants.ResultCode.SUCCESS);
        return response;
    }

    /**
     * Creates a response using a Serializable response object.
     * 
     * @param request the associated request
     * @param responseBody the {@link Serializable} to send as response payload
     * @return the generated response
     * @throws SerializationException on serialization failure
     */
    public static NetworkResponse generateSuccessResponse(NetworkRequest request, Serializable responseBody) throws SerializationException {
        byte[] contentBytes = MessageUtils.serializeObject(responseBody);
        NetworkResponseImpl response =
            new NetworkResponseImpl(contentBytes, request.getRequestId(), ProtocolConstants.ResultCode.SUCCESS);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that a node did not find a valid route to forward the received request to.
     * 
     * @param request the request
     * @param localNodeId the id of the node where the failure occurred
     * @return the generated response
     */
    public static NetworkResponse generateResponseForNoRouteWhileForwarding(NetworkRequest request, NodeIdentifier localNodeId) {
        // TODO actually store the local node id
        NetworkResponseImpl response =
            new NetworkResponseImpl(null, request.getRequestId(), ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_WHILE_FORWARDING);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that was no valid route to the destination at the initial sender.
     * 
     * @param request the request
     * @param localNodeId the id of the node where the failure occurred
     * @return the generated response
     */
    public static NetworkResponse generateResponseForNoRouteAtSender(NetworkRequest request, NodeIdentifier localNodeId) {
        // TODO actually store the local node id
        NetworkResponseImpl response =
            new NetworkResponseImpl(null, request.getRequestId(), ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that an exception has occurred at the final destination of the request.
     * 
     * @param request the request
     * @param cause the exception
     * @return the generated response
     */
    public static NetworkResponse generateResponseForExceptionAtDestination(NetworkRequest request, Throwable cause) {
        // note: this assumes that all locally-generated exceptions are safe for serialization
        byte[] contentBytes = MessageUtils.serializeSafeObject(cause);
        NetworkResponseImpl response =
            new NetworkResponseImpl(contentBytes, request.getRequestId(), ProtocolConstants.ResultCode.EXCEPTION_AT_DESTINATION);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that an exception has occured while forwarding/routing the request to its final
     * destination.
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the exception occured
     * @param cause the exception
     * @return the generated response
     */
    public static NetworkResponse generateResponseForExceptionWhileRouting(NetworkRequest request, String eventNodeId, Throwable cause) {
        // note: this assumes that all locally-generated exceptions are safe for serialization
        byte[] contentBytes = MessageUtils.serializeSafeObject(cause);
        // TODO actually set event node id
        NetworkResponseImpl response =
            new NetworkResponseImpl(contentBytes, request.getRequestId(), ProtocolConstants.ResultCode.EXCEPTION_WHILE_FORWARDING);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that a timeout occurred while waiting for the response after sending the request to
     * the next hop.
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the exception occured
     * @param cause the exception
     * @return the generated response
     */
    public static NetworkResponse generateResponseForTimeoutWaitingForResponse(NetworkRequest request,
        String eventNodeId, Throwable cause) {
        // note: this assumes that all locally-generated exceptions are safe for serialization
        byte[] contentBytes = MessageUtils.serializeSafeObject(cause);
        // TODO actually set the event node id
        NetworkResponseImpl response =
            new NetworkResponseImpl(contentBytes, request.getRequestId(), ProtocolConstants.ResultCode.TIMEOUT_WAITING_FOR_RESPONSE);
        return response;
    }

    /**
     * Generates a {@link NetworkResponse} indicating that a channel could not be used for sending as it has been closed or merked as
     * broken.
     * 
     * @param request the request
     * @return the generated response
     */
    public static NetworkResponse generateResponseForChannelClosedOrBroken(NetworkRequest request) {
        NetworkResponseImpl response = new NetworkResponseImpl(null, request.getRequestId(), ProtocolConstants.ResultCode.CHANNEL_CLOSED);
        return response;
    }

}
