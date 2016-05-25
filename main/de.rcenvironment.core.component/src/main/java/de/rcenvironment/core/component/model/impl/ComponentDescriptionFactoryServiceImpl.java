/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.impl;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentDescriptionFactoryService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Default implementation of {@link ComponentDescriptionFactoryService}.
 * 
 * @author Doreen Seider
 */
public class ComponentDescriptionFactoryServiceImpl implements ComponentDescriptionFactoryService {

    @Override
    public ComponentDescription createComponentDescription(ComponentInstallation componentInstallation) {
        return new ComponentDescription(componentInstallation);
    }

}
