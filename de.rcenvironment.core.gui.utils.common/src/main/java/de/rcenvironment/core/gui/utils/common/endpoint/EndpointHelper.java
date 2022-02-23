/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataConstants.Visibility;
import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointType;

/**
 * Utility methods for Endpoints and endpoint meta data.
 * 
 * @author Sascha Zur
 */
public final class EndpointHelper {

    @Deprecated
    private EndpointHelper() {}

    private static List<String> getMetadataGUINames(Visibility visibility, EndpointType endpointType,
        List<String> dynEndpointIdsToConsider, List<String> statEndpointNamesToConsider, ComponentInstanceProperties compInstProps) {
        Set<String> resultList = new LinkedHashSet<String>();
        Set<EndpointDefinition> dynamicEndpointDefinitions = null;
        Set<EndpointDefinition> staticEndpointDefinitions = null;
        if (compInstProps != null) {
            EndpointDescriptionsManager endpointManager = getEndpointDescriptionsManager(endpointType, compInstProps);
            dynamicEndpointDefinitions = endpointManager.getDynamicEndpointDefinitions(dynEndpointIdsToConsider);
            staticEndpointDefinitions = endpointManager.getStaticEndpointDefinitions(statEndpointNamesToConsider);

            Map<String, Map<Integer, String>> staticGroups = new TreeMap<String, Map<Integer, String>>();
            for (EndpointDefinition currentDefinition : staticEndpointDefinitions) {
                EndpointMetaDataDefinition metaDataDefinition = currentDefinition.getMetaDataDefinition();
                for (String metaDatumKey : metaDataDefinition.getMetaDataKeys()) {
                    if (metaDataDefinition.getVisibility(metaDatumKey).equals(visibility)
                        && checkConfigurationFilter(metaDataDefinition.getGuiVisibilityFilter(metaDatumKey),
                            compInstProps.getConfigurationDescription())) {
                        Map<Integer, String> sortedKeyMap = null;
                        if (staticGroups.get(metaDataDefinition.getGuiGroup(metaDatumKey)) != null) {
                            sortedKeyMap = staticGroups.get(metaDataDefinition.getGuiGroup(metaDatumKey));
                        } else {
                            sortedKeyMap = new TreeMap<Integer, String>();
                            staticGroups.put(metaDataDefinition.getGuiGroup(metaDatumKey), sortedKeyMap);
                        }
                        sortedKeyMap.put(metaDataDefinition.getGuiPosition(metaDatumKey), metaDataDefinition.getGuiName(metaDatumKey));
                    }
                }
            }
            for (Map<Integer, String> groups : staticGroups.values()) {
                for (String key : groups.values()) {
                    resultList.add(key);
                }
            }
            Map<String, Map<Integer, String>> dynamicGroups = new TreeMap<String, Map<Integer, String>>();
            for (EndpointDefinition dynamicDescription : dynamicEndpointDefinitions) {
                if (dynamicDescription == null) {
                    continue;
                } 
                EndpointMetaDataDefinition metaDataDefinition = dynamicDescription.getMetaDataDefinition();
                for (String metaDatumKey : dynamicDescription.getMetaDataDefinition().getMetaDataKeys()) {
                    
                    if (dynamicDescription.getMetaDataDefinition().getVisibility(metaDatumKey).equals(visibility)
                        && checkConfigurationFilter(metaDataDefinition.getGuiVisibilityFilter(metaDatumKey),
                            compInstProps.getConfigurationDescription())) {
                        Map<Integer, String> sortedKeyMap = null;

                        if (dynamicGroups.get(metaDataDefinition.getGuiGroup(metaDatumKey)) != null) {
                            sortedKeyMap = dynamicGroups.get(metaDataDefinition.getGuiGroup(metaDatumKey));
                        } else {
                            sortedKeyMap = new TreeMap<Integer, String>();
                            dynamicGroups.put(metaDataDefinition.getGuiGroup(metaDatumKey), sortedKeyMap);
                        }
                        sortedKeyMap.put(metaDataDefinition.getGuiPosition(metaDatumKey), metaDataDefinition.getGuiName(metaDatumKey));
                    }
                }
            }
            for (Map<Integer, String> groups : dynamicGroups.values()) {
                for (String key : groups.values()) {
                    resultList.add(key);
                }
            }
        }
        return new LinkedList<String>(resultList);
    }
    
    /**
    * @param endpointType {@link EndpointType} to consider
    * @param compInstProps {@link ComponentInstanceProperties} to consider
    * @return {@link List} with names of all static {@link EndpointDefinition}s
     */
    public static List<String> getAllStaticEndpointNames(EndpointType endpointType, ComponentInstanceProperties compInstProps) {
        List<String> endpointNames = new ArrayList<>();
        EndpointDescriptionsManager endpointManager = getEndpointDescriptionsManager(endpointType, compInstProps);
        for (EndpointDefinition endpointDef : endpointManager.getStaticEndpointDefinitions()) {
            endpointNames.add(endpointDef.getName());
        }
        Collections.sort(endpointNames);
        return endpointNames;
    }
    
    /**
    * @param endpointType {@link EndpointType} to consider
    * @param compInstProps {@link ComponentInstanceProperties} to consider
    * @return {@link List} with identifiers of all dynamic {@link EndpointDefinition}s
     */
    public static List<String> getAllDynamicEndpointIds(EndpointType endpointType, ComponentInstanceProperties compInstProps) {
        List<String> endpointIds = new ArrayList<>();
        EndpointDescriptionsManager endpointManager = getEndpointDescriptionsManager(endpointType, compInstProps);
        for (EndpointDefinition endpointDef : endpointManager.getDynamicEndpointDefinitions()) {
            endpointIds.add(endpointDef.getIdentifier());
        }
        return endpointIds;
    }
    
    private static EndpointDescriptionsManager getEndpointDescriptionsManager(EndpointType endpointType,
        ComponentInstanceProperties compInstProps) {
        EndpointDescriptionsManager endpointManager;
        if (endpointType == EndpointType.INPUT) {
            endpointManager = compInstProps.getInputDescriptionsManager();
        } else {
            endpointManager = compInstProps.getOutputDescriptionsManager();
        }
        return endpointManager;
    }

    private static boolean checkConfigurationFilter(Map<String, List<String>> filter,
        ConfigurationDescription configurationDescription) {
        if (filter != null && !filter.isEmpty()) {
            for (String filterKey : filter.keySet()) {
                for (String filterValues : filter.get(filterKey)) {
                    if (filterKey.startsWith("configuration:")) {
                        String newFilterKey = filterKey.split(":")[1];
                        if (configurationDescription.getConfigurationValue(newFilterKey) != null
                            && configurationDescription.getConfigurationValue(newFilterKey).equals(filterValues)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Checks whether the given filter is active or not.
     * 
     * @param filter to check the data
     * @param metaDataValues the current values for checking the filter
     * @param configDesc configuration values to check
     * @return true, if the filter is active, false else.
     */
    public static boolean checkMetadataFilter(Map<String, List<String>> filter, Map<String, String> metaDataValues,
        ConfigurationDescription configDesc) {
        if (filter != null && !filter.isEmpty()) {
            boolean result = true;
            for (String filterKey : filter.keySet()) {

                for (String filterValues : filter.get(filterKey)) {
                    if (filterKey.startsWith("configuration:")) {
                        String newFilterKey = filterKey.split(":")[1];
                        if (!(configDesc.getConfigurationValue(newFilterKey) != null
                            && configDesc.getConfigurationValue(newFilterKey).equals(filterValues))) {
                            result = false;
                        }
                    } else {
                        if (!(metaDataValues.get(filterKey) != null && metaDataValues.get(filterKey).equals(filterValues))) {
                            result = false;
                        }
                    }
                }
            }
            return result;
        }
        return true;
    }

    /**
     * @param endpointType if the inputs or outputs shall be filtered
     * @param dynEndpointIdsToConsider identifiers of dynamic endpoints to consider
     * @param statEndpointNamesToConsider identifiers of static endpoints to consider
     * @param configuration to filter from
     * @return List of all meta data GUI names which shall be shown in the in-/output table.
     */
    public static List<String> getMetaDataNamesForTable(EndpointType endpointType,
        List<String> dynEndpointIdsToConsider, List<String> statEndpointNamesToConsider, ComponentInstanceProperties configuration) {
        return getMetadataGUINames(Visibility.shown, endpointType, dynEndpointIdsToConsider, statEndpointNamesToConsider, configuration);
    }

}
