/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;

/**
 * Provides static methods for accessing services provided by the RCE {@link Toolkit}.
 * 
 * @author Robert Mischke
 */
public final class ToolkitBridge {

    private static final Object sharedInitializationLock = ToolkitBridge.class;

    private static Toolkit toolkitInstance; // access synchronized via "sharedInitializationLock"

    private static ToolkitBridge osgiBridgeInstance; // access synchronized via "sharedInitializationLock"

    private static List<Runnable> deferredRunnables; // access synchronized via "sharedInitializationLock"

    private final ImmutableServiceRegistry toolkitServiceRegistry;

    private final BundleContext bundleContext;

    private final List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();

    private final Log log = LogFactory.getLog(getClass());

    private ToolkitBridge(Toolkit toolkit, BundleContext bundleContext) {
        this.toolkitServiceRegistry = toolkit.getServiceRegistry();
        this.bundleContext = bundleContext;

        if (bundleContext == null) {
            throw new IllegalArgumentException("Creating a ToolkitBridge instance without an OSGi BundleContext is not allowed");
        }
    }

    /**
     * Initialized this bridge with the given {@link Toolkit} instance. Usually called from a bundle activator.
     * 
     * @param toolkit the toolkit to delegate services from
     * @param bundleContext the OSGi {@link BundleContext}
     */
    public static void initialize(Toolkit toolkit, BundleContext bundleContext) {
        synchronized (sharedInitializationLock) {

            toolkitInstance = toolkit;
            StaticToolkitHolder.setInstance(toolkit);

            osgiBridgeInstance = new ToolkitBridge(toolkit, bundleContext);
            osgiBridgeInstance.registerOsgiServices();

            if (deferredRunnables != null) {
                LogFactory.getLog(ToolkitBridge.class).debug(
                    "Running " + deferredRunnables.size() + " deferred task(s) after toolkit initialization");
                for (Runnable runnable : deferredRunnables) {
                    // note: not catching RTEs for now - if a Runnable does not behave, let it crash the startup
                    runnable.run();
                }
                deferredRunnables = null;
            }
        }
    }

    /**
     * Performs cleanup (if necessary) and removes the registered {@link Toolkit} instance.
     */
    public static void dispose() {
        synchronized (sharedInitializationLock) {
            try {
                osgiBridgeInstance.unregisterOsgiServices();
                toolkitInstance.shutdown();
                toolkitInstance = null;
            } finally {
                osgiBridgeInstance = null;
                StaticToolkitHolder.setInstance(null);
            }
        }
    }

    /**
     * This method provides a call mechanism for cases when the timing between caller and the toolkit bridge is not certain. If the toolkit
     * is already available at the time of this call, the {@link Runnable} is immediately executed within the caller's thread. If it is not
     * available yet, it will be executed right after the toolkit becomes available. The caller should make no assumptions about the thread
     * used to execute the provided {@link Runnable}.
     * 
     * @param runnable the task to execute immediately or when the toolkit has become available
     */
    public static void afterToolkitAvailable(Runnable runnable) {
        synchronized (sharedInitializationLock) {
            if (osgiBridgeInstance != null) {
                runnable.run();
            } else {
                // lazy init
                if (deferredRunnables == null) {
                    deferredRunnables = new ArrayList<>();
                }
                deferredRunnables.add(runnable);
            }
        }
    }

    private void registerOsgiServices() {
        // publish all toolkit APIs as OSGi services
        synchronized (serviceRegistrations) {

            for (Class<?> apiClass : toolkitServiceRegistry.listServices()) {
                serviceRegistrations.add(bundleContext.registerService(apiClass.getName(), toolkitServiceRegistry.getService(apiClass),
                    null));
                // note: avoiding StringUtils here, as this would create a bundle dependency cycle
                log.debug("Registered RCE Tookit service " + apiClass.getName() + " as an OSGi service");
            }
        }
    }

    private void unregisterOsgiServices() {
        synchronized (serviceRegistrations) {
            for (ServiceRegistration<?> registration : serviceRegistrations) {
                registration.unregister();
            }
            serviceRegistrations.clear();
        }
        log.debug("Unregistered all toolkit OSGi services");
    }

}
