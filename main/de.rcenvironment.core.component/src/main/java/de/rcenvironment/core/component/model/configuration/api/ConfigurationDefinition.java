/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.Set;

/**
 * Describes the component's configuration options.
 *
 * @author Doreen Seider
 */
public interface ConfigurationDefinition {

    /**
     * @return all pre-defined configuration keys
     */
    Set<String> getConfigurationKeys();

    /**
     * @param key configuration key to get default value for
     * @return default value or <code>null</code> if no one is defined
     */
    String getDefaultValue(String key);
    
    /**
     * @return read-only configuration of the component
     */
    ReadOnlyConfiguration getReadOnlyConfiguration();
    
    /**
     * @return related placeholder meta data definitions
     */
    PlaceholdersMetaDataDefinition getPlaceholderMetaDataDefinition();
    
    /**
     * @return related configuration meta data definitions
     */
    ConfigurationMetaDataDefinition getConfigurationMetaDataDefinition();

}
