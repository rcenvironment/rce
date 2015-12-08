/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistryPublisherAccess;

/**
 * A {@link ServiceRegistryAccess} implementation that registers and acquires services at the OSGi service registry.
 * 
 * @author Robert Mischke
 */
public class OsgiServiceRegistryAccess implements ServiceRegistryPublisherAccess {

    private final BundleContext bundleContext;

    private final List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();

    private final List<ServiceReference<?>> serviceReferences = new ArrayList<ServiceReference<?>>();

    public OsgiServiceRegistryAccess(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        synchronized (serviceReferences) {
            ServiceReference<?> serviceReference = bundleContext.getServiceReference(clazz.getName());
            if (serviceReference == null) {
                throw new IllegalStateException("Found no registered service for class " + clazz.getName());
            }
            // FIXME review @7.0.0: this looks like a memory leak after the change to a "stateless" read access API;
            // distinguish between static/dynamic services regarding disposal and caching? - misc_ro
            serviceReferences.add(serviceReference);
            return (T) bundleContext.getService(serviceReference);
        }
    }

    /**
     * Registers an implementation for a service interface.
     * 
     * @param <T> the interface class to register for
     * @param clazz the interface class to register for
     * @param implementation the implementation to register
     */
    @Override
    public <T> void registerService(Class<T> clazz, Object implementation) {
        synchronized (serviceRegistrations) {
            serviceRegistrations.add(bundleContext.registerService(clazz.getName(), implementation, null));
        }
    }

    @Override
    public void dispose() {
        synchronized (serviceReferences) {
            for (ServiceReference<?> serviceReference : serviceReferences) {
                bundleContext.ungetService(serviceReference);
            }
            serviceReferences.clear();
        }
        synchronized (serviceRegistrations) {
            for (ServiceRegistration<?> registration : serviceRegistrations) {
                registration.unregister();
            }
            serviceRegistrations.clear();
        }
    }

}
