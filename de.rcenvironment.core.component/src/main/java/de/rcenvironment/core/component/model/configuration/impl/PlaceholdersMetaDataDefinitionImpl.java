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

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;

/**
 * Implementation of {@link PlaceholdersMetaDataDefinition}.
 * 
 * @author Doreen Seider
 */
public class PlaceholdersMetaDataDefinitionImpl implements Serializable, PlaceholdersMetaDataDefinition {

    private static final int DEFAULT_POSITION = 1;

    private static final long serialVersionUID = 1821211107072929401L;

    private static final String KEY_KEY = "key";

    private static final String KEY_GUI_NAME = "guiName";

    private static final String KEY_GUI_POSITION = "guiPosition";

    private static final String KEY_DECODE = "decode";

    private static final String DYNAMIC = "*";

    private List<Object> rawPlaceholdersDef = new ArrayList<Object>();

    private final Map<String, Object> placeholdersDef = new HashMap<String, Object>();

    private boolean hasDynamicPlaceholder = false;

    /**
     * @param incPlaceholdersDef raw placeholders definition to set
     */
    public void setPlaceholderMetaDataDefinition(List<Object> incPlaceholdersDef) {
        this.rawPlaceholdersDef = incPlaceholdersDef;

        for (Object obj : rawPlaceholdersDef) {
            String placeholderKey = (String) ((Map<String, Object>) obj).get(KEY_KEY);
            this.placeholdersDef.put(placeholderKey, obj);
            hasDynamicPlaceholder = hasDynamicPlaceholder | placeholderKey.equals(DYNAMIC);
        }
    }

    /**
     * @param placeholdersDefs placeholders definitions to set
     */
    @JsonIgnore
    public void setPlaceholderMetaDataDefinition(Set<PlaceholdersMetaDataDefinitionImpl> placeholdersDefs) {
        for (PlaceholdersMetaDataDefinitionImpl def : placeholdersDefs) {
            placeholdersDef.putAll(def.placeholdersDef);
            hasDynamicPlaceholder = hasDynamicPlaceholder | def.hasDynamicPlaceholder;
            rawPlaceholdersDef = def.rawPlaceholdersDef;
        }
    }

    @JsonIgnore
    @Override
    public String getGuiName(String key) {
        if (placeholdersDef.containsKey(key)) {
            return (String) ((Map<String, Object>) placeholdersDef.get(key)).get(KEY_GUI_NAME);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public int getGuiPosition(String key) {
        if (placeholdersDef.containsKey(key)) {
            return Integer.valueOf((String) ((Map<String, Object>) placeholdersDef.get(key)).get(KEY_GUI_POSITION));
        }
        return DEFAULT_POSITION;
    }

    @JsonIgnore
    @Override
    public String getDataType(String key) {
        if (placeholdersDef.containsKey(key)) {
            return (String) ((Map<String, Object>) placeholdersDef.get(key)).get(ComponentConstants.KEY_DATATYPE);
        } else if (hasDynamicPlaceholder) {
            return (String) ((Map<String, Object>) placeholdersDef.get(DYNAMIC)).get(ComponentConstants.KEY_DATATYPE);
        }
        return null;
    }
    

    @JsonIgnore
    @Override
    public boolean decode(String key) {
        if (placeholdersDef.containsKey(key)) {
            return Boolean.valueOf((String) ((Map<String, Object>) placeholdersDef.get(key)).get(KEY_DECODE));
        } else if (hasDynamicPlaceholder) {
            return Boolean.valueOf((String) ((Map<String, Object>) placeholdersDef.get(DYNAMIC)).get(KEY_DECODE));
        }
        return false;
    }

    public List<Object> getPlaceholdersMetaDataDefinition() {
        return rawPlaceholdersDef;
    }

}
