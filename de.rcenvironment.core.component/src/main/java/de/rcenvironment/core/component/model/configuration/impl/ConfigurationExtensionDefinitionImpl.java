/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;

/**
 * Implementation of {@link ConfigurationExtensionDefinition}.
 * 
 * @author Doreen Seider
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationExtensionDefinitionImpl extends ConfigurationDefinitionImpl implements ConfigurationExtensionDefinition {

    private static final long serialVersionUID = -3257767738643151923L;

    @JsonIgnore
    @Override
    public boolean isActive(Map<String, String> configuration) {
        return super.isActive(configuration);
    }

}
