/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.impl;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationExtensionDefinition;

/**
 * Implementation of {@link ConfigurationExtensionDefinition}.
 * 
 * @author Doreen Seider
 */
public class ConfigurationExtensionDefinitionImpl extends ConfigurationDefinitionImpl implements ConfigurationExtensionDefinition {

    private static final long serialVersionUID = -3257767738643151923L;

    private Map<String, Object> activationFilter;

    public void setActivationFilter(Map<String, Object> activationFilter) {
        this.activationFilter = activationFilter;
    }

    @JsonIgnore
    @Override
    public boolean isActive(Map<String, String> configuration) {
        if (activationFilter != null) {
            for (String key : activationFilter.keySet()) {
                if (configuration.get(key) == null || !((List<Object>) activationFilter.get(key)).contains(configuration.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Map<String, Object> getActivationFilter() {
        return activationFilter;
    }

}
