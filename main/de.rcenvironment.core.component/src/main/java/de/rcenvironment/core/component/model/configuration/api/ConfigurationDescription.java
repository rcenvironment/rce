/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.api;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.configuration.impl.ConfigurationDefinitionImpl;
import de.rcenvironment.core.component.model.spi.PropertiesChangeSupport;

/**
 * Provides information about the component configuration.
 * 
 * @author Doreen Seider
 */
public class ConfigurationDescription extends PropertiesChangeSupport implements Serializable {

    /** Property that is fired when the configuration changes. */
    private static final String CONFIGURATION_PROP = "de.rcenvironment.core.component.configuration.ComponentsConfiguration";

    private static final long serialVersionUID = -1574122807840952727L;

    private final ConfigurationDefinition configDefinition;

    private final ConfigurationDefinition combinedConfigDef;

    private final Set<ConfigurationExtensionDefinition> extConfigDefinitions;

    private Map<String, String> configuration;

    private Map<String, String> placeholders;

    public ConfigurationDescription(ConfigurationDefinition configDef, Set<ConfigurationExtensionDefinition> extConfigDefs) {

        Set<ConfigurationDefinition> configDefs = new HashSet<ConfigurationDefinition>();
        configDefs.add(configDef);
        configDefs.addAll(extConfigDefs);

        configDefinition = configDef;
        extConfigDefinitions = extConfigDefs;

        combinedConfigDef = new ConfigurationDefinitionImpl();
        ((ConfigurationDefinitionImpl) combinedConfigDef).setConfigurationDefinitions(configDefs);

        configuration = new HashMap<String, String>();

        for (String key : combinedConfigDef.getConfigurationKeys()) {
            if (combinedConfigDef.getDefaultValue(key) != null) {
                configuration.put(key, combinedConfigDef.getDefaultValue(key));
            }
        }
        placeholders = new HashMap<String, String>();
    }

    /**
     * @return underlying {@link ConfigurationDefinition}
     */
    public ConfigurationDefinition getComponentConfigurationDefinition() {
        return combinedConfigDef;
    }

    /**
     * @return active {@link ConfigurationDefinition}
     */
    public ConfigurationDefinition getActiveConfigurationDefinition() {
        Set<ConfigurationDefinition> configDefs = new HashSet<>();
        if (((ConfigurationDefinitionImpl) configDefinition).isActive(configuration)) {
            configDefs.add(getActiveConfigurationDefinitionFromConfigurationDefinition(configDefinition));
        }
        for (ConfigurationDefinition configDef : extConfigDefinitions) {
            if (((ConfigurationDefinitionImpl) configDef).isActive(configuration)) {
                configDefs.add(getActiveConfigurationDefinitionFromConfigurationDefinition(configDef));
            }
        }
        
        ConfigurationDefinitionImpl def = new ConfigurationDefinitionImpl();
        def.setConfigurationDefinitions(configDefs);
        return def;
    }
    
    private ConfigurationDefinition getActiveConfigurationDefinitionFromConfigurationDefinition(
        ConfigurationDefinition incConfigDefinition) {

        List<Object> activeConfigurations = new LinkedList<Object>();
        for (String key : incConfigDefinition.getConfigurationKeys()) {
            Map<String, List<String>> keyActivationFilter = ((ConfigurationDefinitionImpl) incConfigDefinition).getActivationFilter(key);
            if (keyActivationFilter != null) {
                for (String neededKey : keyActivationFilter.keySet()) {
                    if (configuration.containsKey(neededKey)
                        && keyActivationFilter.get(neededKey).contains(configuration.get(neededKey))) {
                        activeConfigurations.add(((ConfigurationDefinitionImpl) incConfigDefinition).getConfigurationEntry(key));
                    }
                }
            } else {
                activeConfigurations.add(((ConfigurationDefinitionImpl) incConfigDefinition).getConfigurationEntry(key));
            }
        }

        ConfigurationDefinitionImpl configurationDefinitionImpl = new ConfigurationDefinitionImpl();
        configurationDefinitionImpl.setRawConfigurationDefinition(activeConfigurations);
        configurationDefinitionImpl.setRawPlaceholderMetaDataDefinition(((ConfigurationDefinitionImpl) incConfigDefinition)
            .getRawPlaceholderMetaDataDefinition());
        configurationDefinitionImpl.setRawActivationFilters(((ConfigurationDefinitionImpl) incConfigDefinition).getActivationFilter());
        return configurationDefinitionImpl;
    }

    /**
     * @param key configuration key
     * @return configuration value or <code>null</code> if key doesn't exist
     */
    public String getConfigurationValue(String key) {

        String configValue = configuration.get(key);

        if (isPlaceholder(configValue)) {
            String placeholderValue = placeholders.get(getNameOfPlaceholder(configValue));
            if (placeholderValue != null) {
                if (combinedConfigDef.getPlaceholderMetaDataDefinition().decode(getNameOfPlaceholder(configValue))) {
                    try {
                        configValue = new String(new Base64().decode(placeholderValue.getBytes("UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("decoding placeholder value failed", e);
                    }
                } else {
                    configValue = placeholderValue;
                }
            }

        }
        return configValue;

    }

    /**
     * @return configuration {@link Map} including read-only configuration values
     */
    public Map<String, String> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
    }

    /**
     * @param key configuration key
     * @param value configuration value (includes read-only configuration)
     */
    public void setConfigurationValue(String key, String value) {
        String oldValue = configuration.get(key);
        if (value != null) {
            configuration.put(key, value);
        } else {
            configuration.remove(key);
        }

        firePropertyChange(CONFIGURATION_PROP, oldValue, value);
        firePropertyChange(ComponentDescription.PROPERTIES_PREFIX + key, oldValue, value);
    }

    public void setConfiguration(Map<String, String> newConfiguration) {
        configuration = newConfiguration;
    }

    /**
     * @param key of configuration
     * @return <code>true</code> if current configuration value is a placeholder, otherwise
     *         <code>false</code>
     */
    public boolean isPlaceholderSet(String key) {
        return isPlaceholder(configuration.get(key));
    }

    /**
     * @param key of configuration
     * @return configuration value as it was set. it is not replaced by a placeholder
     */
    public String getActualConfigurationValue(String key) {
        return configuration.get(key);
    }

    /**
     * @param key of placeholder
     * @param value of placeholder
     */
    public void setPlaceholderValue(String key, String value) {
        placeholders.put(key, value);
    }

    public void setPlaceholders(Map<String, String> newPlaceholders) {
        placeholders = newPlaceholders;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    private String getNameOfPlaceholder(String fullPlaceholder) {
        return ComponentUtils.getMatcherForPlaceholder(fullPlaceholder).group(ComponentUtils.PLACEHOLDERNAME);
    }

    private boolean isPlaceholder(String configurationValue) {
        if (configurationValue != null) {
            return configurationValue.matches(ComponentUtils.PLACEHOLDER_REGEX);
        }
        return false;
    }

}
