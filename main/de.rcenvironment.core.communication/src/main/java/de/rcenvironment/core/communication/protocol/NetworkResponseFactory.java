/*
 * Copyright (C) 2006-2016 DLR, Germany
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
import de.rcenvironment.core.communication.protocol.ProtocolConstants.ResultCode;
import de.rcenvironment.core.communication.utils.MessageUtils;
import de.rcenvironment.core.utils.common.StringUtils;

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
        return new NetworkResponseImpl(responseBody, request.getRequestId(), ProtocolConstants.ResultCode.SUCCESS);
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
        return new NetworkResponseImpl(contentBytes, request.getRequestId(), ProtocolConstants.ResultCode.SUCCESS);
    }

    /**
     * Creates a response (typically on the caller side) from a received result code, and raw response bytes.
     * 
     * @param request the associated request
     * @param responseBody the byte array to use as the response payload
     * @param resultCode the numeric result code
     * @return the generated response
     */
    public static NetworkResponse generateResponseWithResultCode(NetworkRequest request, byte[] responseBody, int resultCode) {
        ResultCode resultCodeEnum = ResultCode.fromCode(resultCode); // may return INVALID RESULT_CODE if the code is unknown
        return new NetworkResponseImpl(responseBody, request.getRequestId(), resultCodeEnum);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that a node did not find a valid route to forward the received request to.
     * 
     * @param request the request
     * @param localNodeId the id of the node where the failure occurred
     * @return the generated response
     */
    public static NetworkResponse generateResponseForNoRouteWhileForwarding(NetworkRequest request, NodeIdentifier localNodeId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_WHILE_FORWARDING, localNodeId, null);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that was no valid route to the destination at the initial sender.
     * 
     * @param request the request
     * @param localNodeId the id of the node where the failure occurred
     * @return the generated response
     */
    public static NetworkResponse generateResponseForNoRouteAtSender(NetworkRequest request, NodeIdentifier localNodeId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER, localNodeId, null);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that an exception has occurred at the final destination of the request.
     * 
     * @param request the request
     * @param errorId the exception
     * @return the generated response
     */
    public static NetworkResponse generateResponseForInternalErrorAtRecipient(NetworkRequest request, String errorId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.EXCEPTION_AT_DESTINATION, request.accessMetaData()
            .getFinalRecipient(), errorId);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that an exception has occured while forwarding/routing the request to its final
     * destination.
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the exception occured
     * @param errorId an (optional) error id
     * @return the generated response
     */
    public static NetworkResponse generateResponseForErrorDuringDelivery(NetworkRequest request, NodeIdentifier eventNodeId,
        String errorId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.EXCEPTION_DURING_DELIVERY, eventNodeId, errorId);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that message forwarding/routing failed because one of the channels along the route has
     * been marked as broken, but was not removed from the routing topology yet when the channel was selected (e.g. because of asynchronous
     * queueing).
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the exception occured
     * @param errorId an (optional) error id
     * @return the generated response
     */
    public static NetworkResponse generateResponseForCloseOrBrokenChannelDuringRequestDelivery(NetworkRequest request,
        NodeIdentifier eventNodeId, String errorId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.CHANNEL_CLOSED_OR_BROKEN_BEFORE_SENDING_REQUEST, eventNodeId,
            errorId);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that the request was successfully sent or forwarded, but that the local message
     * channel was closed while waiting for the reponse.
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the exception occured
     * @param errorId an (optional) error id
     * @return the generated response
     */
    public static NetworkResponse generateResponseForChannelCloseWhileWaitingForResponse(NetworkRequest request,
        NodeIdentifier eventNodeId, String errorId) {
        return generateErrorResponse(request,
            ProtocolConstants.ResultCode.CHANNEL_OR_RESPONSE_LISTENER_SHUT_DOWN_WHILE_WAITING_FOR_RESPONSE, eventNodeId, errorId);
    }

    /**
     * Generates a {@link NetworkResponse} indicating that a timeout occurred while waiting for the response after sending the request to
     * the next hop.
     * 
     * @param request the request
     * @param eventNodeId the id of the node where the timeout occurred
     * @return the generated response
     */
    public static NetworkResponse generateResponseForTimeoutWaitingForResponse(NetworkRequest request, NodeIdentifier eventNodeId) {
        return generateErrorResponse(request, ProtocolConstants.ResultCode.TIMEOUT_WAITING_FOR_RESPONSE, eventNodeId, null);
    }

    private static NetworkResponse generateErrorResponse(NetworkRequest request, ResultCode resultCode, NodeIdentifier reporterNodeId,
        String errorId) {
        errorId = StringUtils.nullSafe(errorId);
        String nodeIdString = StringUtils.nullSafe(reporterNodeId.getIdString());
        // wrap into pre-defined format string
        String errorInfoPayload = StringUtils.escapeAndConcat(errorId, nodeIdString);
        // generate response
        return new NetworkResponseImpl(MessageUtils.serializeSafeObject(errorInfoPayload), request.getRequestId(), resultCode);
    }

    private static String representErrorLocationAsString(NodeIdentifier localNodeId) {
        // note: there is a minimal information leak here by revealing what name the intermediate node assigns to itself; it's
        // very unlikely that this a secret, yet the sender is allowed to route across this node, though - misc_ro
        return localNodeId.toString();
    }

}
