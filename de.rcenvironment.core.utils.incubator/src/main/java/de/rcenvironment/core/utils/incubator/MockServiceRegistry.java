/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link ServiceRegistryAccessFactory} implementation for unit tests.
 * 
 * Typical usage in test setups:<code><pre>
    ServiceRegistry.setAccessFactory(new MockServiceRegistry());
    ServiceRegistry.createPublisherAccessFor(this).registerService(&lt;service interface>, &lt;implementation>);
 * </pre></code>
 * 
 * @author Robert Mischke
 */
public class MockServiceRegistry implements ServiceRegistryAccessFactory {

    private final Map<Class<?>, Object> services = new HashMap<>();

    private final AccessAdapter adapter = new AccessAdapter();

    /**
     * Internal {@link ServiceRegistryPublisherAccess} implementation.
     * 
     * @author Robert Mischke
     */
    private final class AccessAdapter implements ServiceRegistryPublisherAccess {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(Class<T> clazz) {
            return (T) services.get(clazz);
        }

        @Override
        public <T> void registerService(Class<T> clazz, Object implementation) {
            services.put(clazz, implementation);
        }

        @Override
        public void dispose() {}

    }

    @Override
    public ServiceRegistryAccess createAccessFor(Object caller) {
        return adapter;
    }

    @Override
    public ServiceRegistryPublisherAccess createPublisherAccessFor(Object caller) {
        return adapter;
    }

}
