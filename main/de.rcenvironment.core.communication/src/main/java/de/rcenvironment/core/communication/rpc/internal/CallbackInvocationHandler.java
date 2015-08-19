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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.rpc.RemoteServiceCallService;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * {@link InvocationHandler} implementation used to create proxy for objects which need to be called back.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CallbackInvocationHandler implements InvocationHandler, Serializable {

    /**
     * Holds the {@link RemoteServiceCallService} for a node so it can be accessed by deserialized {@link CallbackInvocationHandler}s. (Note
     * that the whole callback concept should be reviewed/reworked.)
     * 
     * @author Robert Mischke
     */
    public static class RemoteServiceCallServiceHolder {

        private static RemoteServiceCallService remoteServiceCallService;

        public RemoteServiceCallServiceHolder() {}

        /**
         * OSGi-DS bind method.
         * 
         * @param newInstance the new service instance
         */
        public static void bindRemoteServiceCallService(RemoteServiceCallService newInstance) {
            RemoteServiceCallServiceHolder.remoteServiceCallService = newInstance;
        }

        private static RemoteServiceCallService getRemoteServiceCallService() {
            return remoteServiceCallService;
        }

    }

    private static final long serialVersionUID = 1L;

    private static final Log LOGGER = LogFactory.getLog(CallbackInvocationHandler.class);

    private final CallbackObject callbackObject;

    private final String objectIdentifier;

    private final NodeIdentifier objectHome;

    private final NodeIdentifier proxyHome;

    public CallbackInvocationHandler(CallbackObject callbackObject, String objectIdentifier,
        NodeIdentifier objectHome, NodeIdentifier proxyHome) {

        this.callbackObject = callbackObject;
        this.objectIdentifier = objectIdentifier;
        this.objectHome = objectHome;
        this.proxyHome = proxyHome;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
        final String methodName = method.getName();
        Object returnValue; // extracted to satisfy CheckStyle
        if (methodName.equals("getHomePlatform")) {
            returnValue = objectHome;
        } else if (methodName.equals("getObjectIdentifier")) {
            returnValue = objectIdentifier;
        } else {
            if (matchesCallbackMethod(method)) {
                returnValue = invokeRemoteMethod(methodName, parameters);
            } else {
                returnValue = method.invoke(callbackObject, parameters);
            }
        }
        return returnValue;
    }

    /**
     * Performs a remote method invocation on the wrapped {@link CallbackObject}.
     * 
     * @param methodName the name of the method to invoke
     * @param parameters the parameters to pass to the method
     * 
     * @return the return value of the remote method call
     * @throws Throwable any {@link Throwable} that was throws by the remote method call
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object invokeRemoteMethod(final String methodName, Object[] parameters) throws Throwable {

        List<Serializable> parameterList = new ArrayList<Serializable>();
        parameterList.add(0, objectIdentifier);
        parameterList.add(1, methodName);
        if (parameters != null) {
            // TODO improve generics handling -- misc_ro
            final List<Object> parametersAsList = Arrays.asList(parameters);
            parameterList.add(2, new ArrayList(parametersAsList));
        } else {
            parameterList.add(2, new ArrayList<Serializable>());
        }

        ServiceCallRequest serviceCallRequest = new ServiceCallRequest(objectHome, proxyHome,
            CallbackService.class.getCanonicalName(), null, "callback", parameterList);

        RemoteServiceCallService remoteServiceCallService = RemoteServiceCallServiceHolder.getRemoteServiceCallService();
        ServiceCallResult serviceCallResult = remoteServiceCallService.performRemoteServiceCall(serviceCallRequest);

        if (serviceCallResult == null) {
            // shouldn't normally happen
            LOGGER.warn(String.format("Received null object as service call result (id: %s, method: %s); returning null as return value",
                objectIdentifier, methodName));
            return null;
        }

        Throwable throwable = serviceCallResult.getThrowable();
        if (throwable != null) {
            throw throwable;
        }

        return serviceCallResult.getReturnValue();
    }

    /**
     * Determines whether the given method is applicable for remote invocation. This is the case if and only if it implements a method in
     * the interface returned by {@link CallbackObject#getInterface()} of the wrapped {@link CallbackObject}, and if the implemented method
     * has a @{@link CallbackMethod} annotation in this interface.
     * 
     * @param method the method to invoke
     * @return true if the method is applicable for remote invocation; see method description
     */
    private boolean matchesCallbackMethod(Method method) {
        final String methodName = method.getName();
        try {
            // check whether the method to invoke is part of the callback interface
            Method interfaceMethod = callbackObject.getInterface().getMethod(methodName, method.getParameterTypes());
            // if it exists, check if it has a @Callback method annotation in the interface
            return interfaceMethod.isAnnotationPresent(CallbackMethod.class);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled()) {
                // do not log some common methods to reduce log volume
                final boolean isCommonMethod =
                    methodName.equals("hashCode") || methodName.equals("equals") || methodName.equals("toString");
                if (!isCommonMethod) {
                    LOGGER.debug("Non-interface method called: " + method);
                }
            }
            // not present in interface -> always dispatch locally
            return false;
        }
    }

}
