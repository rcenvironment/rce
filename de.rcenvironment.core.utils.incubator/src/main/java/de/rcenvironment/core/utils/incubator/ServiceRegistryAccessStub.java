/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test stub for {@link ServiceRegistryAccess}; can be both used as a simple stub that returns null for all service requests, or as an
 * actual provider of test service instances.
 *
 * @author Robert Mischke
 */
public class ServiceRegistryAccessStub implements ServiceRegistryAccess {

    private final boolean throwExceptionInsteadOfNullService;

    private final Map<Class<?>, Object> serviceMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * @param throwExceptionOnMissingService if true, a {@link #getService()} call for a non-existing service will trigger a
     *        {@link NullPointerException} being thrown instead of returning null; use this to "fail fast" in a test if this stub is
     *        actually supposed to provide all expected services
     */
    public ServiceRegistryAccessStub(boolean throwExceptionOnMissingService) {
        this.throwExceptionInsteadOfNullService = throwExceptionOnMissingService;
    }

    /**
     * Registers a service instance, typically as part of a test setup.
     * 
     * @param <T> the class to register the service for
     * @param clazz the class to register the service for
     * @param instance the service instance
     */
    public <T> void put(Class<T> clazz, T instance) {
        serviceMap.put(clazz, instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        final T result = (T) serviceMap.get(clazz);
        if (result == null && throwExceptionInsteadOfNullService) {
            throw new NullPointerException("No service registered for interface " + clazz.getSimpleName());
        }
        return result;
    }

}
