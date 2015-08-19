/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.rpc.RemoteServiceCallService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceProxyFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.concurrent.ThreadGuard;
import de.rcenvironment.core.utils.incubator.Assertions;

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

    private static final Log LOGGER = LogFactory.getLog(ServiceProxyFactoryImpl.class);

    private PlatformService platformService;

    private CallbackService callbackService;

    private CallbackProxyService callbackProxyService;

    private RemoteServiceCallService remoteServiceCallService;

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
    public void bindRemoteServiceCallService(RemoteServiceCallService newInstance) {
        remoteServiceCallService = newInstance;
    }

    @Override
    public Object createServiceProxy(NodeIdentifier nodeId, Class<?> serviceIface, Class<?>[] ifaces,
        Map<String, String> serviceProperties) {

        return createServiceProxy(nodeId, serviceIface, ifaces, ServiceUtils.constructFilter(serviceProperties));
    }

    @Override
    public Object createServiceProxy(final NodeIdentifier nodeId, final Class<?> serviceIface, Class<?>[] ifaces,
        final String serviceProperties) {

        Assertions.isDefined(nodeId, "The identifier of the requested platform" + ASSERT_MUST_NOT_BE_NULL);
        Assertions.isDefined(serviceIface, "The interface of the requested service" + ASSERT_MUST_NOT_BE_NULL);

        InvocationHandler handler = new InvocationHandler() {

            private PlatformService ps = platformService;

            private CallbackService cs = callbackService;

            private CallbackProxyService cps = callbackProxyService;

            private NodeIdentifier pi = nodeId;

            private Class<?> myService = serviceIface;

            private String myProperties = serviceProperties;

            @Override
            public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {

                // this should usually not be called from the GUI thread
                ThreadGuard.checkForForbiddenThread();

                // check to avoid string concatenation if stats are disabled
                if (StatsCounter.isEnabled()) {
                    StatsCounter.count("Remote service calls", String.format("%s#%s(...)", myService.getName(), method.getName()));
                }

                List<Serializable> parameterList = new ArrayList<>();
                if (parameters != null) {
                    for (Object parameter : parameters) {
                        StatsCounter.countClass("Remote service proxy parameters", parameter);
                        parameterList.add((Serializable) CallbackUtils.handleCallbackObject(parameter, pi, cs));
                    }
                }

                if (myProperties != null && myProperties.isEmpty()) {
                    myProperties = null;
                }

                ServiceCallRequest serviceCallRequest = new ServiceCallRequest(pi, ps.getLocalNodeId(),
                    myService.getCanonicalName(), myProperties, method.getName(), parameterList);
                ServiceCallResult serviceCallResult = null;
                if (remoteServiceCallService != null) {
                    serviceCallResult =
                        remoteServiceCallService.performRemoteServiceCall(serviceCallRequest);
                    if (serviceCallResult == null) {
                        // should not happen in version 6.1.0 or higher anymore; left in for safety
                        throw new RuntimeException(String.format(
                            "Unexpected null service call result for RPC to method %s#%s() on %s",
                            serviceCallRequest.getService(), serviceCallRequest.getServiceMethod(),
                            serviceCallRequest.getRequestedPlatform()));
                    }
                    Throwable throwable = serviceCallResult.getThrowable();
                    if (throwable != null) {
                        // TODO review: check for UndeclaredThrowables here and log as warning? - misc_ro
                        // TODO @5.0? - add "failure reporting node" information when available - misc_ro
                        LOGGER.debug(String
                            .format("Exception caught in response to RPC for %s#%s() on %s "
                                + "(the error may also have occurred on an intermediate node)",
                                serviceCallRequest.getService(), method.getName(), serviceCallRequest.getRequestedPlatform()),
                            throwable);
                        throw throwable;
                    }
                    Object returnValue = serviceCallResult.getReturnValue();
                    if (returnValue != null) {
                        returnValue = CallbackUtils.handleCallbackProxy(returnValue, cs, cps);
                    }
                } else {
                    throw new Throwable("RemoteServiceCallService was null.");
                }
                // TODO check: shouldn't "returnValue" be returned here? - misc_ro
                return serviceCallResult.getReturnValue();
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
