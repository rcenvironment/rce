/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentDescriptionFactoryService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;

/**
 * Default stub for {@link ComponentDescriptionFactoryService}.
 * 
 * @author Doreen Seider
 */
public class ComponentDescriptionFactoryServiceDefaultStub implements ComponentDescriptionFactoryService {

    @Override
    public ComponentDescription createComponentDescription(ComponentInstallation componentInstallation) {
        return new ComponentDescription(componentInstallation);
    }

}
