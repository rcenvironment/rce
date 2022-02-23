/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.LiveNetworkIdResolutionService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.api.RemoteServiceCallSenderService;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.Assertions;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadGuard;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * Implementation of the {@link ServiceProxyFactory}.
 * 
 * @author Dirk Rossow
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public final class ServiceProxyFactoryImpl implements ServiceProxyFactory {

    private static final String ASSERT_MUST_NOT_BE_NULL = " must not be null!";

    private static final long serialVersionUID = -4239349616520603192L;

    private static final Log sharedLogInstance = LogFactory.getLog(ServiceProxyFactoryImpl.class);

    private static final long ID_RESOLUTION_MAX_RETRIES = 5;

    private static final long ID_RESOLUTION_RETRY_WAIT_MSEC = 100;

    private PlatformService platformService;

    private CallbackService callbackService;

    private CallbackProxyService callbackProxyService;

    private RemoteServiceCallSenderService remoteServiceCallService;

    private CounterCategory parameterTypesCounter;

    private CounterCategory methodCallCounter;

    private LiveNetworkIdResolutionService idResolutionService;

    public ServiceProxyFactoryImpl() {
        // not injecting this via OSGi-DS as this service is planned to move to the toolkit layer anyway - misc_ro
        final StatisticsTrackerService statisticsService =
            StaticToolkitHolder.getServiceWithUnitTestFallback(StatisticsTrackerService.class);
        methodCallCounter =
            statisticsService.getCounterCategory("Remote service calls (sent): service methods", StatisticsFilterLevel.RELEASE);
        parameterTypesCounter =
            statisticsService.getCounterCategory("Remote service calls (sent): parameter types", StatisticsFilterLevel.DEVELOPMENT);
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindPlatformService(PlatformService newInstance) {
        platformService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindLiveNetworkIdResolutionService(LiveNetworkIdResolutionService newInstance) {
        this.idResolutionService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindCallbackService(CallbackService newInstance) {
        callbackService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindCallbackProxyService(CallbackProxyService newInstance) {
        callbackProxyService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    public void bindRemoteServiceCallService(RemoteServiceCallSenderService newInstance) {
        remoteServiceCallService = newInstance;
    }

    @Override
    public Object createServiceProxy(final ResolvableNodeId nodeId, final Class<?> serviceIface, Class<?>[] ifaces,
        final ReliableRPCStreamHandle reliableRPCStreamHandle) {

        Assertions.isDefined(nodeId, "The identifier of the requested platform" + ASSERT_MUST_NOT_BE_NULL);
        Assertions.isDefined(serviceIface, "The interface of the requested service" + ASSERT_MUST_NOT_BE_NULL);

        final InvocationHandler handler = new InvocationHandler() {

            private final PlatformService ps = platformService;

            private final CallbackService cs = callbackService;

            private final CallbackProxyService cps = callbackProxyService;

            private final Class<?> myService = serviceIface;

            private final ResolvableNodeId destinationNodeId = nodeId; // original destination id

            private LogicalNodeSessionId destinationLogicalNodeSessionId; // resolved destination id

            @Override
            public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {

                // this should usually not be called from the GUI thread
                ThreadGuard.checkForForbiddenThread();

                synchronized (this) {
                    if (destinationLogicalNodeSessionId == null) {
                        // lazy resolution at first actual service call time
                        resolveDestinationNodeIdOrFail();
                    }
                }

                if (remoteServiceCallService == null) {
                    // internal error
                    throw new RemoteOperationException("RemoteServiceCallService was null");
                }

                // check to avoid string concatenation if stats are disabled
                if (methodCallCounter.isEnabled()) {
                    methodCallCounter.count(StringUtils.format("%s#%s(...)", myService.getName(), method.getName()));
                }

                List<Serializable> parameterList = new ArrayList<>();
                if (parameters != null) {
                    for (Object parameter : parameters) {
                        if (parameterTypesCounter.isEnabled()) {
                            parameterTypesCounter.countClass(parameter);
                        }
                        parameterList.add((Serializable) CallbackUtils.handleCallbackObject(parameter,
                            destinationLogicalNodeSessionId.convertToInstanceNodeSessionId(), cs));
                    }
                }

                ServiceCallRequest serviceCallRequest =
                    new ServiceCallRequest(destinationLogicalNodeSessionId, ps.getLocalDefaultLogicalNodeSessionId(),
                        myService.getCanonicalName(), method.getName(), parameterList, reliableRPCStreamHandle);

                Object returnValue = remoteServiceCallService.performRemoteServiceCallAsProxy(serviceCallRequest);
                if (returnValue != null) {
                    returnValue = CallbackUtils.handleCallbackProxy(returnValue, cs, cps);
                }
                return returnValue;
            }

            private void resolveDestinationNodeIdOrFail() throws RemoteOperationException {
                int retries = 0;

                if (reliableRPCStreamHandle != null) {
                    // for reliable RPC sessions, the logical node must have been specifically resolved on session start already;
                    // anything else would be an internal consistency error -- misc_ro
                    destinationLogicalNodeSessionId = (LogicalNodeSessionId) destinationNodeId;
                    return;
                }

                // TODO this retry loop is mostly needed for unit tests, as waiting for node visibility does not guarantee complete
                // propagation of ids to the resolution service; check if there is a more elegant solution to this - misc_ro, 8.0.0
                while (true) {
                    try {
                        destinationLogicalNodeSessionId = idResolutionService.resolveToLogicalNodeSessionId(destinationNodeId);
                        if (retries > 0) {
                            sharedLogInstance.debug("Resolved service call destination id " + destinationNodeId + " after " + retries
                                + " failed attempts");
                        }
                        return;
                    } catch (IdentifierException e) {
                        if (retries < ID_RESOLUTION_MAX_RETRIES) {
                            try {
                                Thread.sleep(ID_RESOLUTION_RETRY_WAIT_MSEC);
                            } catch (InterruptedException e1) {
                                throw new RemoteOperationException(
                                    "Interrupted while waiting to retry id resolution for " + destinationNodeId);
                            }
                            retries++;
                        } else {
                            sharedLogInstance.debug(
                                "Converting id resolution exception to a " + RemoteOperationException.class.getSimpleName()
                                    + " of type "
                                    + ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER + ": " + e.toString());
                            // intended to show similar behavior as NetworkResponseFactory.generateResponseForNoRouteAtSender()
                            throw new RemoteOperationException(StringUtils.format("%s; the destination instance was %s",
                                ProtocolConstants.ResultCode.NO_ROUTE_TO_DESTINATION_AT_SENDER.toString(), destinationNodeId));
                        }
                    }
                }
            }
        };

        if (ifaces == null) {
            return Proxy.newProxyInstance(serviceIface.getClassLoader(), new Class[] { serviceIface }, handler);
        } else {
            Class<?>[] allIfaces = new Class<?>[ifaces.length + 1];
            allIfaces[0] = serviceIface;
            System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);
            return Proxy.newProxyInstance(serviceIface.getClassLoader(), allIfaces, handler);
        }
    }
}
