/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties.commands;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.AddDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Adds a input to a Optimizer-{@link WorkflowNode}.
 * 
 * @author Sascha Zur
 */
public class OptimizerAddDynamicEndpointCommand extends AddDynamicEndpointCommand {

    private final EndpointType direction;

    private final boolean hasGradient;

    private final Map<String, String> gradientMetadata = new HashMap<String, String>();

    public OptimizerAddDynamicEndpointCommand(EndpointType direction, String name, String id, DataType type,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, name, type, metaData, refreshable);
        this.direction = direction;
        hasGradient = Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.HAS_GRADIENT));
    }

    @Override
    public void execute() {
        super.execute();
        final WorkflowNode workflowNode = getWorkflowNode();
        for (String key : metaData.keySet()) {
            if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                gradientMetadata.put(key, metaData.get(key));
            } else {
                gradientMetadata.put(key, "-");
            }
        }
        switch (direction) {
        case INPUT:
            if (hasGradient) {
                for (EndpointDescription e : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!e.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                        endpointDescManager.addDynamicEndpointDescription(OptimizerComponentConstants.ID_GRADIENTS,
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, e.getName()), e.getDataType(),
                            gradientMetadata);
                    }
                }
            }
            OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(name, type, metaData, workflowNode);
            break;
        case OUTPUT:
            for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                if (function.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT) != null
                    && Boolean.parseBoolean(function.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT))) {
                    for (String key : metaData.keySet()) {
                        if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                            gradientMetadata.put(key, metaData.get(key));
                        } else {
                            gradientMetadata.put(key, "-");
                        }
                    }
                    workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_GRADIENTS,
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name),
                        type, gradientMetadata);
                }
            }
            endpointDescManager.addDynamicEndpointDescription(OptimizerComponentConstants.ID_OPTIMA, name
                + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX, type,
                metaData);
            if ((metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && metaData.get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                    name + OptimizerComponentConstants.STARTVALUE_SIGNATURE, type,
                    new HashMap<String, String>());
            }
            OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(name, type, metaData, workflowNode);
            break;
        default:
            throw new RuntimeException();
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    @Override
    public void undo() {
        super.undo();
        final WorkflowNode workflowNode = getWorkflowNode();
        switch (direction) {
        case INPUT:
            if (hasGradient) {
                for (EndpointDescription e : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!e.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                        endpointDescManager.removeDynamicEndpointDescription(
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, e.getName()));
                    }
                }
            }
            OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(name, metaData, workflowNode);
            break;
        case OUTPUT:
            for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                if (!workflowNode.getInputDescriptionsManager().isValidEndpointName(
                    OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name))) {
                    workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name));
                }
            }
            endpointDescManager.removeDynamicEndpointDescription(name + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX);
            if ((metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && metaData.get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                    name + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
            }
            OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(name, metaData, workflowNode);
            break;
        default:
            throw new RuntimeException();
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

}
