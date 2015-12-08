/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.endpoint;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
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
    private EndpointHelper() {

    }

    /**
     * Gets a List of all static endpoint names from the given configuration in the given direction.
     * 
     * @param direction if it should be in- or outputs.
     * @param configuration to look at
     * @return List of all static endpoint names
     */
    public static List<String> getStaticEndpointNames(EndpointType direction, ComponentInstanceProperties configuration) {
        List<String> result = new LinkedList<String>();
        Set<EndpointDescription> descriptions = new HashSet<EndpointDescription>();
        if (direction == EndpointType.INPUT) {
            if (configuration != null && configuration.getInputDescriptionsManager() != null) {
                descriptions = configuration.getInputDescriptionsManager().getStaticEndpointDescriptions();
            }
        } else {
            if (configuration != null && configuration.getInputDescriptionsManager() != null) {
                descriptions = configuration.getOutputDescriptionsManager().getStaticEndpointDescriptions();
            }
        }
        for (EndpointDescription e : descriptions) {
            result.add(e.getName());
        }
        return result;

    }

    /**
     * Gets a List of all dynamic endpoint names from the given configuration in the given direction.
     * 
     * @param direction if it should be in- or outputs.
     * @param id of dynamic endpoints
     * @param configuration to look at
     * @param filter filter for id
     * @return List of all dynamic endpoint names
     */
    public static List<String> getDynamicEndpointNames(EndpointType direction, String id,
        ComponentInstanceProperties configuration, boolean filter) {
        List<String> result = new LinkedList<String>();
        Set<EndpointDescription> descriptions = new HashSet<EndpointDescription>();
        if (direction == EndpointType.INPUT) {
            if (configuration != null && configuration.getInputDescriptionsManager() != null) {
                descriptions = configuration.getInputDescriptionsManager().getDynamicEndpointDescriptions();
            }
        } else {
            if (configuration != null && configuration.getInputDescriptionsManager() != null) {
                descriptions = configuration.getOutputDescriptionsManager().getDynamicEndpointDescriptions();
            }
        }
        for (EndpointDescription e : descriptions) {
            if (!filter || e.getEndpointDefinition().getIdentifier().equals(id)) {
                result.add(e.getName());
            }
        }
        return result;
    }

    private static List<String> getMetadataGUINames(Visibility visibility, EndpointType direction,
        String id, ComponentInstanceProperties configuration) {
        Set<String> resultList = new LinkedHashSet<String>();
        Set<EndpointDefinition> dynamicDescriptions = null;
        Set<EndpointDescription> staticDescriptions = null;
        if (configuration != null) {
            if (direction == EndpointType.INPUT) {
                dynamicDescriptions = configuration.getInputDescriptionsManager().getDynamicEndpointDefinitions();
                staticDescriptions = configuration.getInputDescriptionsManager().getStaticEndpointDescriptions();
            } else {
                dynamicDescriptions = configuration.getOutputDescriptionsManager().getDynamicEndpointDefinitions();
                staticDescriptions = configuration.getOutputDescriptionsManager().getStaticEndpointDescriptions();
            }
            Map<String, Map<Integer, String>> staticGroups = new TreeMap<String, Map<Integer, String>>();

            for (EndpointDescription currentDescription : staticDescriptions) {
                EndpointMetaDataDefinition metaDataDefinition =
                    currentDescription.getEndpointDefinition().getMetaDataDefinition();
                for (String metaDatumKey : metaDataDefinition
                    .getMetaDataKeys()) {
                    if (metaDataDefinition.getVisibility(metaDatumKey)
                        .equals(visibility)) {
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
            for (EndpointDefinition dynamicDescription : dynamicDescriptions) {
                EndpointMetaDataDefinition metaDataDefinition = dynamicDescription.getMetaDataDefinition();
                for (String metaDatumKey : dynamicDescription.getMetaDataDefinition().getMetaDataKeys()) {
                    if (dynamicDescription.getIdentifier().equals(id)
                        && dynamicDescription.getMetaDataDefinition().getVisibility(metaDatumKey)
                            .equals(visibility)) {
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
     * @param direction if the inputs or outputs shall be filtered
     * @param id for dynamic channel
     * @param configuration to filter from
     * @return List of all meta data GUI names which shall be shown in the in-/output table.
     */
    public static List<String> getMetaDataNamesForTable(EndpointType direction,
        String id, ComponentInstanceProperties configuration) {
        return getMetadataGUINames(Visibility.shown, direction, id, configuration);

    }

    /**
     * @param direction if the inputs or outputs shall be filtered
     * @param configuration to filter from
     * @param id for dynamic channel
     * @return List of all meta data GUI names which shall be shown in the configuration dialog.
     */
    public static List<String> getMetadataNamesForDialog(EndpointType direction,
        ComponentInstanceProperties configuration, String id) {
        return getMetadataGUINames(Visibility.userConfigurable, direction, id, configuration);
    }

    /**
     * @param direction if the inputs or outputs shall be filtered
     * @param configuration to filter from
     * @param id for dynamic channel
     * @return List of all meta data GUI names which shall not be shown.
     */
    public static List<String> getMetadataNamesForDeveloper(EndpointType direction,
        ComponentInstanceProperties configuration, String id) {
        return getMetadataGUINames(Visibility.developerConfigurable, direction, id, configuration);
    }
}
