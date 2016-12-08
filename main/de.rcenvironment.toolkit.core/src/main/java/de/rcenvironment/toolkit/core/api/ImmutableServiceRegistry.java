/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.api;

import java.util.Collection;

/**
 * An immutable holder for a mapping of service interface to implementing instances.
 * 
 * @author Robert Mischke
 */
public interface ImmutableServiceRegistry {

    /**
     * @return all service interfaces
     */
    Collection<Class<?>> listServices();

    /**
     * @param <T> the service interface class
     * @param apiClass the service interface class
     * @return the associated instance
     */
    <T> T getService(Class<T> apiClass);
}
