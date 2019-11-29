/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.util.Map;

/**
 * Describes extended configuration options of components as they can be contributed by fragments.
 * 
 * @author Doreen Seider
 * 
 * Note: The concept of configuration extensions was introduces for the Pyranha optimizer fragments. It is kind of unintuitive and
 * should be reviewed where it is still used and whether it can be improved. --seid_do
 */
public interface ConfigurationExtensionDefinition extends ConfigurationDefinition {

    /**
     * @param configuration current configuration
     * @return <code>true</code> if configuration extensions are active and must be considered, otherwise <code>false</code>
     */
    boolean isActive(Map<String, String> configuration);

}
