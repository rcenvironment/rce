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
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.commands.endpoint.EditDynamicEndpointCommand;
import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;

/**
 * Command for editing an endpoint in the optimizer component.
 * 
 * @author Sascha Zur
 */
public class OptimizerEditDynamicEndpointCommand extends EditDynamicEndpointCommand {

    private static final String DASH = "-";

    public OptimizerEditDynamicEndpointCommand(EndpointType direction, String id, EndpointDescription oldDescription,
        EndpointDescription newDescription, boolean executable, boolean undoable, Refreshable... refreshable) {
        super(direction, oldDescription, newDescription, refreshable);
    }

    @Override
    public void execute() {
        final WorkflowNode workflowNode = getWorkflowNode();
        super.execute();

        switch (direction) {
        case INPUT:
            if (hasGradient(oldDesc)) {
                if (hasGradient(newDesc)) {
                    executeBothHaveGradients(workflowNode);
                } else {
                    for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                        if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                            String oldGradientName = OptimizerDynamicEndpointCommandHelper.
                                createGradientChannelName(oldDesc.getName(), variable.getName());
                            workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(oldGradientName);
                        }
                    }
                }

            } else {
                if (hasGradient(newDesc)) {
                    for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                        if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                            String newGradientName = OptimizerDynamicEndpointCommandHelper.
                                createGradientChannelName(newDesc.getName(), variable.getName());
                            Map<String, String> metaData = new HashMap<String, String>();
                            for (String key : newDesc.getMetaData().keySet()) {
                                if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                                    || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                                    || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                                    metaData.put(key, newDesc.getMetaData().get(key));
                                } else {
                                    metaData.put(key, DASH);
                                }
                            }
                            workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(
                                OptimizerComponentConstants.ID_GRADIENTS, newGradientName,
                                variable.getDataType(), metaData);
                        }
                    }
                }
            }

            break;
        case OUTPUT:
            if (!newDesc.getName().equals(oldDesc.getName()) || !newDesc.getDataType().equals(oldDesc.getDataType())) {
                for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!workflowNode.getInputDescriptionsManager().isValidEndpointName(
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), oldDesc.getName()))) {
                        String oldName =
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), oldDesc.getName());
                        String newName =
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), newDesc.getName());
                        EndpointDescription desc = workflowNode.getInputDescriptionsManager().getEndpointDescription(
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), oldDesc.getName()));
                        desc.setName(newName);
                        workflowNode.getInputDescriptionsManager().editDynamicEndpointDescription(oldName, newName, newDesc.getDataType(),
                            desc.getMetaData());
                    }
                }

            }
            if (getWorkflowNode().getOutputDescriptionsManager().getEndpointDescription(
                oldDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX) != null) {
                getWorkflowNode().getOutputDescriptionsManager().removeDynamicEndpointDescription(
                    oldDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX);
            }
            getWorkflowNode().getOutputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_OPTIMA,
                newDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                newDesc.getDataType(), newDesc.getMetaData());

            if ((oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && oldDesc.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                    oldDesc.getName() + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
            }
            if ((newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && newDesc.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                    newDesc.getName() + OptimizerComponentConstants.STARTVALUE_SIGNATURE, newDesc.getDataType(),
                    new HashMap<String, String>());
            }
            break;
        default:
            throw new RuntimeException();
        }
        String oldHasBounds = oldDesc.getMetaDataValue(OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
        String newHasBounds = newDesc.getMetaDataValue(OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
        if (oldHasBounds != null && Boolean.parseBoolean(oldHasBounds)) {
            if (!Boolean.parseBoolean(newHasBounds)) {
                OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(newDesc.getName(), newDesc.getDataType(),
                    newDesc.getMetaData(), workflowNode);
            }
        }
        if (oldHasBounds != null && !Boolean.parseBoolean(oldHasBounds)) {
            if (Boolean.parseBoolean(newHasBounds)) {
                OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(oldDesc.getName(), oldDesc.getMetaData(),
                    workflowNode);
            } else {
                if (!oldDesc.getName().equals(newDesc.getName())) {
                    OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(oldDesc.getName(), oldDesc.getMetaData(),
                        workflowNode);
                    OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(newDesc.getName(), newDesc.getDataType(),
                        newDesc.getMetaData(), workflowNode);
                }
            }
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    private void executeBothHaveGradients(final WorkflowNode workflowNode) {
        if (!oldDesc.getName().equals(newDesc.getName()) || oldDesc.getDataType() != newDesc.getDataType()) {
            for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                    String oldGradientName = OptimizerDynamicEndpointCommandHelper.
                        createGradientChannelName(oldDesc.getName(), variable.getName());
                    String newGradientName = OptimizerDynamicEndpointCommandHelper.
                        createGradientChannelName(newDesc.getName(), variable.getName());
                    EndpointDescription newDesc =
                        workflowNode.getInputDescriptionsManager().getEndpointDescription(oldGradientName);
                    newDesc.setName(newGradientName);
                    newDesc.setDataType(this.newDesc.getDataType());

                    Map<String, String> gradientMetadata = new HashMap<String, String>();
                    for (String key : this.newDesc.getMetaData().keySet()) {
                        if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                            gradientMetadata.put(key, this.newDesc.getMetaData().get(key));
                        } else {
                            gradientMetadata.put(key, DASH);
                        }
                    }
                    newDesc.setMetaData(gradientMetadata);

                    workflowNode.getInputDescriptionsManager().editDynamicEndpointDescription(oldGradientName, newGradientName,
                        newDesc.getDataType(), newDesc.getMetaData());
                }
            }
        }
    }

    private boolean hasGradient(EndpointDescription description) {
        if (description.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT) != null
            && Boolean.parseBoolean(description.getMetaDataValue(OptimizerComponentConstants.HAS_GRADIENT))) {
            return true;
        }
        return false;
    }

    @Override
    public void undo() {
        final WorkflowNode workflowNode = getWorkflowNode();
        super.undo();
        switch (direction) {
        case INPUT:
            if (hasGradient(newDesc)) {
                if (hasGradient(oldDesc)) {
                    undoBothHaveGradients(workflowNode);
                } else {
                    for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                        if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                            String newGradientName = OptimizerDynamicEndpointCommandHelper.
                                createGradientChannelName(newDesc.getName(), variable.getName());
                            workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(newGradientName);
                        }
                    }
                }
            } else {
                if (hasGradient(oldDesc)) {
                    for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                        if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                            String oldGradientName = OptimizerDynamicEndpointCommandHelper.
                                createGradientChannelName(oldDesc.getName(), variable.getName());
                            Map<String, String> metaData = new HashMap<String, String>();
                            for (String key : newDesc.getMetaData().keySet()) {
                                if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                                    || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                                    || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                                    metaData.put(key, newDesc.getMetaData().get(key));
                                } else {
                                    metaData.put(key, DASH);
                                }
                            }
                            workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(
                                OptimizerComponentConstants.ID_GRADIENTS, oldGradientName,
                                oldDesc.getDataType(), metaData);
                        }
                    }
                }
            }
            break;
        case OUTPUT:
            if (!newDesc.getName().equals(oldDesc.getName())) {
                for (EndpointDescription function : workflowNode.getInputDescriptionsManager().getDynamicEndpointDescriptions()) {
                    if (!workflowNode.getInputDescriptionsManager().isValidEndpointName(
                        OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), newDesc.getName()))) {
                        String oldName =
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), oldDesc.getName());
                        String newName =
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), newDesc.getName());
                        EndpointDescription desc = workflowNode.getInputDescriptionsManager().getEndpointDescription(
                            OptimizerDynamicEndpointCommandHelper.createGradientChannelName(function.getName(), newDesc.getName()));
                        desc.setName(oldName);
                        workflowNode.getInputDescriptionsManager().editDynamicEndpointDescription(newName, oldName, desc.getDataType(),
                            desc.getMetaData());
                    }
                }
                if (getWorkflowNode().getOutputDescriptionsManager().getEndpointDescription(
                    newDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX) != null) {
                    getWorkflowNode().getOutputDescriptionsManager().removeDynamicEndpointDescription(
                        newDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX);
                }
                getWorkflowNode().getOutputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_OPTIMA,
                    oldDesc.getName() + OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX,
                    newDesc.getDataType(), newDesc.getMetaData());
            }
            if ((newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && newDesc.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(newDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().removeDynamicEndpointDescription(
                    newDesc.getName() + OptimizerComponentConstants.STARTVALUE_SIGNATURE);
            }
            if ((oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) == null
                && oldDesc.getMetaData().get(OptimizerComponentConstants.META_STARTVALUE).isEmpty())
                || (oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE) != null
                && !Boolean.parseBoolean(oldDesc.getMetaData().get(OptimizerComponentConstants.META_HAS_STARTVALUE)))) {
                workflowNode.getInputDescriptionsManager().addDynamicEndpointDescription(OptimizerComponentConstants.ID_STARTVALUES,
                    oldDesc.getName() + OptimizerComponentConstants.STARTVALUE_SIGNATURE, oldDesc.getDataType(),
                    new HashMap<String, String>());
            }
            break;
        default:
            throw new RuntimeException();
        }
        String oldHasBounds = oldDesc.getMetaDataValue(OptimizerComponentConstants.META_KEY_HAS_BOUNDS);
        String newHasBounds = newDesc.getMetaDataValue(OptimizerComponentConstants.META_KEY_HAS_BOUNDS);

        if (Boolean.parseBoolean(newHasBounds)) {
            if (oldHasBounds != null && !Boolean.parseBoolean(oldHasBounds)) {
                OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(oldDesc.getName(), oldDesc.getDataType(),
                    oldDesc.getMetaData(), workflowNode);
            }
        } else {
            if (oldHasBounds != null && !Boolean.parseBoolean(oldHasBounds)) {
                if (!oldDesc.getName().equals(newDesc.getName())) {
                    OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(newDesc.getName(), newDesc.getMetaData(),
                        workflowNode);
                    OptimizerDynamicEndpointCommandHelper.addLowerAndUpperBoundsEndpoints(oldDesc.getName(), oldDesc.getDataType(),
                        oldDesc.getMetaData(), workflowNode);
                }
            } else {
                OptimizerDynamicEndpointCommandHelper.removeUpperLowerBoundsEndpoints(newDesc.getName(), newDesc.getMetaData(),
                    workflowNode);
            }
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    private void undoBothHaveGradients(final WorkflowNode workflowNode) {
        if (!oldDesc.getName().equals(newDesc.getName()) || oldDesc.getDataType() != newDesc.getDataType()) {
            for (EndpointDescription variable : workflowNode.getOutputDescriptionsManager().getDynamicEndpointDescriptions()) {
                if (!variable.getName().contains(OptimizerComponentConstants.OPTIMUM_VARIABLE_SUFFIX)) {
                    String oldGradientName = OptimizerDynamicEndpointCommandHelper.
                        createGradientChannelName(oldDesc.getName(), variable.getName());
                    String newGradientName = OptimizerDynamicEndpointCommandHelper.
                        createGradientChannelName(newDesc.getName(), variable.getName());
                    EndpointDescription desc =
                        workflowNode.getInputDescriptionsManager().getEndpointDescription(newGradientName);
                    desc.setName(oldGradientName);
                    Map<String, String> gradientMetadata = new HashMap<String, String>();
                    for (String key : this.oldDesc.getMetaData().keySet()) {
                        if (key.equals(OptimizerComponentConstants.METADATA_VECTOR_SIZE)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_DATUM_HANDLING)
                            || key.startsWith(ComponentConstants.INPUT_METADATA_KEY_INPUT_EXECUTION_CONSTRAINT)) {
                            gradientMetadata.put(key, newDesc.getMetaData().get(key));
                        } else {
                            gradientMetadata.put(key, DASH);
                        }
                    }
                    desc.setMetaData(gradientMetadata);
                    workflowNode.getInputDescriptionsManager().editDynamicEndpointDescription(newGradientName, oldGradientName,
                        oldDesc.getDataType(), gradientMetadata);
                }
            }
        }
    }
}
