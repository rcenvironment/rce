/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.registration.api;

import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Registry for {@link Component}s.
 * 
 * @author Roland Gude
 * @author Jens Ruehmkorf
 * @author Doreen Seider
 */
public interface ComponentRegistry {

    /**
     * Adds a component.
     * 
     * @param componentInstallation {@link ComponentInstallation} to add
     */
    void addComponent(ComponentInstallation componentInstallation);
    
    /**
     * Removes a component.
     * 
     * @param compInstallationId identifier of {@link ComponentInstallation} to remove
     */
    void removeComponent(String compInstallationId);
    
    /**
     * Add a token which authorizes to create a new component instance.
     * 
     * @param token given authorization token (created at workflow controller instance creation)
     */
    void addComponentInstantiationAuthToken(String token);
}
