/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.rpc.ServiceCallHandler;
import de.rcenvironment.core.communication.rpc.ServiceCallRequest;
import de.rcenvironment.core.communication.rpc.ServiceCallResult;
import de.rcenvironment.core.communication.rpc.api.CallbackProxyService;
import de.rcenvironment.core.communication.rpc.api.CallbackService;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheck;
import de.rcenvironment.core.utils.common.security.MethodPermissionCheckHasAnnotation;
import de.rcenvironment.core.utils.incubator.Assertions;

/**
 * Implementation of the {@link ServiceCallHandler}.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class OSGiServiceCallHandlerImpl implements ServiceCallHandler {

    private static final String COLON = ":";

    /** Maximal number of hops to traverse. */
    private static final int MAXIMAL_HOPS = 15;

    private static final String ERROR_GET_SERVICE = "Failed to get the service from the OSGi registry: ";

    private static final String ERROR_MAXIMAL_HOPS_REACHED = "Maximal number of hops reached for request: ";

    private static final Log LOGGER = LogFactory.getLog(OSGiServiceCallHandlerImpl.class);

    // the callback that verifies the presence of @AllowRemoteCall annotations
    private static final MethodPermissionCheck METHOD_PERMISSION_CHECK = new MethodPermissionCheckHasAnnotation(AllowRemoteAccess.class);

    private static final int SERVICE_RESOLUTION_NUM_RETRIES = 10;

    private static final int SERVICE_RESOLUTION_RETRY_DELAY_MSEC = 1000;

    private BundleContext bundleContext;

    private PlatformService platformService;

    private CallbackService callbackService;

    private CallbackProxyService callbackProxyService;

    protected void activate(BundleContext context) {
        bundleContext = context;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    protected void bindCallbackService(CallbackService newCallbackService) {
        callbackService = newCallbackService;
    }

    protected void bindCallbackProxyService(CallbackProxyService newCallbackProxyService) {
        callbackProxyService = newCallbackProxyService;
    }

    @Override
    public ServiceCallResult handle(ServiceCallRequest serviceCallRequest) throws CommunicationException {

        if (!platformService.isLocalNode(serviceCallRequest.getRequestedPlatform())) {
            throw new IllegalStateException("Internal consistency error: called to handle a ServiceCallResult for another node");
        }

        Assertions.isDefined(serviceCallRequest, "The parameter \"serviceCallRequest\" must not be null.");

        // LOGGER.debug("Received a service call request: " +
        // serviceCallRequest.getRequestedPlatform() + COLON
        // + serviceCallRequest.getService() + COLON + serviceCallRequest.getServiceMethod());

        if (serviceCallRequest.increaseHopCount() > MAXIMAL_HOPS) {
            throw new CommunicationException(ERROR_MAXIMAL_HOPS_REACHED + serviceCallRequest.getRequestedPlatform()
                + COLON + serviceCallRequest.getService());
        }

        return handleLocal(serviceCallRequest);
    }

    /**
     * Handles a service call request locally.
     * 
     * @param serviceCallRequest {@link ServiceCallRequest} with all information about the method to call.
     * @return The {@link ServiceCallResult} with the result of the service call.
     * @throws CommunicationException Thrown if the call failed.
     */
    private ServiceCallResult handleLocal(ServiceCallRequest serviceCallRequest) throws CommunicationException {

        Object[] parameters = serviceCallRequest.getParameterList().toArray();
        List<Serializable> parameterList = new ArrayList<>();
        for (Object parameter : parameters) {
            parameterList.add((Serializable) CallbackUtils.handleCallbackProxy(parameter, callbackService, callbackProxyService));
        }

        // String uuid = serviceCallRequest.getService();
        // if (serviceCallRequest.getServiceProperties() != null) {
        // uuid += COLON + serviceCallRequest.getServiceProperties();
        // }

        Object service =
            lookupService(serviceCallRequest.getService(), serviceCallRequest.getServiceProperties(), SERVICE_RESOLUTION_NUM_RETRIES,
                SERVICE_RESOLUTION_RETRY_DELAY_MSEC);

        Object returnValue = null;
        try {
            returnValue = MethodCaller.callMethod(service, serviceCallRequest.getServiceMethod(), parameterList, METHOD_PERMISSION_CHECK);
        } catch (NullPointerException e) {
            // TODO review: what if the target method threw this NPE? - misc_ro
            throw new CommunicationException("The service could not be called: " + service + " - the service is gone.");
        }

        if (returnValue != null) {
            if (!(returnValue instanceof Serializable)) {
                throw new CommunicationException("Return value is not serializable: " + returnValue.getClass().getName());
            }
            returnValue = CallbackUtils.handleCallbackObject(returnValue, serviceCallRequest.getCallingPlatform(), callbackService);
            return new ServiceCallResult((Serializable) returnValue);
        } else {
            return new ServiceCallResult(null);
        }

    }

    /**
     * 
     * Gets the service from the OSGi service registry.
     * 
     * @param service The name of the service to get.
     * @param filter The desired properties of the service.
     * @param numAttempts the number of attempts before the service is considered unavailable
     * @param delayBetweenAttemptsMsec the delay (in milliseconds) between attempts to acquire the service
     * @return the service object
     * @throws CommunicationException if the service could not be got.
     */
    protected Object lookupService(String service, String filter, int numAttempts, int delayBetweenAttemptsMsec)
        throws CommunicationException {

        ServiceReference<?>[] serviceReferences = null;

        int attempt = 1;
        while (attempt <= numAttempts) {
            try {
                serviceReferences = bundleContext.getAllServiceReferences(service, filter);
            } catch (InvalidSyntaxException e) {
                throw new CommunicationException(ERROR_GET_SERVICE + service + " - invalid protocol filter syntax: " + filter);
            }

            Object serviceObject;
            if (serviceReferences != null && serviceReferences.length > 0) {
                if (serviceReferences.length > 1) {
                    // warn if more than one service matched
                    LOGGER
                        .warn("More than one OSGi service reference matched (request: service=" + service + ", filter=" + filter + ")");
                }
                serviceObject = bundleContext.getService(serviceReferences[0]);
                if (serviceObject != null) {
                    // success
                    return serviceObject;
                }
            }

            // prepare for retry
            attempt++;
            if (attempt <= numAttempts) {
                LOGGER.warn(String.format("Failed to acquire OSGi service on attempt #%d; "
                    + "it may not have started yet, retrying after %d msec (request: service=%s, filter=%s)",
                    attempt, delayBetweenAttemptsMsec, service, filter));
                try {
                    Thread.sleep(delayBetweenAttemptsMsec);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted while waiting for retry");
                }
            }
        }
        throw new CommunicationException(ERROR_GET_SERVICE + service + " (filter: " + filter + ") - service not available; made "
            + numAttempts + " attempt(s)");
    }
}
