/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.spi.module;

import de.rcenvironment.toolkit.core.api.ImmutableServiceRegistry;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;

/**
 * An abstraction of dependency injection libraries/frameworks. Service implementations are added declaratively, and the actual
 * instantiation and dependency injection is performed after all contributors (in this case, modules) have added their parts.
 * 
 * The currently supported DI mode (using the PicoContainer library) is constructor injection without requiring any annotations in the
 * service implementations; in the future, adding annotations like javax.inject.Inject may become necessary.
 * 
 * @author Robert Mischke
 * 
 */
public interface ObjectGraph {

    /**
     * Sets a {@link ClassFilter} to determine which of a service implementation's interfaces should be considered a "public" interface, ie
     * exported. Interfaces not matched by this filter can be used for dependency injection within the scope of the {@link Toolkit}, but are
     * not accessible from the outside.
     * 
     * Note that this filter is typically reset before an {@link ObjectGraph} is passed to a new module.
     * 
     * @param filter the filter to set
     */
    void setPublicInterfaceFilter(ClassFilter filter);

    /**
     * @param object an object to register as-is within the object graph; it can be used for dependency injection in service implementations
     */
    void registerObject(Object object);

    /**
     * @param implementationClass a class implementing one or more service interfaces; these are automatically detected, and will be
     *        exported/published if they match the most recently set {@link ClassFilter}. It is valid to register internal services, ie one
     *        not providing any exportable/public interfaces.
     */
    void registerServiceClass(Class<?> implementationClass);

    /**
     * Attempts to construct the actual object graph from all previous registrations.
     * 
     * @return the registry of published/exported services
     * @throws ToolkitException on instantiation failure, for example if a service implementation is missing a required dependency
     */
    ImmutableServiceRegistry instantiate() throws ToolkitException;

    /**
     * @param serviceInterface the service interface to check
     * @return true if no service implementation is providing this interface yet; intended for dependency probing
     */
    boolean isMissingService(Class<?> serviceInterface);

}
