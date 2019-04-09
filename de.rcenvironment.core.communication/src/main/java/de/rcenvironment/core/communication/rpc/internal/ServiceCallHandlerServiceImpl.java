/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.api.ServiceCallContextUtils;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.messaging.internal.InternalMessagingException;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.ServiceCallResultFactory;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.communication.rpc.spi.LocalServiceLookupResult;
import de.rcenvironment.core.communication.rpc.spi.LocalServiceResolver;
import de.rcenvironment.core.communication.rpc.spi.RemoteServiceCallHandlerService;
import de.rcenvironment.core.toolkitbridge.api.StaticToolkitHolder;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheckHasAnnotation;
import de.rcenvironment.core.utils.incubator.Assertions;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextMemento;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * Implementation of the {@link RemoteServiceCallHandlerService}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
@Component
public class ServiceCallHandlerServiceImpl implements RemoteServiceCallHandlerService {

    // the callback that verifies the presence of @AllowRemoteCall annotations
    private static final MethodPermissionCheck METHOD_PERMISSION_CHECK = new MethodPermissionCheckHasAnnotation(AllowRemoteAccess.class);

    private PlatformService platformService;

    private CallbackService callbackService;

    private CallbackProxyService callbackProxyService;

    private LocalServiceResolver serviceResolver;

    private final Map<String, LocalServiceLookupResult> serviceCache;

    private final Log log = LogFactory.getLog(getClass());

    private final CounterCategory parameterTypesCounter;

    public ServiceCallHandlerServiceImpl() {
        serviceCache = new HashMap<>();

        // not injecting this via OSGi-DS as this service is planned to move to the toolkit layer anyway - misc_ro
        final StatisticsTrackerService statisticsService =
            StaticToolkitHolder.getServiceWithUnitTestFallback(StatisticsTrackerService.class);
        parameterTypesCounter =
            statisticsService.getCounterCategory("Remote service calls (received): parameter types", StatisticsFilterLevel.DEVELOPMENT);
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindLocalServiceResolver(LocalServiceResolver newInstance) {
        this.serviceResolver = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindPlatformService(PlatformService newInstance) {
        platformService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindCallbackService(CallbackService newInstance) {
        callbackService = newInstance;
    }

    /**
     * OSGi-DS bind method; made public for integration testing.
     * 
     * @param newInstance the new service instance
     */
    @Reference
    public void bindCallbackProxyService(CallbackProxyService newInstance) {
        callbackProxyService = newInstance;
    }

    @Override
    public ServiceCallResult dispatchToLocalService(ServiceCallRequest serviceCallRequest) throws InternalMessagingException {
        Assertions.isDefined(serviceCallRequest, "The parameter \"serviceCallRequest\" must not be null.");

        if (!platformService.matchesLocalInstance(serviceCallRequest.getTargetNodeId())) {
            throw new IllegalStateException("Internal consistency error: called to handle a ServiceCallResult for another node");
        }

        final ThreadContextMemento previousThreadContext =
            ServiceCallContextUtils.attachServiceCallDataToThreadContext(serviceCallRequest.getCallerNodeId(),
                serviceCallRequest.getTargetNodeId(), serviceCallRequest.getServiceName(), serviceCallRequest.getMethodName());
        try {
            return invokeLocalService(serviceCallRequest);
        } finally {
            previousThreadContext.restore();
        }
    }

    /**
     * Handles a service call request locally.
     * 
     * @param serviceCallRequest {@link ServiceCallRequest} with all information about the method to call.
     * @return The {@link ServiceCallResult} with the result of the service call.
     * @throws CommunicationException Thrown if the call failed.
     */
    protected ServiceCallResult invokeLocalService(ServiceCallRequest serviceCallRequest) throws InternalMessagingException {

        Object[] parameters = serviceCallRequest.getParameterList().toArray();
        List<Serializable> parameterList = new ArrayList<>();
        for (Object parameter : parameters) {
            parameterList.add((Serializable) CallbackUtils.handleCallbackProxy(parameter, callbackService, callbackProxyService));
        }
        // count parameter types if enabled
        if (parameterTypesCounter.isEnabled()) {
            for (Object parameter : parameterList) {
                parameterTypesCounter.countClass(parameter);
            }
        }

        final String serviceName = serviceCallRequest.getServiceName();

        LocalServiceLookupResult serviceLookupResult;
        synchronized (serviceCache) {
            serviceLookupResult = serviceCache.get(serviceName);
            if (serviceLookupResult == null) {
                serviceLookupResult = lookupAndValidateService(serviceCallRequest, serviceName);
                serviceCache.put(serviceName, serviceLookupResult);
            }
        }

        if (!serviceLookupResult.isValidRemotableService()) {
            // no matching valid service
            return ServiceCallResultFactory.representInvalidRequestAtHandler(serviceCallRequest, "No matching service found");
        }

        final String methodName = serviceCallRequest.getMethodName();
        if (!serviceLookupResult.isValidMethodRequest(methodName)) {
            // invalid method of existing service requested
            return ServiceCallResultFactory.representInvalidRequestAtHandler(serviceCallRequest,
                "Matching service found, but the method is not allowed to be called");
        }

        try {
            Object returnValue;
            try {
                returnValue =
                    MethodCaller.callMethod(serviceLookupResult.getImplementation(), methodName, parameterList, METHOD_PERMISSION_CHECK);
            } catch (InvocationTargetException e) {
                final Throwable methodException = e.getCause();
                // TODO 7.0.0: review: more detailed checks necessary?
                if (methodException instanceof Exception && !(methodException instanceof RuntimeException)) {
                    return ServiceCallResultFactory.wrapMethodException((Exception) methodException);
                } else {
                    return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest,
                        "Unexpected Throwable during service method invocation", methodException);
                }
            } catch (RemoteOperationException e) {
                return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest,
                    "Error during service method invocation", e);
            }
            if (returnValue != null) {
                if (!(returnValue instanceof Serializable)) {
                    final String message = StringUtils.format("Return value is not serializable: " + returnValue.getClass().getName());
                    return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest, message);
                }
                returnValue =
                    CallbackUtils.handleCallbackObject(returnValue, serviceCallRequest.getCallerNodeId().convertToInstanceNodeSessionId(),
                        callbackService);
                return ServiceCallResultFactory.wrapReturnValue((Serializable) returnValue);
            } else {
                return ServiceCallResultFactory.wrapReturnValue(null);
            }
        } catch (RuntimeException e) {
            return ServiceCallResultFactory.representInternalErrorAtHandler(serviceCallRequest, "Uncaught RuntimeException", e);
        }

    }

    private LocalServiceLookupResult lookupAndValidateService(ServiceCallRequest serviceCallRequest, final String serviceName) {
        final Class<?> serviceInterface;
        try {
            serviceInterface = Class.forName(serviceName);
        } catch (ClassNotFoundException e) {
            log.warn(StringUtils.format("Found no interface for service '%s' requested from '%s'", serviceName,
                serviceCallRequest.getCallerNodeId()));
            return LocalServiceLookupResult.createInvalidServicePlaceholder();
        }
        if (!serviceInterface.isAnnotationPresent(RemotableService.class)) {
            log.warn(StringUtils
                .format("Found the requested service interface '%s', but it is not a %s; refusing access",
                    serviceName, RemotableService.class.getSimpleName()));
            return LocalServiceLookupResult.createInvalidServicePlaceholder();
        }

        final Object serviceImplementation = serviceResolver.getLocalService(serviceName);
        if (serviceImplementation == null) {
            log.warn(StringUtils
                .format("Found the service interface '%s' requested by %s, but no registered implementation",
                    serviceName, serviceCallRequest.getCallerNodeId()));
            return LocalServiceLookupResult.createInvalidServicePlaceholder();
        }
        if (!serviceInterface.isAssignableFrom(serviceImplementation.getClass())) {
            log.error(StringUtils
                .format("Consistency error: Found the service interface '%s', but the resolved implementation %s is not assignable to it!",
                    serviceName, serviceImplementation.getClass()));
            return LocalServiceLookupResult.createInvalidServicePlaceholder();
        }

        Set<String> encounteredMethodNamesWithParameterCount = new HashSet<>();
        Set<String> validatedMethodNames = new HashSet<>();
        // in case of collisions, make sure following methods of the same name don't get allowed
        Set<String> blockedMethodNames = new HashSet<>();

        final Method[] methods = serviceInterface.getMethods();
        for (Method method : methods) {
            final String methodName = method.getName();
            final int parameterCount = method.getParameterTypes().length;
            final Class<?>[] exceptionTypes = method.getExceptionTypes();

            boolean roeDeclared = false;
            boolean allowMethodAccess = true;

            // check for method overloading with same number of parameters
            if (!encounteredMethodNamesWithParameterCount.add(methodName + "/" + parameterCount)) {
                log.error(StringUtils.format("Found overloaded method variants with same parameter count for %s#%s(), "
                    + "which is not allowed in remote service interfaces", serviceName, methodName));
                blockedMethodNames.add(methodName);
                allowMethodAccess = false;
            }

            // check exception declarations
            for (Class<?> exceptionClass : exceptionTypes) {
                if (RuntimeException.class.isAssignableFrom(exceptionClass)) {
                    log.error(StringUtils.format("Method %s#%s() declares 'throws %s', which is a RuntimeException", serviceName,
                        methodName, exceptionClass.getName()));
                    allowMethodAccess = false;
                }
                if (exceptionClass == RemoteOperationException.class) {
                    roeDeclared = true;
                } else {
                    // for all other exceptions, check presence of a string-only constructor
                    try {
                        final Constructor<?> stringOnlyConstructor = exceptionClass.getConstructor(String.class);
                        Assertions.isDefined(stringOnlyConstructor, "Unexpected: getConstructor() should never return null");
                    } catch (NoSuchMethodException e) {
                        log.error(StringUtils.format(
                            "Method %s#%s() declares 'throws %s', but the exception class does not have a string-only constructor",
                            serviceName, methodName, exceptionClass.getName()));
                        allowMethodAccess = false;
                    }
                }
            }

            // check for presence of RemoteOperationException
            if (!roeDeclared) {
                log.error(StringUtils.format(
                    "Method %s#%s() is used as part of a remote service interface, but does not declare 'throws %s'",
                    serviceName, methodName, RemoteOperationException.class.getSimpleName()));
                allowMethodAccess = false;
            }

            // TODO add additional method signature checks? e.g. valid parameter types
            // TODO also check implementing class?

            if (allowMethodAccess) {
                validatedMethodNames.add(methodName);
            } else {
                log.warn(StringUtils.format(
                    "Preventing remote access to %s#%s() as it violates one or more remote service method constraints",
                    serviceName, methodName, RemoteOperationException.class.getSimpleName()));
            }

        }

        validatedMethodNames.removeAll(blockedMethodNames); // "blocked" overrides permission

        log.debug(StringUtils.format("Verified remote service methods for interface %s: %s", serviceName,
            Arrays.toString(validatedMethodNames.toArray())));

        return new LocalServiceLookupResult(serviceImplementation, validatedMethodNames);
    }
}
