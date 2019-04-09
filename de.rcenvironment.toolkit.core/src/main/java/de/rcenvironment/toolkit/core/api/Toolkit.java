/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.core.api;

import de.rcenvironment.toolkit.core.setup.ToolkitFactory;

/**
 * The initial interface for accessing and controlling a {@link Toolkit} instance.
 * 
 * The main purpose of the RCE Toolkit is to provide a consistent set of services within a JVM. It is currently used to provide reusable
 * services for distributed applications, but could be used to provide all kinds of services. It is explicitly possible to use multiple
 * {@link Toolkit} instances within the same JVM without interference.
 * 
 * The basic building blocks of a {@link Toolkit} setup are modules, which define a subset of services, and the {@link ToolkitFactory} which
 * provides dependency injection support for the common task of consuming services as part of implementing another service.
 * 
 * @author Robert Mischke
 */
public interface Toolkit {

    /**
     * @return the registry for accessing the set/map of services
     */
    ImmutableServiceRegistry getServiceRegistry();

    /**
     * Not used yet, but intended to provide shutdown hooks to services (e.g. for gracefully closing network ports or connections).
     */
    void shutdown();

    /**
     * Convenience shortcut for fetching service instances. Returns null for unavailable or unknown services.
     * 
     * @param <T> the class of the service API to fetch
     * @param serviceClass the class of the service API to fetch
     * @return a service implementation, or null if none is available
     */
    <T> T getService(Class<T> serviceClass);

}
