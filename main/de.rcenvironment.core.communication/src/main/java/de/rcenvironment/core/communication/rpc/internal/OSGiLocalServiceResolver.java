/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import de.rcenvironment.core.communication.rpc.spi.LocalServiceResolver;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Resolves service requests against the OSGi service registry, using the containing bundle's {@link BundleContext}.
 * 
 * @author Robert Mischke
 */
public class OSGiLocalServiceResolver implements LocalServiceResolver {

    private static final int SINGLETON_SERVICE_RESOLUTION_NUM_ATTEMPTS = 10;

    private static final int SERVICE_RESOLUTION_RETRY_DELAY_MSEC = 1000;

    private static final String ERROR_GET_SERVICE = "Failed to acquire OSGi service ";

    private final Log log = LogFactory.getLog(getClass());

    private BundleContext bundleContext;

    private final int retryCount;

    /**
     * Default constructor; uses a retry count of {@link #SINGLETON_SERVICE_RESOLUTION_NUM_ATTEMPTS}.
     */
    public OSGiLocalServiceResolver() {
        this.retryCount = SINGLETON_SERVICE_RESOLUTION_NUM_ATTEMPTS;
    }

    /**
     * Constructor that allows setting a custom retry count (intended to speed up unit testing).
     * 
     * @param retryCount the number of lookup retries before giving up
     */
    public OSGiLocalServiceResolver(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * OSGi life cycle method.
     * 
     * @param context the {@link BundleContext} to resolve against
     */
    public void activate(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public Object getLocalService(String serviceName) {
        // allow several retries as the service may be still starting up
        return getLocalSingletonService(serviceName, retryCount, SERVICE_RESOLUTION_RETRY_DELAY_MSEC);
    }

    /**
     * 
     * Gets the service from the OSGi service registry.
     * 
     * @param service The name of the service to get.
     * @param numAttempts the number of attempts before the service is considered unavailable
     * @param delayBetweenAttemptsMsec the delay (in milliseconds) between attempts to acquire the service
     * @return the service object
     * @throws RemoteOperationException if the service could not be got.
     */
    protected Object getLocalSingletonService(String service, int numAttempts, int delayBetweenAttemptsMsec) {

        ServiceReference<?>[] serviceReferences = null;

        int attempt = 1;
        while (attempt <= numAttempts) {
            try {
                serviceReferences = bundleContext.getServiceReferences(service, null);
            } catch (InvalidSyntaxException e) {
                log.error(ERROR_GET_SERVICE + service, e);
                return null;
            }

            Object serviceObject;
            if (serviceReferences != null && serviceReferences.length > 0) {
                if (serviceReferences.length > 1) {
                    log.error("More than one OSGi service reference matched (request: service=" + service + ")");
                    return null;
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
                log.warn(StringUtils.format("Failed to acquire OSGi service on attempt #%d; "
                    + "it may not have started yet, retrying after %d msec (request: service=%s)", attempt - 1,
                    delayBetweenAttemptsMsec, service));
                try {
                    Thread.sleep(delayBetweenAttemptsMsec);
                } catch (InterruptedException e) {
                    log.error("Interrupted while waiting for retry");
                    return null;
                }
            }
        }
        log.error(ERROR_GET_SERVICE + service + " - service not available; made " + numAttempts + " attempt(s)");
        return null;
    }
}
