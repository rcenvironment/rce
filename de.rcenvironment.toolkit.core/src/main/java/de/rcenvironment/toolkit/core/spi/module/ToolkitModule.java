/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

import java.util.Set;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.setup.ToolkitSetup;

/**
 * A {@link Toolkit} module defining a set of services.
 * 
 * Within the {@link #registerMembers(ObjectGraph)} method, the module specifies its service implementations and (optionally) other objects.
 * An {@link ObjectGraph} is then created after all modules have registered their services. Decoupling registration and graph building has
 * the advantage of not requiring a specific order between module or service declarations.
 * 
 * @author Robert Mischke
 * 
 * @param <TModuleConf>
 */
public interface ToolkitModule<TModuleConf> {

    /**
     * Typically called from a {@link ToolkitSetup}, this method instructs the module to register its services with the provided
     * {@link ObjectGraph}. It is also providing the (optional) configuration object for this module.
     * 
     * Configuration objects can either be added to the {@link ObjectGraph} so that service implementations can consume it via dependency
     * injection, or its values can be interpreted to affect the graph setup itself. For example, a parameter in the configuration object
     * may determine which concrete service implementation is registered for a given service interface.
     * 
     * @param objectGraph the object graph to register services with
     */
    void registerMembers(ObjectGraph objectGraph);

    /**
     * Creates a default configuration object of the appropriate type. If the configuration type is Void, null should be returned.
     * 
     * @return a new default configuration object of type TModuleConf
     */
    TModuleConf createConfigurationObject();

    /**
     * Allows a module to suggest modules that provide services not found in the current {@link ObjectGraph} yet.
     * 
     * This "on-demand" approach avoids hard-coded module dependencies, and allows for service substitution outside of the standard module.
     * For example, if module M1 requires a service implementation of interface A, which is typically provided by module M2, this approach
     * still allows for another implementation of A to be provided by another source, in which case module M2 would not be suggested for
     * loading.
     * 
     * @param objectGraph the current {@link ObjectGraph}
     * @param modulesToLoad a {@link Set} where suggested module classes can be added to
     */
    void suggestMissingModuleDependencies(ObjectGraph objectGraph, Set<Class<? extends ToolkitModule<?>>> modulesToLoad);

    /**
     * Allows a module to register shutdown hooks, implemented as plain {@link Runnable}s.
     * 
     * @param serviceRegistry the instantiated service registry
     * @param shutdownHookReceiver the receiver to add any shutdown hooks to
     */
    void registerShutdownHooks(ImmutableServiceRegistry serviceRegistry, ShutdownHookReceiver shutdownHookReceiver);

    /**
     * Returns the mutable (but final) configuration object for this module instance. If the configuration type is Void, null is returned.
     * 
     * @return the internal, mutable configuration object of type TModuleConf
     */
    TModuleConf getConfiguration();

}
