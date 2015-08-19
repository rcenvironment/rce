/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.Map;

/**
 * Describes extended configuration options of components as they can be contributed by fragments.
 * 
 * @author Doreen Seider
 */
public interface ConfigurationExtensionDefinition extends ConfigurationDefinition {

    /**
     * @param configuration current configuration
     * @return <code>true</code> if configuration extensions are active and must be considered, otherwise <code>false</code>
     */
    boolean isActive(Map<String, String> configuration);

}
