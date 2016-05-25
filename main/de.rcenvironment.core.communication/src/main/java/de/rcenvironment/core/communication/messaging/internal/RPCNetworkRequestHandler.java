/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Handler for incoming RPC (remote procedure call) requests.
 * 
 * @author Robert Mischke
 */
public class RPCNetworkRequestHandler implements NetworkRequestHandler {

    private static final long SLOW_SERVICE_CALL_LOGGING_THRESHOLD_MSEC = 10000; // 10 sec

    private static final String SLOW_SERVICE_CALL_STATISTICS_CATEGORY_NAME =
        "Remote service call processing times above threshold (" + SLOW_SERVICE_CALL_LOGGING_THRESHOLD_MSEC + " msec)";

    // the service to dispatch remote service calls to
    private final RemoteServiceCallHandlerService serviceCallHandler;

    private final Log log = LogFactory.getLog(getClass());

    public RPCNetworkRequestHandler(RemoteServiceCallHandlerService serviceCallHandler) {
        if (serviceCallHandler == null) {
            throw new NullPointerException("Service call handler cannot be null");
        }
        this.serviceCallHandler = serviceCallHandler;
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, NodeIdentifier lastHopNodeId) throws InternalMessagingException {
        ServiceCallRequest serviceCallRequest = (ServiceCallRequest) NetworkRequestUtils.deserializeWithExceptionHandling(request);
        ServiceCallResult scResult;
        try {
            if (StatsCounter.isEnabled()) {
                StatsCounter.count("Remote service calls (received)",
                    StringUtils.format("%s#%s(...)", serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()));
            }

            scResult = handleInternal(serviceCallRequest);
            try {
                // note: RPCs that throw a declared service exception are still considered successful on the network level
                return NetworkResponseFactory.generateSuccessResponse(request, scResult);
            } catch (SerializationException e) {
                throw new InternalMessagingException("Failed to serialize the result of a call to "
                    + formatGenericCallInfo(serviceCallRequest), e);
            }
        } catch (RuntimeException e) {
            throw new InternalMessagingException("Caught an unhandled " + e.getClass().getSimpleName() + " while processing a call to "
                + formatGenericCallInfo(serviceCallRequest), e);
        }
    }

    private String formatGenericCallInfo(ServiceCallRequest serviceCallRequest) {
        return serviceCallRequest.getServiceName() + "#" + serviceCallRequest.getMethodName() + "; caller="
            + serviceCallRequest.getSender();
    }

    private ServiceCallResult handleInternal(ServiceCallRequest serviceCallRequest) throws InternalMessagingException {
        long startTime = System.currentTimeMillis();
        ServiceCallResult scResult = serviceCallHandler.handle(serviceCallRequest);
        if (scResult == null) {
            // create synthetic exception for the stacktrace; do not actually throw it
            log.warn("ServiceCallResult result was null immediately after dispatching an RPC request to "
                + formatGenericCallInfo(serviceCallRequest) + " (no exception thrown; null result forwarded for now)",
                new RuntimeException());
        }
        long duration = System.currentTimeMillis() - startTime;
        if (duration >= SLOW_SERVICE_CALL_LOGGING_THRESHOLD_MSEC) {
            // log
            log.debug(StringUtils.format("An incoming service call from %s to %s#%s() took %,d msec to complete",
                serviceCallRequest.getSender(), serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), duration));
            // add duration to statistics
            StatsCounter.registerValue(SLOW_SERVICE_CALL_STATISTICS_CATEGORY_NAME,
                StringUtils.format("%s#%s", serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()), duration);
        }
        return scResult;
    }

}
