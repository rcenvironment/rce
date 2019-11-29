/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.Map;
import java.util.Set;

/**
 * Describes the component's read-only configuration.
 *
 * @author Doreen Seider
 * 
 * Note: Mainly used for integrated tools. The values stored in the configuration.json file of an integrated tools goes into the
 * {@link ConfigurationDefinition} (and {@link ConfigurationDescription}) as read-only configuration values. --seid_do
 */
public interface ReadOnlyConfiguration {

    /**
     * @return all pre-defined configuration keys
     */
    Set<String> getConfigurationKeys();

    /**
     * @param key configuration key to get default value for
     * @return default value or <code>null</code> if no one is defined
     */
    String getValue(String key);
    
    /**
     * @return configuration map
     */
    Map<String, String> getConfiguration();

}
