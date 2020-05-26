/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.component.model.impl.ComponentImageManagerImpl.ImagePackage;

/**
 * Creates shared SWT images of component icons collected in an ImagePackage.
 * 
 * @author Dominik Schneider
 */
public interface ComponentImageManagerService {

    /**
     * Returns the image package of all icons of the specified component.
     * 
     * @param componentId The id of the component.
     * @return The image package containing all icon sizes of the component icon.
     */
    ImagePackage getImagePackage(String componentId);
}
