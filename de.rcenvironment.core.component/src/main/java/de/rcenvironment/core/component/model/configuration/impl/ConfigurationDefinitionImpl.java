/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinition;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationMetaDataDefinition;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.model.configuration.api.ReadOnlyConfiguration;

/**
 * Implementation of {@link ConfigurationDefinition}.
 * 
 * @author Doreen Seider
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationDefinitionImpl implements Serializable, ConfigurationDefinition {

    private static final long serialVersionUID = -3257767738643151923L;

    private List<Object> rawConfigurationDef = new ArrayList<Object>();

    private List<Object> rawPlaceholderMetaDataDef = new ArrayList<Object>();
    
    private List<Object> rawConfigurationMetaDataDef = new ArrayList<Object>();

    private Map<String, String> rawReadOnlyConfiguration = new HashMap<String, String>();

    private Map<String, Object> rawActivationFilter = new HashMap<>();

    @JsonIgnore
    private Map<String, Object> configurationDef = new HashMap<String, Object>();

    @JsonIgnore
    private PlaceholdersMetaDataDefinition placeholderMetaDataDef = new PlaceholdersMetaDataDefinitionImpl();

    @JsonIgnore
    private ConfigurationMetaDataDefinition configurationMetaDataDef = new ConfigurationMetaDataDefinitionImpl();

    @JsonIgnore
    private ReadOnlyConfiguration readOnlyConfiguration = new ReadOnlyConfigurationImpl();

    /**
     * @param configurationDefs {@link ConfigurationDefinition} objects to merge and set
     */
    @JsonIgnore
    public void setConfigurationDefinitions(Set<ConfigurationDefinition> configurationDefs) {
        for (ConfigurationDefinition def : configurationDefs) {
            rawConfigurationDef.addAll(((ConfigurationDefinitionImpl) def).rawConfigurationDef);
            rawPlaceholderMetaDataDef.addAll(((ConfigurationDefinitionImpl) def).rawPlaceholderMetaDataDef);
            rawConfigurationMetaDataDef.addAll(((ConfigurationDefinitionImpl) def).rawConfigurationMetaDataDef);
            rawReadOnlyConfiguration.putAll(((ConfigurationDefinitionImpl) def).rawReadOnlyConfiguration);
        }
        setRawConfigurationDefinition(rawConfigurationDef);
        setRawPlaceholderMetaDataDefinition(rawPlaceholderMetaDataDef);
        setRawConfigurationMetaDataDefinition(rawConfigurationMetaDataDef);
        setRawReadOnlyConfiguration(rawReadOnlyConfiguration);
    }

    @JsonIgnore
    @Override
    public Set<String> getConfigurationKeys() {
        return Collections.unmodifiableSet(configurationDef.keySet());
    }

    /**
     * @param key configuration key
     * @return default value or <code>null</code> if no one is defined
     */
    @JsonIgnore
    @Override
    public String getDefaultValue(String key) {
        return (String) ((Map<String, Object>) configurationDef.get(key)).get(ComponentConstants.KEY_DEFAULT_VALUE);
    }
    
    /**
     * @param key configuration key
     * @return activation filter for key or <code>null</code> if no one is defined
     */
    @JsonIgnore
    public Map<String, List<String>> getActivationFilter(String key) {
        return (Map<String, List<String>>) ((Map<String, Object>) configurationDef.get(key))
            .get(ConfigurationDefinitionConstants.JSON_KEY_ACTIVATION_FILTER);
    }
    
    /**
     * @param configuration current configuration
     * @return <code>true</code> if configuration is active and must be considered, otherwise <code>false</code>
     */
    @JsonIgnore
    public boolean isActive(Map<String, String> configuration) {
        if (rawActivationFilter != null) {
            for (String key : rawActivationFilter.keySet()) {
                if (configuration.get(key) == null || !((List<Object>) rawActivationFilter.get(key)).contains(configuration.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get complete declarative configuration entry from given key.
     * 
     * @param key to get configuration from
     * @return all information about the configuration
     */
    @JsonIgnore
    public Object getConfigurationEntry(String key) {
        return configurationDef.get(key);
    }

    @JsonIgnore
    @Override
    public ReadOnlyConfiguration getReadOnlyConfiguration() {
        return readOnlyConfiguration;
    }

    @JsonIgnore
    @Override
    public PlaceholdersMetaDataDefinition getPlaceholderMetaDataDefinition() {
        return placeholderMetaDataDef;
    }

    @JsonIgnore
    @Override
    public ConfigurationMetaDataDefinition getConfigurationMetaDataDefinition() {
        return configurationMetaDataDef;
    }
    
    @Override
    public List<Object> getRawConfigurationDefinition() {
        return rawConfigurationDef;
    }

    public List<Object> getRawPlaceholderMetaDataDefinition() {
        return rawPlaceholderMetaDataDef;
    }

    @Override
    public List<Object> getRawConfigurationMetaDataDefinition() {
        return rawConfigurationMetaDataDef;
    }

    public Map<String, String> getRawReadOnlyConfiguration() {
        return rawReadOnlyConfiguration;
    }
    
    public Map<String, Object> getRawActivationFilter() {
        return rawActivationFilter;
    }
    
    /**
     * @param incConfigurationDef raw configuration definition
     */
    public void setRawConfigurationDefinition(List<Object> incConfigurationDef) {
        rawConfigurationDef = incConfigurationDef;
        for (Object obj : rawConfigurationDef) {
            configurationDef.put((String) ((Map<String, Object>) obj).get(ConfigurationDefinitionConstants.KEY_CONFIGURATION_KEY), obj);
        }
    }

    /**
     * @param incPlaceholderMetaDataDef raw placeholders definition
     */
    public void setRawPlaceholderMetaDataDefinition(List<Object> incPlaceholderMetaDataDef) {
        rawPlaceholderMetaDataDef = incPlaceholderMetaDataDef;
        placeholderMetaDataDef = new PlaceholdersMetaDataDefinitionImpl();
        ((PlaceholdersMetaDataDefinitionImpl) placeholderMetaDataDef).setPlaceholderMetaDataDefinition(incPlaceholderMetaDataDef);
    }

    /**
     * @param incConfigurationMetaDataDef raw placeholder meta data definition
     */
    public void setRawConfigurationMetaDataDefinition(List<Object> incConfigurationMetaDataDef) {
        rawConfigurationMetaDataDef = incConfigurationMetaDataDef;
        configurationMetaDataDef = new ConfigurationMetaDataDefinitionImpl();
        ((ConfigurationMetaDataDefinitionImpl) configurationMetaDataDef).setConfigurationMetaDataDefinition(incConfigurationMetaDataDef);
    }

    /**
     * @param incReadOnlyConfiguration raw read-only configuration meta data definition
     */
    public void setRawReadOnlyConfiguration(Map<String, String> incReadOnlyConfiguration) {
        rawReadOnlyConfiguration = incReadOnlyConfiguration;
        readOnlyConfiguration = new ReadOnlyConfigurationImpl();
        ((ReadOnlyConfigurationImpl) readOnlyConfiguration).setConfiguration(incReadOnlyConfiguration);
    }
    
    /**
     * @param incActivationFiler raw activation filter definition
     */
    public void setRawActivationFilter(Map<String, Object> incActivationFiler) {
        rawActivationFilter = incActivationFiler;
    }

}
