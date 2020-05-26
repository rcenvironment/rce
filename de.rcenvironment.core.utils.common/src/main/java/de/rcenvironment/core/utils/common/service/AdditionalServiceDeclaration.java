/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.service;

/**
 * Defines a service interface/implementation pair that a {@link AdditionalServicesProvider} instance would like to register.
 * 
 * @author Robert Mischke
 */
public class AdditionalServiceDeclaration {

    private final Class<?> serviceClass;

    private final Object implementation;

    public <T> AdditionalServiceDeclaration(Class<T> serviceClass, T implementation) {
        this.serviceClass = serviceClass;
        this.implementation = implementation;
    }

    /**
     * @return the additional service's interface
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * @return the additional service's implementation; must be an implementation of {@link #getServiceClass()}
     */
    public Object getImplementation() {
        return implementation;
    }

}
