/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinitionConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link EndpointMetaDataDefinition}.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Tim Rosenbach
 */
public class EndpointMetaDataDefinitionImpl implements Serializable, EndpointMetaDataDefinition {

    private static final long serialVersionUID = 1277591939597311465L;

    private static final int MINUS_ONE = -1;

    private static final String KEY_POSSIBLEVALUES = "possibleValues";

    private static final String KEY_GUINAMESOFPOSSIBLEVALUES = "guiNamesOfPossibleValues";

    private static final String KEY_VALIDATION = "validation";

    private static final String KEY_GUI_ACTIVATION_FILTER = "guiActivationFilter";

    private static final String DEFAULT_GROUP = "";

    private static final String KEY_ENDPOINT_DATATYPES = "endpointDataTypes";

    private static final String KEY_PERSISTENT = "persistent";

    private static final Object KEY_GUI_VISIBILITY_FILTER = "guiVisibilityFilter";

    // <meta data key, <meta data prop key, meta data prop value>>
    private Map<String, Map<String, Object>> rawMetaData;

    private Map<String, Map<String, Object>> rawMetaDataExtension;

    private Map<String, Map<String, Object>> combinedRawMetaData;

    private Map<String, List<DataType>> endpointDataTypes = new HashMap<String, List<DataType>>();

    @JsonIgnore
    @Override
    public Set<String> getMetaDataKeys() {
        return Collections.unmodifiableSet(combinedRawMetaData.keySet());
    }

    @JsonIgnore
    @Override
    public String getGuiName(String key) {
        return (String) combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_GUI_NAME);
    }

    @JsonIgnore
    @Override
    public String getGuiGroup(String key) {
        if (combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_GUIGROUP) != null) {
            return (String) combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_GUIGROUP);
        }
        return DEFAULT_GROUP;
    }

    @JsonIgnore
    @Override
    public int getGuiPosition(String key) {
        if (combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_GUI_POSITION) != null) {
            return Integer.valueOf((String) combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_GUI_POSITION));
        }
        return MINUS_ONE;
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    @Override
    public Map<String, List<String>> getGuiActivationFilter(String key) {
        return (Map<String, List<String>>) combinedRawMetaData.get(key).get(KEY_GUI_ACTIVATION_FILTER);
    }
    @SuppressWarnings("unchecked")
    @JsonIgnore
    @Override
    public Map<String, List<String>> getGuiVisibilityFilter(String key) {
        return (Map<String, List<String>>) combinedRawMetaData.get(key).get(KEY_GUI_VISIBILITY_FILTER);
    }
    @SuppressWarnings("unchecked")
    @JsonIgnore
    @Override
    public List<String> getPossibleValues(String key) {
        return (List<String>) combinedRawMetaData.get(key).get(KEY_POSSIBLEVALUES);
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    @Override
    public List<String> getGuiNamesOfPossibleValues(String key) {
        if (combinedRawMetaData.get(key).containsKey(KEY_GUINAMESOFPOSSIBLEVALUES)) {
            return (List<String>) combinedRawMetaData.get(key).get(KEY_GUINAMESOFPOSSIBLEVALUES);
        } else {
            return getPossibleValues(key);
        }
    }

    @JsonIgnore
    @Override
    public String getDefaultValue(String key) {
        if ((String) combinedRawMetaData.get(key).get(ComponentConstants.KEY_DEFAULT_VALUE) != null) {
            return (String) combinedRawMetaData.get(key).get(ComponentConstants.KEY_DEFAULT_VALUE);
        }
        return "";
    }

    @JsonIgnore
    @Override
    public String getDataType(String key) {
        if (combinedRawMetaData.get(key).get(ComponentConstants.KEY_DATATYPE) != null) {
            return (String) combinedRawMetaData.get(key).get(ComponentConstants.KEY_DATATYPE);
        }
        return EndpointMetaDataConstants.TYPE_TEXT;
    }
    
    @JsonIgnore
    @Override
    public void setDataType(String key, String dataType) {
        if (combinedRawMetaData.containsKey(key)) {
            combinedRawMetaData.get(key).put(ComponentConstants.KEY_DATATYPE, dataType);
        }
    }

    @JsonIgnore
    @Override
    public boolean isDefinedForDataType(String key, DataType dataType) {
        if (!combinedRawMetaData.get(key).containsKey(KEY_ENDPOINT_DATATYPES)) {
            return true;
        } else {
            return endpointDataTypes.get(key).contains(dataType);
        }
    }

    @JsonIgnore
    @Override
    public String getValidation(String key) {
        return (String) combinedRawMetaData.get(key).get(KEY_VALIDATION);
    }

    @JsonIgnore
    @Override
    public Visibility getVisibility(String key) {
        if (combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_VISIBILITY) != null) {
            return Visibility.valueOf((String) combinedRawMetaData.get(key).get(EndpointDefinitionConstants.KEY_VISIBILITY));
        }
        return Visibility.userConfigurable;
    }

    @JsonIgnore
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getActivationFilter(String key) {
        return (Map<String, List<String>>) combinedRawMetaData.get(key).get(ConfigurationDefinitionConstants.JSON_KEY_ACTIVATION_FILTER);
    }

    /**
     * @param metaData raw meta data definition
     */
    @SuppressWarnings("unchecked")
    public void setRawMetaData(Map<String, Map<String, Object>> metaData) {
        rawMetaData = metaData;
        combinedRawMetaData = new HashMap<>(rawMetaData);
        for (String key : metaData.keySet()) {
            if (metaData.get(key).containsKey(KEY_ENDPOINT_DATATYPES)) {
                List<DataType> dataTypes = new ArrayList<DataType>();
                for (String type : (List<String>) metaData.get(key).get(KEY_ENDPOINT_DATATYPES)) {
                    dataTypes.add(DataType.valueOf(type));
                }
                endpointDataTypes.put(key, dataTypes);
            }
        }
    }

    /**
     * @param metaDataExtension raw meta data extension definition
     */
    public void setRawMetaDataExtensions(Map<String, Map<String, Object>> metaDataExtension) {
        rawMetaDataExtension = metaDataExtension;
        for (String key : metaDataExtension.keySet()) {
            if (!rawMetaData.containsKey(key)) {
                combinedRawMetaData.put(key, metaDataExtension.get(key));
            } else {
                LogFactory.getLog(getClass()).warn(StringUtils.format("Meta data key '%s' is already defined and will be ignored", key));
            }
        }
    }

    public Map<String, Map<String, Object>> getRawMetaData() {
        return rawMetaData;
    }

    public Map<String, Map<String, Object>> getRawMetaDataExtension() {
        return rawMetaDataExtension;
    }

    @JsonIgnore
    @Override
    public boolean isPersistent(String key) {
        if (combinedRawMetaData.get(key) != null && combinedRawMetaData.get(key).containsKey(KEY_PERSISTENT)) {
            return Boolean.parseBoolean((String) combinedRawMetaData.get(key).get(KEY_PERSISTENT));
        } 
        return false;
    }
}
