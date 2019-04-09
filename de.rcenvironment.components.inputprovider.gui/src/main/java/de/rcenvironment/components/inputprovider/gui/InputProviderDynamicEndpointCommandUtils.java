/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.inputprovider.gui;

import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataConstants;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * 
 * Helper class for the Input Provider.
 *
 * @author Marc Stammerjohann
 */
public final class InputProviderDynamicEndpointCommandUtils {

    /**
     * This class has only static methods.
     */
    private InputProviderDynamicEndpointCommandUtils() {
        // only static methods
    }

    /**
     * 
     * Set the output and placeholder name.
     * 
     * @param workflowNode which the output and placeholder name are added to
     * @param type of the output
     * @param name of the output
     */
    public static void setValueName(WorkflowNode workflowNode, DataType type, String name) {
        String placeholder = "${" + name + "}";
        setOutputValues(workflowNode, name, placeholder);
        setOutputValues(workflowNode, name + PlaceholdersMetaDataConstants.DATA_TYPE, getPlaceholderDataType(type));
    }

    private static void setOutputValues(WorkflowNode workflowNode, String name, String value) {
        workflowNode.getConfigurationDescription().setConfigurationValue(name, value);
    }
    
    private static String getPlaceholderDataType(DataType endpointDatatype) {
        String placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_TEXT;
        switch (endpointDatatype) {
        case FileReference:
            placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_FILE;
            break;
        case DirectoryReference:
            placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_DIR;
            break;
        case Boolean:
            placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_BOOL;
            break;
        case Integer:
            placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_INT;
            break;
        case Float:
            placeholderDatatype = PlaceholdersMetaDataConstants.TYPE_FLOAT;
            break;
        default:
            break;
        }
        return placeholderDatatype;
    }

}
