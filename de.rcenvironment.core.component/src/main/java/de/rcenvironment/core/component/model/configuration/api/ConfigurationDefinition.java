/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.List;
import java.util.Set;

/**
 * Describes the component's configuration options.
 *
 * @author Doreen Seider
 * 
 * Note: {@link ConfigurationDefinition} describes the configuration of a component as a kind of blueprint. Each
 * {@link ComponentDescription} then has an actual implementation of it (with actual configuration values, etc.) in the form of a
 * {@link ConfigurationDescription}. --seid_do
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

    /**
     * @return raw configuration definition.
     */
    List<Object> getRawConfigurationDefinition();

    
    /**
     * @return raw configuration metadata definition.
     */
    List<Object> getRawConfigurationMetaDataDefinition();

}
