/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.setup.ToolkitSetup;
import de.rcenvironment.toolkit.core.spi.module.DefaultClassFilter;
import de.rcenvironment.toolkit.core.spi.module.ObjectGraph;
import de.rcenvironment.toolkit.core.spi.module.ShutdownHookReceiver;
import de.rcenvironment.toolkit.core.spi.module.ToolkitModule;

/**
 * Default {@link ToolkitSetup} implementation.
 * 
 * @author Robert Mischke
 */
public final class ToolkitBuilderImpl implements ToolkitSetup, ShutdownHookReceiver {

    private static final int MAX_DEPENDENCY_RESOLUTION_ATTEMPTS = 10;

    private final ObjectGraph objectGraph;

    private final DefaultClassFilter defaultClassFilter = new DefaultClassFilter();

    private final Map<Class<?>, ToolkitModule<?>> moduleInstances = new HashMap<>();

    private final List<Runnable> shutdownHooks = new ArrayList<>();

    public ToolkitBuilderImpl(ObjectGraph objectGraphBuilder) {
        this.objectGraph = objectGraphBuilder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ToolkitModule<TModuleConf>, TModuleConf> TModuleConf configureModule(Class<T> module) throws ToolkitException {
        final T moduleInstance;
        // ignore duplicate registrations; in that case, reuse the existing module and its configuration object
        if (!moduleInstances.containsKey(module)) {
            try {
                moduleInstance = module.newInstance();
                moduleInstances.put(module, moduleInstance);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ToolkitException(e);
            }
        } else {
            moduleInstance = (T) moduleInstances.get(module);
        }

        return moduleInstance.getConfiguration();
    }

    /**
     * Attempts to initialize all modules and construct the set of services.
     * 
     * @return the assembled {@link Toolkit}
     * @throws ToolkitException on instantiation errors
     */
    public Toolkit create() throws ToolkitException {
        for (Entry<Class<?>, ToolkitModule<?>> e : moduleInstances.entrySet()) {
            final ToolkitModule<?> module = e.getValue();
            registerModule(module);
        }

        for (int i = 0; i < MAX_DEPENDENCY_RESOLUTION_ATTEMPTS; i++) {
            Set<Class<? extends ToolkitModule<?>>> missingModulesDependencies = new HashSet<>();
            for (Entry<Class<?>, ToolkitModule<?>> e : moduleInstances.entrySet()) {
                final ToolkitModule<?> module = e.getValue();
                module.suggestMissingModuleDependencies(objectGraph, missingModulesDependencies);
            }

            if (missingModulesDependencies.isEmpty()) {
                break; // all ok, leave the retry loop
            }

            LogFactory.getLog(getClass()).debug(
                "Identified missing service dependencies, loading suggested default module(s) " + missingModulesDependencies);
            for (Class<? extends ToolkitModule<?>> moduleClass : missingModulesDependencies) {
                try {
                    registerModule(moduleClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new ToolkitException(e);
                }
            }
        }

        final ImmutableServiceRegistry serviceRegistry = objectGraph.instantiate();

        for (ToolkitModule<?> module : moduleInstances.values()) {
            module.registerShutdownHooks(serviceRegistry, this);
        }

        return new ToolkitImpl(serviceRegistry, Collections.unmodifiableList(shutdownHooks));
    }

    @Override
    public void addShutdownHook(Runnable shutdownHook) {
        shutdownHooks.add(shutdownHook);
    }

    private void registerModule(final ToolkitModule<?> module) {
        objectGraph.setPublicInterfaceFilter(defaultClassFilter); // reset filter for each module
        module.registerMembers(objectGraph);
    }

}
