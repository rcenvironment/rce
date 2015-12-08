/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Mock implementation of {@link AdditionalServicesRegistrationService} to simulate/handle listener registration in integration tests.
 * 
 * @author Robert Mischke
 */
public class MockAdditionalServicesRegistrationService implements AdditionalServicesRegistrationService {

    private Set<AdditionalServiceDeclaration> serviceDeclarations = new HashSet<AdditionalServiceDeclaration>();

    @Override
    public void registerAdditionalServicesProvider(AdditionalServicesProvider listenerProvider) {
        for (AdditionalServiceDeclaration declaration : listenerProvider.defineAdditionalServices()) {
            serviceDeclarations.add(declaration);
        }
    }

    @Override
    public void unregisterAdditionalServicesProvider(AdditionalServicesProvider listenerProvider) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Fetches all registered listeners for a given listener interface.
     * 
     * @param <T> the class of the listener interface
     * @param requestedClass the class of the listener interface
     * @return a all listener implementations registered for the given interface
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getListeners(Class<T> requestedClass) {
        Collection<T> listeners = new ArrayList<T>();
        for (AdditionalServiceDeclaration declaration : serviceDeclarations) {
            if (requestedClass == declaration.getServiceClass()) {
                listeners.add((T) declaration.getImplementation());
            }
        }
        return listeners;
    }
}
