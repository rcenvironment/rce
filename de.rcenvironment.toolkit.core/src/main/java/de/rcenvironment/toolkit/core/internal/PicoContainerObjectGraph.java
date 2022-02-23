/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.behaviors.Caching;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.ToolkitException;

/**
 * An {@link AbstractObjectGraph} implementation based on the PicoContainer library.
 * 
 * @author Robert Mischke
 */
public class PicoContainerObjectGraph extends AbstractObjectGraph {

    private DefaultPicoContainer container;

    private Map<Class<?>, Object> services = new HashMap<>();

    public PicoContainerObjectGraph() {
        container = new DefaultPicoContainer(new Caching()); // container with object caching behavior
    }

    @Override
    protected void registerObject(Object object, Collection<Class<?>> privateInterfaces) {
        container.addComponent(object);
    }

    @Override
    protected void registerServiceClass(Object implementationClass, Collection<Class<?>> publicInterfaces,
        Collection<Class<?>> privateInterfaces) {
        // optimistically let PicoContainer handle the internal interface mapping selection for now, ie no explicit filtering;
        // adjust as necessary
        container.addComponent(implementationClass);
        // logDebug("Registering service implementation class " + implementationClass);
        for (Class<?> i : publicInterfaces) {
            // TODO verify uniqueness, fail on overlap
            // logDebug("Registering interface to publish: " + i);
            services.put(i, null);
        }
    }

    @Override
    public boolean isMissingService(Class<?> serviceInterface) {
        return !services.containsKey(serviceInterface);
    }

    @Override
    protected ImmutableServiceRegistry instantiateServices(Collection<Class<?>> serviceInterfaces) throws ToolkitException {
        try {
            for (Class<?> i : services.keySet()) {
                Object impl = container.getComponent(i);
                services.put(i, impl);
                logDebug("Publishing service " + i.getName() + " implemented by " + impl);
            }
            return new ImmutableServiceRegistryImpl(services);
        } catch (RuntimeException e) {
            if (e.getClass().getName().contains("UnsatisfiableDependenciesException")) {
                // compress known stacktrace; this also keeps PicoContainer classes hidden from user
                throw new ToolkitException("Error constructing the service object graph: " + e.getMessage());
            } else {
                throw new ToolkitException("Error constructing the service object graph", e);
            }
        }
    }

}
