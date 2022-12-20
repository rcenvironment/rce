/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Describes endpoint meta data.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Tim Rosenbach
 * 
 * Note: Used to generate GUI elements in the endpoint dialog and to provide proper display names, proper ordering, etc. --seid_do
 */
public interface EndpointMetaDataDefinition {

    /**
     * @return all meta data keys
     */
    Set<String> getMetaDataKeys();

    /**
     * @param key meta data key
     * @return name which should appear in the GUI
     */
    String getGuiName(String key);

    /**
     * @param key meta data key
     * @return name of the group the key should be in
     */
    String getGuiGroup(String key);

    /**
     * @param key meta data key
     * @return position the GUI name should appear at or -1, if key is null
     */
    int getGuiPosition(String key);

    /**
     * @param key meta data key
     * @return activation filter: meta data value set under which the meta datum is active in the GUI
     */
    Map<String, List<String>> getGuiActivationFilter(String key);
    /**
     * @param key meta data key
     * @return visibility filter: meta data value set under which the meta datum is shown in the GUI
     */
    Map<String, List<String>> getGuiVisibilityFilter(String key);
    /**
     * @param key meta data key
     * @return possible values for this meta data. "*" if every alphanumeric value is possible
     */
    List<String> getPossibleValues(String key);

    /**
     * @param key meta data key
     * @return GUI names of possible values for this meta data.
     */
    List<String> getGuiNamesOfPossibleValues(String key);

    /**
     * @param key meta data key
     * @return default meta data value or <code>null</code> if no default is defined
     */
    String getDefaultValue(String key);

    /**
     * @param key meta data key
     * @return data type of given meta datum
     */
    String getDataType(String key);
    
    /**
     * @param key meta data key
     * @param dataType
     */
    void setDataType(String key, String dataType);

    /**
     * @param key meta data key
     * @param dataType {@link DataType} to check for
     * @return <code>true</code> if meta datum is defined for given {@link DataType}, otherwise <code>false</code>
     */
    boolean isDefinedForDataType(String key, DataType dataType);

    /**
     * @param key meta data key
     * @return comma separated list of validations for the meta datum
     */
    String getValidation(String key);

    /**
     * @param key meta data key
     * @return visibility level of meta datum
     */
    Visibility getVisibility(String key);

    /**
     * @param key meta data key to get activation filter for
     * @return action filter as map or <code>null</code> if no activation filter was defined
     */
    Map<String, List<String>> getActivationFilter(String key);

    /**
     * @param key meta data key
     * @return true if the metadata with the given key should be persisted in the data management.
     */
    boolean isPersistent(String key);

}
