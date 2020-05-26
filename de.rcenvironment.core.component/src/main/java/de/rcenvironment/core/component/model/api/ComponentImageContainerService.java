/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.api;

import de.rcenvironment.core.component.model.impl.ComponentImageContainer;

/**
 * This service is used to manage the Component Image Containers. It is important that for each component only one Container is created.
 * This class shares and deletes the containers. It is counted how often a container has been shared. If the counter is decreased to 0, the
 * container has to be disposed.
 * 
 * @author Dominik Schneider
 */
public interface ComponentImageContainerService {

    /**
     * Returns the Component Image Container of a specified component.
     * 
     * @param componentId The ID + VERSION of the component of which the component image container should be returned
     * @return the component image container of the specified component
     */
    ComponentImageContainer getComponentImageContainer(String componentId);

    /**
     * Returns the Component Image Container of a specified component.
     * 
     * @param ci The component interface of the component of which the component image container should be returned
     * @return the component image container of the specified component
     */
    ComponentImageContainer getComponentImageContainer(ComponentInterface ci);

}
