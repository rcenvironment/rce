/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.api;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.toolkitbridge.internal.DefaultToolkitConfiguration;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.setup.ToolkitFactory;

/**
 * @author Robert Mischke
 */
public final class StaticToolkitHolder {

    private static Toolkit instance; // access synchronized via "sharedInstanceLock"

    private static final Object sharedInstanceLock = StaticToolkitHolder.class;

    private StaticToolkitHolder() {}

    private static Toolkit getInstance() {
        synchronized (sharedInstanceLock) {
            if (instance == null) {
                throw new IllegalStateException(
                    "No toolkit instance available - most likely, the code tried to access a service before the toolkit "
                        + "was initialized or after it has been shut down");
            }
            return instance;
        }
    }

    /**
     * Injects a {@link Toolkit} instance. If there already is an instance, an exception is thrown. Setting "null" is allowed to remove an
     * existing instance.
     * 
     * @param newinstance the new instance to set, or null to remove any existing instance
     */
    public static void setInstance(Toolkit newinstance) {
        synchronized (sharedInstanceLock) {
            if (newinstance != null && instance != null) {
                throw new IllegalStateException("Duplicate toolkit initialization");
            }
            instance = newinstance;
        }
    }

    /**
     * @param <T> the interface class of the requested service
     * @param serviceClass the interface class of the requested service
     * @return the service instance for the given interface; if no such service exists, an {@link IllegalStateException} is thrown
     */
    public static <T> T getService(Class<T> serviceClass) {
        T service = getInstance().getServiceRegistry().getService(serviceClass);
        if (service == null) {
            throw new IllegalStateException("The registered toolkit does not contain a service for interface " + serviceClass);
        }
        return service;
    }

    /**
     * A variant of {@link #getService(Class)} that allows explicit fallback handling in contexts where no toolkit is available (e.g. static
     * delegate classes being used by unit tests).
     * 
     * @param <T> the interface class of the requested service
     * @param serviceClass the interface class of the requested service
     * @return the service instance for the given interface; if no toolkit is available, this method returns null; it a toolkit is
     *         available, but no such service exists, an {@link IllegalStateException} is thrown
     */
    public static <T> T getServiceIfInitialized(Class<T> serviceClass) {
        synchronized (sharedInstanceLock) {
            if (instance != null) {
                return getService(serviceClass);
            } else {
                return null;
            }
        }
    }

    /**
     * A variant of {@link #getService(Class)} that creates an implicit {@link Toolkit} instance if none has been initialized before.
     * 
     * @param <T> the interface class of the requested service
     * @param serviceClass the interface class of the requested service
     * @return the service instance for the given interface; if no such service exists, an {@link IllegalStateException} is thrown
     */
    public static <T> T getServiceWithUnitTestFallback(Class<T> serviceClass) {
        synchronized (sharedInstanceLock) {
            if (instance == null) {
                instance = createImplicitToolkit();
            }
        }
        return getService(serviceClass);
    }

    private static Toolkit createImplicitToolkit() {
        LogFactory.getLog(StaticToolkitHolder.class).info("Creating an implicit toolkit instance (usually as part of a unit test)");
        try {
            return ToolkitFactory.create(new DefaultToolkitConfiguration());
        } catch (ToolkitException e) {
            throw new IllegalStateException("Error creating implicit toolkit instance", e);
        }
    }
}
