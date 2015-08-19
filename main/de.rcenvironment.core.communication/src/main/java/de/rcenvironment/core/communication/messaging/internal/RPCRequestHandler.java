/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

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

    private static final long SLOW_SERVICE_CALL_LOGGING_THRESHOLD_NANOS = 2 * 1000000000L;

    private static final float NANOS_PER_SECOND = 1000000000f;

    // the service to dispatch remote service calls to
    private final ServiceCallHandler serviceCallHandler;

    public RPCRequestHandler(ServiceCallHandler serviceCallHandler) {
        if (serviceCallHandler == null) {
            throw new NullPointerException("Service call handler cannot be null");
        }
        this.serviceCallHandler = serviceCallHandler;
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, NodeIdentifier lastHopNodeId) throws SerializationException,
        CommunicationException {
        return handleInternal(request);
    }

    private NetworkResponse handleInternal(NetworkRequest request) throws SerializationException, CommunicationException {
        ServiceCallRequest serviceCallRequest = (ServiceCallRequest) request.getDeserializedContent();
        long startTime = System.nanoTime();
        ServiceCallResult scResult = serviceCallHandler.handle(serviceCallRequest);
        long duration = System.nanoTime() - startTime;
        if (duration > SLOW_SERVICE_CALL_LOGGING_THRESHOLD_NANOS) {
            LogFactory.getLog(getClass()).debug(
                "Slow RPC dispatch (" + (duration / NANOS_PER_SECOND) + " sec): " + serviceCallRequest.getService() + "#"
                    + serviceCallRequest.getServiceMethod() + "; caller=" + serviceCallRequest.getCallingPlatform() + ", target="
                    + serviceCallRequest.getRequestedPlatform());
        }
        // note: RPCs that threw an exception are still considered successful on the network level
        return NetworkResponseFactory.generateSuccessResponse(request, scResult);
    }

}
