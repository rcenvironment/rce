/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;

/**
 * Simple {@link ImmutableServiceRegistry} implementation. The {@link Map} provided to the constructor is copied internally, so it can be
 * safely modified or discarded after the constructor finishes.
 * 
 * @author Robert Mischke
 */
public class ImmutableServiceRegistryImpl implements ImmutableServiceRegistry {

    private final Map<Class<?>, Object> services;

    /**
     * @param serviceSourceMap a map of services; this map is copied internally, so it can be safely modified or discarded after the
     *        constructor finishes.
     */
    public ImmutableServiceRegistryImpl(Map<Class<?>, Object> serviceSourceMap) {
        services = Collections.unmodifiableMap(new HashMap<>(serviceSourceMap));
    }

    @Override
    public Collection<Class<?>> listServices() {
        return services.keySet();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> apiClass) {
        return (T) services.get(apiClass);
    }

}
