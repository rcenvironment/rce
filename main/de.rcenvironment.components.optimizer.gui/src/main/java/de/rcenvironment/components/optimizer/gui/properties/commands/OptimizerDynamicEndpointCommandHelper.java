/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties.commands;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Helper class for OptimizerDynamicEndpointCommands.
 * 
 * @author Sascha Zur
 */
public final class OptimizerDynamicEndpointCommandHelper {

    @Deprecated
    private OptimizerDynamicEndpointCommandHelper() {

    }

    /**
     * Creates the channel name of the gradient for the given function and variable.
     * 
     * @param function : the used function name
     * @param variable : the used variable name
     * @return String "del f / del x"
     */
    public static String createGradientChannelName(String function, String variable) {
        return OptimizerComponentConstants.GRADIENT_DELTA + function + "." + OptimizerComponentConstants.GRADIENT_DELTA + variable;
    }

    /**
     * Adds the start value endpoints for lower and upper bounds if neccesary.
     * 
     * @param name of the endpoint
     * @param type of the endpoint
     * @param metaData of the endpoint
     * @param workflowNode to create the endpoints for
     */
    public static void addLowerAndUpperBoundsEndpoints(String name, DataType type, final Map<String, String> metaData,
        final WorkflowNode workflowNode) {
        if (metaData.get(OptimizerComponentConstants.META_KEY_HAS_BOUNDS) != null
            && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_KEY_HAS_BOUNDS))) {
            Map<String, String> lowerBoundsMetaData = new HashMap<String, String>();
            lowerBoundsMetaData.put(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE, LoopEndpointType.OuterLoopEndpoint.name());
            workflowNode.getInputDescriptionsManager()
                .addDynamicEndpointDescription(
                    OptimizerComponentConstants.ID_STARTVALUES,
                    name + OptimizerComponentConstants.BOUNDS_STARTVALUE_LOWER_SIGNITURE
                        + OptimizerComponentConstants.STARTVALUE_SIGNATURE,
                    type,
                    lowerBoundsMetaData);
            Map<String, String> upperBoundsMetaData = new HashMap<String, String>();
            upperBoundsMetaData.put(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE, LoopEndpointType.OuterLoopEndpoint.name());

            workflowNode.getInputDescriptionsManager()
                .addDynamicEndpointDescription(
                    OptimizerComponentConstants.ID_STARTVALUES,
                    name + OptimizerComponentConstants.BOUNDS_STARTVALUE_UPPER_SIGNITURE
                        + OptimizerComponentConstants.STARTVALUE_SIGNATURE,
                    type,
                    upperBoundsMetaData);
        }
    }

    /**
     * Removes the start value endpoints for lower and upper bounds if neccesary.
     * 
     * @param name of the endpoint
     * @param metaData of the endpoint
     * @param workflowNode to remove the endpoints from
     */
    public static void removeUpperLowerBoundsEndpoints(String name, final Map<String, String> metaData, final WorkflowNode workflowNode) {
        if (metaData.get(OptimizerComponentConstants.META_KEY_HAS_BOUNDS) != null
            && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_KEY_HAS_BOUNDS))) {
            workflowNode.getInputDescriptionsManager()
                .removeDynamicEndpointDescription(name + OptimizerComponentConstants.BOUNDS_STARTVALUE_LOWER_SIGNITURE
                    + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
            workflowNode.getInputDescriptionsManager()
                .removeDynamicEndpointDescription(name + OptimizerComponentConstants.BOUNDS_STARTVALUE_UPPER_SIGNITURE
                    + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
        }
    }
}
