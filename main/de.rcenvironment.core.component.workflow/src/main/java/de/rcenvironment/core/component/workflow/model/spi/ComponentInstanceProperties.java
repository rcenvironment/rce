/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.model.spi;

import java.beans.PropertyChangeListener;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;

/**
 * Provides read-write access to the configuration-time setup of component instances.
 * 
 * @author Robert Mischke
 * @author Doreen Seider
 */
public interface ComponentInstanceProperties {

    /**
     * Adds the given {@link PropertyChangeListener}.
     * 
     * @param listener the {@link PropertyChangeListener}
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the given {@link PropertyChangeListener}.
     * 
     * @param listener the {@link PropertyChangeListener}
     */
    void removePropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * @return {@link EndpointDescriptionsManager} for inputs
     */
    EndpointDescriptionsManager getInputDescriptionsManager();
    
    /**
     * @return {@link EndpointDescriptionsManager} for outputs
     */
    EndpointDescriptionsManager getOutputDescriptionsManager();
    
    /**
     * @return {@link ConfigurationDescription}
     */
    ConfigurationDescription getConfigurationDescription();
    
}
