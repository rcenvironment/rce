/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.configuration.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationMetaDataDefinition;

/**
 * Implementation of {@link ConfigurationMetaDataDefinition}.
 * 
 * @author Doreen Seider
 */
public class ConfigurationMetaDataDefinitionImpl implements Serializable, ConfigurationMetaDataDefinition {

    private static final int DEFAULT_POSITION = 1;

    private static final long serialVersionUID = 1821211107072929401L;

    private List<Object> rawConfigurationDef = new ArrayList<Object>();

    private Map<String, Object> configurationDef = new HashMap<String, Object>();

    /**
     * @param incPlaceholdersDef raw configuration meta data definition to set
     */
    public void setConfigurationMetaDataDefinition(List<Object> incPlaceholdersDef) {
        rawConfigurationDef = incPlaceholdersDef;

        for (Object obj : rawConfigurationDef) {
            String placeholderKey = (String) ((Map<String, Object>) obj).get(ConfigurationDefinitionConstants.KEY_METADATA_CONFIG_KEY);
            configurationDef.put(placeholderKey, obj);
        }
    }

    /**
     * @param configurationDefs configuration meta data definitions to set
     */
    @JsonIgnore
    public void setConfigurationMetaDataDefinition(Set<ConfigurationMetaDataDefinitionImpl> configurationDefs) {
        for (ConfigurationMetaDataDefinitionImpl def : configurationDefs) {
            configurationDef.putAll(def.configurationDef);
            rawConfigurationDef = def.rawConfigurationDef;
        }
    }

    @JsonIgnore
    @Override
    public String getGuiName(String key) {
        if (configurationDef.containsKey(key)) {
            return (String) ((Map<String, Object>) configurationDef.get(key)).get(ConfigurationDefinitionConstants.KEY_METADATA_GUI_NAME);
        }
        return key;
    }

    @JsonIgnore
    @Override
    public int getGuiPosition(String key) {
        if (configurationDef.containsKey(key)) {
            return Integer.valueOf((String) ((Map<String, Object>) configurationDef.get(key)).
                get(ConfigurationDefinitionConstants.KEY_METADATA_GUI_POSITION));
        }
        return DEFAULT_POSITION;
    }

    @JsonIgnore
    @Override
    public String getGuiGroupName(String key) {
        if (configurationDef.containsKey(key)) {
            return (String) ((Map<String, Object>) configurationDef.get(key))
                .get(ConfigurationDefinitionConstants.KEY_METADATA_GUI_GROUP_NAME);
        }
        return key;
    }

    @JsonIgnore
    @Override
    public String getMetaDataValue(String key, String metaDataKey) {
        if (configurationDef.containsKey(key)) {
            return (String) ((Map<String, Object>) configurationDef.get(key)).get(metaDataKey);
        }
        return null;
    }

    public List<Object> getConfigurationMetaDataDefinition() {
        return rawConfigurationDef;
    }

}
