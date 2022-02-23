/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
