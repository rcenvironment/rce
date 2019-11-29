/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import java.util.Map;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;

/**
 * Static utility methods related to {@link WorkflowHostService}, including the {@link NodeProperty}s published and consumed by it.
 * 
 * @author Robert Mischke
 */
// FIXME move to component.workflow bundle when migration is complete
public final class WorkflowHostUtils {

    /**
     * If this node property is set and has the value {@link NodePropertyConstants#VALUE_TRUE}, the publishing node is a "workflow host".
     */
    public static final String KEY_IS_WORKFLOW_HOST = "workflowHost";

    private WorkflowHostUtils() {}

    /**
     * @param property a single {@link NodeProperty}
     * @return if this property represents the "workflow host" setting
     */
    public static boolean isWorkflowHostProperty(NodeProperty property) {
        return KEY_IS_WORKFLOW_HOST.equals(property.getKey());
    }

    /**
     * @param property the property set of a node
     * @return the value of this node's "workflow host" setting
     */
    public static boolean getWorkflowHostPropertyValue(NodeProperty property) {
        // consistency check
        if (!isWorkflowHostProperty(property)) {
            throw new IllegalArgumentException();
        }
        String value = property.getValue();
        if (NodePropertyConstants.VALUE_TRUE.equals(value)) {
            return true;
        } else if (NodePropertyConstants.VALUE_FALSE.equals(value)) {
            return false;
        } else {
            LogFactory.getLog(WorkflowHostUtils.class).error(
                "Invalid property value for " + KEY_IS_WORKFLOW_HOST + ", parsing to 'false': " + value);
            return false;
        }
    }

    /**
     * @param nodeProperties the complete properties of a node
     * @return true if this node is marked as a "workflow host" by its properties
     */
    public static boolean doNodePropertiesIndicateWorkflowHost(Map<String, String> nodeProperties) {
        return NodePropertyConstants.VALUE_TRUE.equals(nodeProperties.get(KEY_IS_WORKFLOW_HOST));
    }

}
