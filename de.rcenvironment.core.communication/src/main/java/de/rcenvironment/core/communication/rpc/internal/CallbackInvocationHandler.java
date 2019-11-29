/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.api.RemotableCallbackService;
import de.rcenvironment.core.communication.rpc.api.RemoteServiceCallSenderService;
import de.rcenvironment.core.communication.spi.CallbackMethod;
import de.rcenvironment.core.communication.spi.CallbackObject;

/**
 * {@link InvocationHandler} implementation used to create proxy for objects which need to be called back.
 * 
 * TODO review/rework this concept; it is odd that the {@link InvocationHandler} itself is serialized
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class CallbackInvocationHandler implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 3758584730981030172L;

    /**
     * Holds the {@link RemoteServiceCallSenderService} for a node so it can be accessed by deserialized {@link CallbackInvocationHandler}s.
     * (Note that the whole callback concept should be reviewed/reworked.)
     * 
     * @author Robert Mischke
     */
    public static class RemoteServiceCallServiceHolder {

        private static RemoteServiceCallSenderService remoteServiceCallService;

        public RemoteServiceCallServiceHolder() {}

        /**
         * OSGi-DS bind method.
         * 
         * @param newInstance the new service instance
         */
        public void bindRemoteServiceCallService(RemoteServiceCallSenderService newInstance) {
            RemoteServiceCallServiceHolder.remoteServiceCallService = newInstance;
        }

        private static RemoteServiceCallSenderService getRemoteServiceCallService() {
            return remoteServiceCallService;
        }

    }

    private static final transient Log LOGGER = LogFactory.getLog(CallbackInvocationHandler.class);

    private final CallbackObject callbackObject;

    private final String objectId;

    private final InstanceNodeSessionId objectNodeId;

    private final InstanceNodeSessionId proxyNodeId;

    private final Class<?> callbackInterface;

    public CallbackInvocationHandler(CallbackObject callbackObject, String objectIdentifier,
        InstanceNodeSessionId objectHome, InstanceNodeSessionId proxyHome) {

        this.callbackObject = callbackObject;
        this.callbackInterface = callbackObject.getInterface();
        this.objectId = objectIdentifier;
        this.objectNodeId = objectHome;
        this.proxyNodeId = proxyHome;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
        final String methodName = method.getName();
        Object returnValue; // extracted to satisfy CheckStyle
        if (methodName.equals("getHomePlatform")) {
            returnValue = objectNodeId;
        } else if (methodName.equals("getObjectIdentifier")) {
            returnValue = objectId;
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
        parameterList.add(0, objectId);
        parameterList.add(1, methodName);
        if (parameters != null) {
            // TODO improve generics handling -- misc_ro
            final List<Object> parametersAsList = Arrays.asList(parameters);
            parameterList.add(2, new ArrayList(parametersAsList));
        } else {
            parameterList.add(2, new ArrayList<Serializable>());
        }

        ServiceCallRequest serviceCallRequest =
            new ServiceCallRequest(objectNodeId.convertToDefaultLogicalNodeSessionId(),
                proxyNodeId.convertToDefaultLogicalNodeSessionId(),
                RemotableCallbackService.class.getCanonicalName(), "callback", parameterList,
                null); // no "reliable RPC" session support for callbacks yet

        RemoteServiceCallSenderService remoteServiceCallService = RemoteServiceCallServiceHolder.getRemoteServiceCallService();
        return remoteServiceCallService.performRemoteServiceCallAsProxy(serviceCallRequest);

    }

    /**
     * Determines whether the given method is applicable for remote invocation. This is the case if and only if it implements a method in
     * the interface returned by {@link CallbackObject#getInterface()} of the wrapped {@link CallbackObject}, and if the implemented method
     * has a @{@link CallbackMethod} annotation in this interface.
     * 
     * @param method the method to invoke
     * @return true if the method is applicable for remote invocation; see method description
     */
    // TODO add caching by class and method to avoid repeated reflection?
    private boolean matchesCallbackMethod(Method method) {
        final String methodName = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final int parameterCount = parameterTypes.length;

        // skip the interface check for common local methods to prevent needless generation of NoSuchMethodExceptions;
        // also check the parameter count to detect simple name clashes
        final boolean isCommonMethod =
            ("hashCode".equals(methodName) && parameterCount == 0)
                || ("toString".equals(methodName) && parameterCount == 0)
                || ("equals".equals(methodName) && parameterCount == 1);
        if (isCommonMethod) {
            // always dispatch these locally, and do not log them
            return false;
        }

        try {
            Method interfaceMethod = callbackInterface.getMethod(methodName, parameterTypes);
            // if it exists, check if it has a @Callback method annotation in the interface
            return interfaceMethod.isAnnotationPresent(CallbackMethod.class);
        } catch (NoSuchMethodException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Non-interface method called on callback object of interface " + callbackInterface.getName()
                    + ": " + method);
            }
            // not present in interface -> dispatch locally
            return false;
        }
    }

}
