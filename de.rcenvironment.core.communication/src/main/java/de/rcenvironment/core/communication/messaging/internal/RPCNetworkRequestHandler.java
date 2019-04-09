/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.messaging.internal;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandler;
import de.rcenvironment.core.communication.model.NetworkRequest;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.protocol.NetworkResponseFactory;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.internal.ReliableRPCStreamService;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;
import de.rcenvironment.toolkit.modules.statistics.api.ValueEventCategory;

/**
 * Handler for incoming RPC (remote procedure call) requests.
 * 
 * @author Robert Mischke
 */
public class RPCNetworkRequestHandler implements NetworkRequestHandler {

    private static final long SLOW_SERVICE_CALL_LOGGING_THRESHOLD_MSEC = 10000; // 10 sec

    // the service to dispatch remote service calls to
    private final RemoteServiceCallHandlerService serviceCallHandler;

    private final CounterCategory methodCallCounter;

    private final ValueEventCategory slowMethodCallCounter;

    private final Log log = LogFactory.getLog(getClass());

    private ReliableRPCStreamService reliableRPCStreamService;

    public RPCNetworkRequestHandler(RemoteServiceCallHandlerService serviceCallHandler, ReliableRPCStreamService reliableRPCStreamService) {
        this.serviceCallHandler = Objects.requireNonNull(serviceCallHandler);
        this.reliableRPCStreamService = Objects.requireNonNull(reliableRPCStreamService);

        // not injecting this via OSGi-DS as this service is planned to move to the toolkit layer anyway - misc_ro
        final StatisticsTrackerService statisticsService =
            StaticToolkitHolder.getServiceWithUnitTestFallback(StatisticsTrackerService.class);
        methodCallCounter =
            statisticsService.getCounterCategory("Remote service calls (received): service methods",
                StatisticsFilterLevel.RELEASE);
        slowMethodCallCounter =
            statisticsService.getValueEventCategory("Remote service calls (received): slow method calls (more than "
                + SLOW_SERVICE_CALL_LOGGING_THRESHOLD_MSEC + " msec)", StatisticsFilterLevel.RELEASE);
    }

    @Override
    public NetworkResponse handleRequest(NetworkRequest request, InstanceNodeSessionId lastHopNodeId) throws InternalMessagingException {
        ServiceCallRequest serviceCallRequest = (ServiceCallRequest) NetworkRequestUtils.deserializeWithExceptionHandling(request);
        ServiceCallResult scResult;
        try {
            if (methodCallCounter.isEnabled()) {
                methodCallCounter.count(StringUtils.format("%s#%s(...)", serviceCallRequest.getServiceName(),
                    serviceCallRequest.getMethodName()));
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
            + serviceCallRequest.getCallerNodeId();
    }

    private ServiceCallResult handleInternal(ServiceCallRequest serviceCallRequest) throws InternalMessagingException {
        long startTime = System.currentTimeMillis();

        final ServiceCallResult scResult;
        String reliableRPCStreamId = serviceCallRequest.getReliableRPCStreamId();
        if (reliableRPCStreamId != null) {
            return reliableRPCStreamService.handleIncomingRequest(serviceCallRequest);
        } else {
            scResult = serviceCallHandler.dispatchToLocalService(serviceCallRequest);
        }

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
                serviceCallRequest.getCallerNodeId(), serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName(), duration));
            // add duration to statistics
            slowMethodCallCounter.registerEvent(
                StringUtils.format("%s#%s", serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName()), duration);
        }
        return scResult;
    }

}
