/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Abstract base class of {@link ServiceRegistryAccess} factories, and static acquisition point for the global default factory instance.
 * 
 * @author Robert Mischke
 */
public interface ServiceRegistryAccessFactory {

    /**
     * Creates a read-only {@link ServiceRegistryAccess} for a given caller. There is no need to dispose of the returned access object or
     * the services retrieved from it. In particular, there is no need to keep a reference to the {@link ServiceRegistryAccess} object after
     * fetching services from it.
     * <p>
     * (Note that this usage pattern is very different from how {@link ServiceRegistryPublisherAccess} instances retrieved from
     * {@link #createPublisherAccessFor()} must be used.)
     * 
     * @param caller the caller object
     * @return the new {@link ServiceRegistryAccess}
     */
    ServiceRegistryAccess createAccessFor(Object caller);

    /**
     * Creates a {@link ServiceRegistryPublisherAccess} for a given caller that can be used to publish temporary service instances. The
     * caller is responsible for calling {@link ServiceRegistryPublisherAccess#dispose()} before shutdown, or at the time the temporary
     * service instances should be unregistered.
     * <p>
     * A typical use case is a dynamic GUI element (for example, an RCP dialog or view) that calls this method during creation, registers
     * one or more listener services, and then calls {@link ServiceRegistryPublisherAccess#dispose()} when it is destroyed/disposed. This
     * way, all registered listeners are automatically unregistered.
     * <p>
     * For convenience, the {@link ServiceRegistryPublisherAccess} also implements the {@link ServiceRegistryAccess} interface. There are no
     * additional restrictions to services fetched from {@link ServiceRegistryAccess#getService()}. In particular, these instances can still
     * be used after {@link ServiceRegistryPublisherAccess#dispose()} has been called on the creating instance.
     * 
     * @param caller the caller object; used to locate undisposed access instances
     * @return the new {@link ServiceRegistryAccess}
     */
    ServiceRegistryPublisherAccess createPublisherAccessFor(Object caller);
}
