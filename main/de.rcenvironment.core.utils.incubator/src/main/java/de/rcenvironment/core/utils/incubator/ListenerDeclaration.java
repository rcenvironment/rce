/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

/**
 * Defines a listener that a service would like to register.
 * 
 * @author Robert Mischke
 */
public class ListenerDeclaration {

    private final Class<?> serviceClass;

    private final Object implementation;

    public <T> ListenerDeclaration(Class<T> serviceClass, T implementation) {
        this.serviceClass = serviceClass;
        this.implementation = implementation;
    }

    /**
     * @return the listener service interface
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * @return the listener implementation; must be an implementation of {@link #getServiceClass()}
     */
    public Object getImplementation() {
        return implementation;
    }

}
