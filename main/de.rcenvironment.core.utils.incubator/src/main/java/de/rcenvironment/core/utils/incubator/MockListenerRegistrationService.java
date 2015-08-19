/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Mock implementation of {@link ListenerRegistrationService} to simulate/handle listener
 * registration in integration tests.
 * 
 * @author Robert Mischke
 */
public class MockListenerRegistrationService implements ListenerRegistrationService {

    private Set<ListenerDeclaration> listenerDeclarations = new HashSet<ListenerDeclaration>();

    @Override
    public void registerListenerProvider(ListenerProvider listenerProvider) {
        for (ListenerDeclaration declaration : listenerProvider.defineListeners()) {
            listenerDeclarations.add(declaration);
        }
    }

    @Override
    public void unregisterListenerProvider(ListenerProvider listenerProvider) {
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
        for (ListenerDeclaration declaration : listenerDeclarations) {
            if (requestedClass == declaration.getServiceClass()) {
                listeners.add((T) declaration.getImplementation());
            }
        }
        return listeners;
    }
}
