/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.api.RemoteServiceCallSenderService;
import de.rcenvironment.core.communication.rpc.spi.ServiceProxyFactory;
import de.rcenvironment.core.utils.common.StatsCounter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.ThreadGuard;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
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

    private RemoteServiceCallSenderService remoteServiceCallService;

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
    public void bindRemoteServiceCallService(RemoteServiceCallSenderService newInstance) {
        remoteServiceCallService = newInstance;
    }

    @Override
    public Object createServiceProxy(final NodeIdentifier nodeId, final Class<?> serviceIface, Class<?>[] ifaces) {

        Assertions.isDefined(nodeId, "The identifier of the requested platform" + ASSERT_MUST_NOT_BE_NULL);
        Assertions.isDefined(serviceIface, "The interface of the requested service" + ASSERT_MUST_NOT_BE_NULL);

        InvocationHandler handler = new InvocationHandler() {

            private PlatformService ps = platformService;

            private CallbackService cs = callbackService;

            private CallbackProxyService cps = callbackProxyService;

            private NodeIdentifier pi = nodeId;

            private Class<?> myService = serviceIface;

            @Override
            public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {

                // this should usually not be called from the GUI thread
                ThreadGuard.checkForForbiddenThread();

                if (remoteServiceCallService == null) {
                    // internal error
                    throw new RemoteOperationException("RemoteServiceCallService was null");
                }

                // check to avoid string concatenation if stats are disabled
                if (StatsCounter.isEnabled()) {
                    StatsCounter.count("Remote service calls (sent)",
                        StringUtils.format("%s#%s(...)", myService.getName(), method.getName()));
                }

                List<Serializable> parameterList = new ArrayList<>();
                if (parameters != null) {
                    for (Object parameter : parameters) {
                        StatsCounter.countClass("Remote service proxy parameters (sent)", parameter);
                        parameterList.add((Serializable) CallbackUtils.handleCallbackObject(parameter, pi, cs));
                    }
                }

                ServiceCallRequest serviceCallRequest = new ServiceCallRequest(pi, ps.getLocalNodeId(),
                    myService.getCanonicalName(), method.getName(), parameterList);

                Object returnValue = remoteServiceCallService.performRemoteServiceCallAsProxy(serviceCallRequest);
                if (returnValue != null) {
                    returnValue = CallbackUtils.handleCallbackProxy(returnValue, cs, cps);
                }
                return returnValue;
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
