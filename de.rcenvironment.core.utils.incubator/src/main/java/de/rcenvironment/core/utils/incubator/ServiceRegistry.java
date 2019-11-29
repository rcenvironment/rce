/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Abstract base class of {@link ServiceRegistryAccess} factories, and static acquisition point for the global default factory instance.
 * 
 * @author Robert Mischke
 */
public abstract class ServiceRegistry {

    private static volatile ServiceRegistryAccessFactory factory = null;

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
    public static ServiceRegistryAccess createAccessFor(Object caller) {
        if (factory == null) {
            throw new IllegalStateException(
                "No default ServiceRegistryAccessFactory yet (possible cause: are you using this from a 'core' bundle?)");
        }
        return factory.createAccessFor(caller);
    }

    /**
     * Creates a {@link ServiceRegistryPublisherAccess} for a given caller, which can be used to publish temporary service instances. The
     * caller is responsible for calling {@link ServiceRegistryAccess#dispose()} before shutdown, or when the temporary service instances
     * should be unregistered.
     * <p>
     * For convenience, the {@link ServiceRegistryPublisherAccess} also implements the {@link ServiceRegistryAccess} interface. There are no
     * additional restrictions to services fetched from {@link ServiceRegistryAccess#getService()}. In particular, these instances can still
     * be used after {@link ServiceRegistryPublisherAccess#dispose()} has been called on the creating instance.
     * 
     * @param caller the caller object; used to locate undisposed access instances
     * @return the new {@link ServiceRegistryAccess}
     */
    public static ServiceRegistryPublisherAccess createPublisherAccessFor(Object caller) {
        if (factory == null) {
            throw new IllegalStateException(
                "No default ServiceRegistryAccessFactory yet (possible cause: are you using this from a 'core' bundle?)");
        }
        return factory.createPublisherAccessFor(caller);
    }

    public static void setAccessFactory(ServiceRegistryAccessFactory newFactory) {
        ServiceRegistry.factory = newFactory;
    }

}
