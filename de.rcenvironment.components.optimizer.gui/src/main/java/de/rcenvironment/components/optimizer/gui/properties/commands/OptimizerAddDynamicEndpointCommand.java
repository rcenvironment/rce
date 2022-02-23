/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties.commands;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDefinition;
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

    private static final String DESIGN_VARIABLE = "Design";

    private final EndpointType direction;

    private final boolean hasGradient;

    private final Map<String, String> gradientMetadata = new HashMap<>();

    public OptimizerAddDynamicEndpointCommand(EndpointType direction, String name, String id, DataType type,
        Map<String, String> metaData, Refreshable... refreshable) {
        super(direction, id, name, type, metaData, refreshable);
        this.direction = direction;
        hasGradient = Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.HAS_GRADIENT));
    }

    @Override
    public void execute() {
        super.execute();
        EndpointDescription endpoint = endpointDescManager.getEndpointDescription(name);
        final WorkflowNode workflowNode = getWorkflowNode();
        for (String key : metaData.keySet()) {
            if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                gradientMetadata.put(key, metaData.get(key));
            } else {
                gradientMetadata.put(key, "-");
            }
            gradientMetadata.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                EndpointDefinition.InputExecutionContraint.Required.name());
        }
        switch (direction) {
        case INPUT:
            if (hasGradient) {
                for (EndpointDescription e : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (e.getDynamicEndpointIdentifier().equals(DESIGN_VARIABLE)) {
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
                        gradientMetadata.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                            EndpointDefinition.InputExecutionContraint.Required.name());
                    }

                    workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_GRADIENTS,
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), name),
                        type, gradientMetadata);
                }
            }
            Map<String, String> optimalMetaData = new HashMap<>();
            optimalMetaData.putAll(metaData);

            endpointDescManager.addDynamicEndpointDescription(OptimizerComponentConstants.ID_OPTIMA, name
                + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX, type,
                optimalMetaData);
            if ((metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && metaData.get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                    && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                Map<String, String> startValueMetaData = new HashMap<>();
                startValueMetaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                    EndpointDefinition.InputExecutionContraint.Required.name());
                workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                    name + OptimizerComponentConstants.STARTVALUE_SIGNATURE, type,
                    startValueMetaData);
            }
            if ((endpoint.getMetaDataValue(OptimizerComponentConstants.META_USE_STEP) != null
                && !endpoint.getMetaDataValue(OptimizerComponentConstants.META_USE_STEP).isEmpty())) {
                if (endpoint.getMetaDataValue(OptimizerComponentConstants.META_USE_UNIFIED_STEP) != null
                    && !Boolean.parseBoolean(endpoint.getMetaDataValue(OptimizerComponentConstants.META_USE_UNIFIED_STEP))) {
                    Map<String, String> stepValueMetaData = new HashMap<>();
                    stepValueMetaData.put(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT,
                        EndpointDefinition.InputExecutionContraint.Required.name());
                    workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                        name + OptimizerComponentConstants.STEP_VALUE_SIGNATURE, type, stepValueMetaData);
                }
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
                    if (e.getDynamicEndpointIdentifier().equals(DESIGN_VARIABLE)) {
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
            if ((metaData.get(OptimizerComponentConstants.META_USE_STEP) != null
                && !metaData.get(OptimizerComponentConstants.META_USE_STEP).isEmpty())) {
                if (metaData.get(OptimizerComponentConstants.META_USE_UNIFIED_STEP) != null
                    && !Boolean.parseBoolean(metaData.get(OptimizerComponentConstants.META_USE_UNIFIED_STEP))) {
                    workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                        name + OptimizerComponentConstants.STEP_VALUE_SIGNATURE);
                }
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
