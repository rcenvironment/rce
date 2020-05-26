/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.execute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Utils class for sorting the placeholder in the GUI.
 *
 * @author Sascha Zur
 */
public final class PlaceholderSortUtils {

    private PlaceholderSortUtils() {}

    /**
     * This method sorts the given list of component identifier based on their component names.
     * 
     * @param instancesWithPlaceholder : The list to sort
     * @param workflowDescription : the wd with the components, used for getting the names
     * @return Sorted List.
     */
    public static List<String> sortInstancesWithPlaceholderByName(
        List<String> instancesWithPlaceholder, WorkflowDescription workflowDescription) {
        List<String> sortedList = new LinkedList<>();
        for (String identifier : instancesWithPlaceholder) {
            String componentName = workflowDescription.getWorkflowNode(identifier).getName();
            int i = 0;
            while (i < sortedList.size()
                && workflowDescription.getWorkflowNode(sortedList.get(i)).getName().compareToIgnoreCase(componentName) < 0) {
                i++;
            }
            sortedList.add(i, identifier);
        }
        return sortedList;
    }

    /**
     * Sorts the global placeholder for the workflowPage and the ClearHistoryDialog.
     * 
     * @param placeholderNameKeysOfComponentID :Set of names
     * @param metaData contains ordering information
     * @return sorted List
     */
    public static List<String> sortGlobalPlaceholders(Set<String> placeholderNameKeysOfComponentID,
        PlaceholdersMetaDataDefinition metaData) {
        Map<Integer, List<String>> prioLists = new HashMap<>();

        if (placeholderNameKeysOfComponentID != null) {
            for (String componentPlaceholder : placeholderNameKeysOfComponentID) {
                int prio = metaData.getGuiPosition(componentPlaceholder);
                List<String> prioList = prioLists.computeIfAbsent(prio, key -> new ArrayList<>());
                prioList.add(componentPlaceholder);
            }
        } else {
            prioLists = null;
        }
        List<String> result = new ArrayList<>();
        if (prioLists != null && !prioLists.isEmpty()) {
            List<Integer> prioCategories = new ArrayList<>(prioLists.keySet());
            Collections.sort(prioCategories);

            for (Integer i : prioCategories) {
                List<String> current = prioLists.get(i);
                Collections.sort(current);
                result.addAll(current);
            }
        }
        return result;
    }

}
