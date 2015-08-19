/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.Map;
import java.util.Set;

/**
 * Describes the component's read-only configuration.
 *
 * @author Doreen Seider
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
