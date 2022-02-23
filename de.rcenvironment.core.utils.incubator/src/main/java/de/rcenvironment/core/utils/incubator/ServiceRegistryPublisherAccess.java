/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Abstract access to a central service registry, intended for use by a single caller. The caller is
 * responsible for disposing this instance before its own shutdown or destruction.
 * 
 * @author Robert Mischke
 */
public interface ServiceRegistryPublisherAccess extends ServiceRegistryAccess{


    /**
     * Registers a service instance of the given type.
     * 
     * @param <T> the service type to register
     * @param clazz the class of the service type to register; required for generics resolution
     * @param implementation the service implementation
     */
    <T> void registerService(Class<T> clazz, Object implementation);

    /**
     * Releases this {@link ServiceRegistryPublisherAccess}, which will usually cause all acquired services
     * to be released, and all registered service instances to be unregistered.
     */
    void dispose();
}
