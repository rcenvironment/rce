/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccessFactory;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * {@link ServiceRegistryAccessFactory} implementation that produces {@link OsgiServiceRegistryAccess} instances.
 * 
 * If possible, services are acquired and registered using the {@link BundleContext} of the OSGi {@link Bundle} of the caller. If this
 * bundle cannot be determined, the {@link BundleContext} provided at creation is used as fallback.
 * 
 * @author Robert Mischke
 */
public class OsgiServiceRegistryAccessFactory implements ServiceRegistryAccessFactory {

    private final BundleContext fallbackBundleContext;

    private final Log log = LogFactory.getLog(getClass());

    public OsgiServiceRegistryAccessFactory(BundleContext fallbackBundleContext) {
        this.fallbackBundleContext = fallbackBundleContext;
    }

    @Override
    public ServiceRegistryAccess createAccessFor(Object caller) {
        // return common implementation under the restricted interface for now
        // TODO improve?
        return createPublisherAccessFor(caller);
    }

    @Override
    public ServiceRegistryPublisherAccess createPublisherAccessFor(Object caller) {
        // explicit check for a common error when using this API - misc_ro
        if (caller.getClass() == Class.class) {
            throw new IllegalArgumentException(
                "This argument for this method should not be the *class* of the caller (e.g. \"this.getClass()\"),"
                    + " but an arbitrary class *instance* from the caller's bundle (e.g. \"this\")");
        }
        // TODO add caller tracking to find service leaks
        Bundle callerBundle = FrameworkUtil.getBundle(caller.getClass());
        if (callerBundle != null) {
            BundleContext bundleContext = callerBundle.getBundleContext();
            if (bundleContext != null) {
                return new OsgiServiceRegistryAccess(bundleContext);
            } else {
                log.warn("Unexpected state: Bundle " + callerBundle.getSymbolicName()
                    + " returned a null BundleContext (using fallback); bundle state: " + callerBundle.getState());
            }
        } else {
            log.warn(
                "Using fallback BundleContext as the containing bundle could not be identified for " + caller.getClass());
        }
        return new OsgiServiceRegistryAccess(fallbackBundleContext);
    }
}
