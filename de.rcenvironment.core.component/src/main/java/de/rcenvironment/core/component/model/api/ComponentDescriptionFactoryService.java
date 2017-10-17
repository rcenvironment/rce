/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.api;

/**
 * Factory service for {@link ComponentDescription} instances.
 * 
 * @author Doreen Seider
 */
public interface ComponentDescriptionFactoryService {

    /**
     * Creates {@link ComponentDescription} instance.
     * 
     * @param componentInstallation {@link ComponentInstallation} the {@link ComponentDescription} instance is related to
     * @return {@link ComponentDescription} instance
     */
    ComponentDescription createComponentDescription(ComponentInstallation componentInstallation);
}
