/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.integration;

import java.util.Map;

/**
 * A migration that can be applied to the map backing a {@link ConfigurationMap}.
 * 
 * @author Alexander Weinert
 */
public interface ConfigurationMapMigration {
    /**
     * @param configurationMap The mutable map backing a {@link ConfigurationMap}. Is never null.
     */
    void migrate(Map<String, Object> configurationMap);
}
