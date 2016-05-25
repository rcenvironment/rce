/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.RemoveDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Removes a input to a {@link WorkflowNode}.
 * 
 * @author Sascha Zur
 */

public class OptimizerRemoveDynamicEndpointCommand extends RemoveDynamicEndpointCommand {

    public OptimizerRemoveDynamicEndpointCommand(EndpointType direction,
        List<String> names, String id, Refreshable... refreshable) {
        super(direction, id, names, refreshable);
    }

    @Override
    public void execute() {
        super.execute();
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            final WorkflowNode workflowNode = getWorkflowNode();
            switch (direction) {
            case INPUT:
                for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                        if (!workflowNode.getInputDescriptionsManager().isValidEndpointName(
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, variable.getName()))) {
                            workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                                OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, variable.getName()));
                        }
                    }
                }
                OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(oldDescription.getName(),
                    oldDescription.getMetaData(),
                    workflowNode);
                break;
            case OUTPUT:
                for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!workflowNode.getInputDescriptionsManager().isValidEndpointName(
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name))) {
                        workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name));
                    }
                }
                if (getWorkflowNode().getOutputDescriptionsManager().getEndpointDescription(
                    name + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX) != null) {
                    getWorkflowNode().getOutputDescriptionsManager().removeDynamicEndpointDescription(
                        name + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX);
                }
                if ((oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                    && oldDescription.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                    || (oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                        && !Boolean.parseBoolean(oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                    getWorkflowNode().getInputDescriptionsManager().removeDynamicEndpointDescription(
                        name + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
                }
                if ((oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_STEP) != null
                    && !oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_STEP).isEmpty())) {
                    if (oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_UNIFIED_STEP) != null
                        && !Boolean.parseBoolean(oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_UNIFIED_STEP))) {
                        workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                            name + OptimizerComponentConstants.STEP_VALUE_SIGNATURE);
                    }
                }
                OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(oldDescription.getName(),
                    oldDescription.getMetaData(),
                    workflowNode);
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

    @Override
    public void undo() {
        final WorkflowNode workflowNode = getWorkflowNode();
        super.undo();
        for (String name : names) {
            EndpointDescription oldDescription = oldDescriptions.get(name);
            switch (direction) {
            case INPUT:
                for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                        if (oldDescription.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT) != null
                            && Boolean.parseBoolean(oldDescription.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT))
                            && workflowNode.getInputDescriptionsManager().isValidEndpointName(
                                OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, variable.getName()))) {
                            Map<String, String> gradientData = new HashMap<String, String>();
                            for (String key : oldDescription.getMetaData().keySet()) {
                                if (!key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)) {
                                    gradientData.put(key, "-");
                                } else {
                                    gradientData.put(key, oldDescription.getMetaDataValue(key));
                                }
                            }
                            workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(
                                OptimizerComponentConstants.ID_GRADIENTS,
                                OptimizerDynamicEndpointCommandHelper.createGradientChannelName(name, variable.getName()),
                                variable.getDataType(), gradientData);
                        }
                    }
                }

                OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(oldDescription.getName(),
                    oldDescription.getDataType(),
                    oldDescription.getMetaData(), workflowNode);
                break;
            case OUTPUT:
                for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (function.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT) != null
                        && Boolean.parseBoolean(function.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT))) {
                        workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_GRADIENTS,
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name),
                            oldDescription.getDataType(), new HashMap<String, String>());
                    }

                }
                getWorkflowNode().getOutputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_OPTIMA,
                    name + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                    oldDescription.getDataType(), oldDescription.getMetaData());
                if ((oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                    && oldDescription.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                    || (oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                        && !Boolean.parseBoolean(oldDescription.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                    getWorkflowNode().getInputDescriptionsManager().addDynamicEndpointDescription(
                        OptimizerComponentConstants.ID_STARTVALUES,
                        name + OptimizerComponentConstants.STARTVALUE_SIGNATURE, oldDescription.getDataType(),
                        new HashMap<String, String>());
                }
                if ((oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_STEP) != null
                    && !oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_STEP).isEmpty())) {
                    if (oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_UNIFIED_STEP) != null
                        && !Boolean.parseBoolean(oldDescription.getMetaData().get(OptimizerComponentConstants.META_USE_UNIFIED_STEP))) {
                        Map<String, String> stepValueMetaData = new HashMap<String, String>();
                        stepValueMetaData.put(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE,
                            LoopEndpointType.OuterLoopEndpoint.name());
                        workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                            name + OptimizerComponentConstants.STEP_VALUE_SIGNATURE, oldDescription.getDataType(), stepValueMetaData);
                    }
                }
                OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(oldDescription.getName(),
                    oldDescription.getDataType(),
                    oldDescription.getMetaData(), workflowNode);
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
}
