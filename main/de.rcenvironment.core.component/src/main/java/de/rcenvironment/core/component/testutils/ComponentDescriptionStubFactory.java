/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.component.model.api.ComponentDescription;

/**
 * Create {@link ComponentDescription} objects.
 * 
 * @author Doreen Seider
 */
public final class ComponentDescriptionStubFactory {

    private ComponentDescriptionStubFactory() {}
    
    /**
     * @return {@link ComponentDescription} with default behavior
     */
    public static ComponentDescription createComponentDescriptionDefaultStub() {
        
        return new ComponentDescription(ComponentInstallationStubFactory.createComponentInstallationStub());
    }
}
