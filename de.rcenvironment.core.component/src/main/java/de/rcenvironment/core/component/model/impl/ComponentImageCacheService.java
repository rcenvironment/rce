/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.model.impl;

import de.rcenvironment.core.component.model.impl.ComponentImageManagerImpl.IconSize;

/**
 * The ComponentImageCache is used to save image date of components persistent on the local storage.
 * 
 * @author Dominik Schneider
 *
 */
public interface ComponentImageCacheService {

    /**
     * Returns the raw data of a cached component icon.
     * 
     * @param iconHash of the requested icon
     * @param size of the requested icon
     * @return raw data of the requested icon
     */
    byte[] getImageData(String iconHash, IconSize size);
    
    
    /**
     * Returns the icon hash of the specified component if it has been saved before.
     * @param componentId Id of the component of the requested icon hash
     * @return the icon hash of the specified component
     */
    String getIconHash(String componentId);

}
