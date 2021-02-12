/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
