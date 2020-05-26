/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.testutils;

import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;

/**
 * Represents a simulated communication bundle that provides consistently configured service instances. Intended for use in integration
 * tests.
 * 
 * @author Robert Mischke
 */
public interface VirtualCommunicationBundle {

    /**
     * Simulates the activation of the virtual bundle. Internally, this triggers "activate()" calls on the individual OSGi-DS components,
     * similar to the actual lifecycle.
     */
    void activate();

    /**
     * Allows unit or integration tests to prevent {@link #startUpNetwork()} from being called automatically as part of the
     * {@link #activate()} method.
     * 
     * @param autoStartNetworkOnActivation the new value; default is "true"
     */
    void setAutoStartNetworkOnActivation(boolean autoStartNetworkOnActivation);

    /**
     * Returns an activated service instance that was bound to the given service interface.
     * 
     * @param <T> the service interface class
     * @param clazz the service interface class
     * @return the service instance, or null if none exists
     */
    <T> T getService(Class<T> clazz);

    /**
     * Allows injection of service instances; useful for remote service call testing.
     * 
     * @param <T> the service interface class
     * @param clazz the service interface class
     * @param implementation the service instance
     */
    <T> void injectService(Class<T> clazz, T implementation);

    /**
     * Convenience method to register a {@link NetworkTransportProvider}.
     * 
     * @param newProvider the {@link NetworkTransportProvider} instance to register
     */
    void registerNetworkTransportProvider(NetworkTransportProvider newProvider);

}
