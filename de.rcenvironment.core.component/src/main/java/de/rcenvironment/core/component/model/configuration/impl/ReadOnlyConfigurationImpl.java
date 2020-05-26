/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.configuration.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;

/**
 * Implementation if read-only configuration.
 * 
 * @author Doreen Seider
 */
public class ReadOnlyConfigurationImpl implements ReadOnlyConfiguration, Serializable {

    private static final long serialVersionUID = 60339704245897514L;

    private Map<String, String> configuration = new HashMap<String, String>();
    
    @Override
    public Set<String> getConfigurationKeys() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String key) {
        return configuration.get(key);
    }

    @Override
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

}
