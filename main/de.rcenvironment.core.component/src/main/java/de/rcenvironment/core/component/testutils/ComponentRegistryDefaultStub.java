/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.registration.api.ComponentRegistry;

/**
 * Default stub implementation of {@link ComponentRegistry}.
 * 
 * @author Doreen Seider
 */
public class ComponentRegistryDefaultStub implements ComponentRegistry {

    @Override
    public void addComponent(ComponentInstallation componentInstallation) {
    }

    @Override
    public void removeComponent(String compInstallationId) {
    }

    @Override
    public void addComponentInstantiationAuthToken(String token) {
    }

}
