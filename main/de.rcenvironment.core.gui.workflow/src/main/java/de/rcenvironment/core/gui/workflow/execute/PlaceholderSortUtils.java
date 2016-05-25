/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.execute;

import java.util.LinkedList;
import java.util.List;
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
        List<String> sortedList = new LinkedList<String>();
        for (String identifier : instancesWithPlaceholder){
            String componentName = workflowDescription.getWorkflowNode(identifier).getName();
            int i = 0;
            while (i < sortedList.size() 
                && workflowDescription.getWorkflowNode(sortedList.get(i)).getName().compareToIgnoreCase(componentName) < 0){
                i++;
            }
            sortedList.add(i, identifier);
        }
        return sortedList;
    }
    
    /**
     * Sorts the global placeholder for the workflowPage and the ClearHistoryDialog.
     * @param placeholderNameKeysOfComponentID :Set of names
     * @param metaData contains ordering information
     * @return sorted List
     */
    public static List<String> getPlaceholderOrder(Set<String> placeholderNameKeysOfComponentID, 
        PlaceholdersMetaDataDefinition metaData) {
        List <String> result = new LinkedList<String>();
        if (placeholderNameKeysOfComponentID != null) {
            for (String componentPlaceholder : placeholderNameKeysOfComponentID){
                int prio = metaData.getGuiPosition(componentPlaceholder);
                int resultIndex = 0;
                for (int i = 0; i < result.size(); i++){
                    int currentPrio = metaData.getGuiPosition(result.get(i));
                    if (currentPrio > prio) {
                        resultIndex = i;
                        break;
                    } else if (currentPrio == prio) {
                        String nameNewPH = metaData.getGuiName(componentPlaceholder);
                        String nameOldPH = metaData.getGuiName(result.get(i));
                        if (nameNewPH != null && nameOldPH != null && nameNewPH.compareToIgnoreCase(nameOldPH) < 0){
                            resultIndex = i;
                            break;
                        }
                    }
                }
                result.add(resultIndex, componentPlaceholder);
            }
        } else {
            result = null;
        }
        return result;
    }
}
