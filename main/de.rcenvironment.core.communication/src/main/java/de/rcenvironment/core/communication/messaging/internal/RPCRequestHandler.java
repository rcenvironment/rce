/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.rpc.ServiceCallHandler;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;

/**
 * Handler for incoming RPC (remote procedure call) requests.
 * 
 * @author Robert Mischke
 */
public class RPCRequestHandler implements NetworkRequestHandler {

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE_FOR_CALLER =
        "An internal error occured on the remote node while processing the request; check the remote node's log files for details";

    private static final long SLOW_SERVICE_CALL_LOGGING_THRESHOLD_NANOS = 2 * 1000000000L;

    private static final float NANOS_PER_SECOND = 1000000000f;

    // the service to dispatch remote service calls to
    private final ServiceCallHandler serviceCallHandler;

    private final Log log = LogFactory.getLog(getClass());

    public RPCRequestHandler(ServiceCallHandler serviceCallHandler) {
        if (serviceCallHandler == null) {
            throw new NullPointerException("Service call handler cannot be null");
        }
        this.serviceCallHandler = serviceCallHandler;
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, NodeIdentifier lastHopNodeId) throws SerializationException,
        CommunicationException {
        ServiceCallRequest serviceCallRequest = (ServiceCallRequest) request.getDeserializedContent();
        ServiceCallResult scResult;
        try {
            scResult = handleInternal(serviceCallRequest);
            try {
                // note: RPCs that throw a declared service exception are still considered successful on the network level
                return NetworkResponseFactory.generateSuccessResponse(request, scResult);
            } catch (SerializationException e) {
                log.warn("Failed to serialize the result of a call to " + formatGenericCallInfo(serviceCallRequest), e);
                // do not propagate unknown-content stack trace information to the outside
                throw new CommunicationException(GENERIC_INTERNAL_ERROR_MESSAGE_FOR_CALLER);
            }
        } catch (RuntimeException e) {
            log.error("Caught an unhandled " + e.getClass().getSimpleName() + " while processing a call to "
                + formatGenericCallInfo(serviceCallRequest), e);
            // do not propagate unknown-content stack trace information to the outside
            throw new CommunicationException(GENERIC_INTERNAL_ERROR_MESSAGE_FOR_CALLER);
        }
    }

    private String formatGenericCallInfo(ServiceCallRequest serviceCallRequest) {
        return serviceCallRequest.getService() + "#" + serviceCallRequest.getServiceMethod() + "; caller="
            + serviceCallRequest.getCallingPlatform();
    }

    private ServiceCallResult handleInternal(ServiceCallRequest serviceCallRequest) throws CommunicationException {
        long startTime = System.nanoTime();
        ServiceCallResult scResult = serviceCallHandler.handle(serviceCallRequest);
        if (scResult == null) {
            // create synthetic exception for the stacktrace; do not actually throw it
            log.warn("ServiceCallResult result was null immediately after dispatching an RPC request to "
                + formatGenericCallInfo(serviceCallRequest) + " (no exception thrown; null result forwarded for now)",
                new RuntimeException());
        }
        long duration = System.nanoTime() - startTime;
        if (duration > SLOW_SERVICE_CALL_LOGGING_THRESHOLD_NANOS) {
            log.debug("Slow RPC dispatch (" + (duration / NANOS_PER_SECOND) + " sec): " + formatGenericCallInfo(serviceCallRequest)
                + ", target=" + serviceCallRequest.getRequestedPlatform());
        }
        return scResult;
    }

}
