/*
 * Copyright 2006-2020 DLR, Germany
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
public interface ServiceRegistryAccess {

    /**
     * Acquires a service instance of the given type.
     * 
     * @param <T> the service type to acquire
     * @param clazz the class of the service type to acquire; required for generics resolution
     * @return the service instance
     */
    <T> T getService(Class<T> clazz);
}
