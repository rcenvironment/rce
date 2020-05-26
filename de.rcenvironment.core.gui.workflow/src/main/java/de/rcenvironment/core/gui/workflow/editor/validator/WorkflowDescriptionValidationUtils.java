/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.validator;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessage;
import de.rcenvironment.core.component.validation.api.ComponentValidationMessageStore;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDeleteCommand;

/**
 * Utility class with useful methods when validating a {@link WorkflowDescription}.
 * 
 * @author Jascha Riedel
 */
public final class WorkflowDescriptionValidationUtils {

    private static final ComponentValidationMessageStore MESSAGE_STORE = ComponentValidationMessageStore.getInstance();

    private WorkflowDescriptionValidationUtils() {};

    /**
     * Validates the given {@link WorkflowDescription} and saves the messages in the {@link ComponentValidationMessageStore}.
     * 
     * @param workflowDescription to validate.
     * @param onWorkflowStart Boolean whether to active additional validation steps that are only necessary right before a workflow start.
     * @param cleanWorkflow Cleaning the workflow in this context means, that all disabled and not available components are temporarily
     *        removed from the description. This is necessary since a component does not know anything about the state of the other
     *        components especially those connected which may be disabled or N/A.
     */
    public static void validateWorkflowDescription(WorkflowDescription workflowDescription, boolean onWorkflowStart,
        boolean cleanWorkflow) {

        // This is necessary to make sure the true description isn't changed.
        // Since the validation messages are linked to the componentId this is
        // not a problem
        WorkflowDescription workflowDescriptionClone = workflowDescription.clone();

        if (cleanWorkflow) {
            cleanWorkflowDescription(workflowDescription, workflowDescriptionClone);
        }

        validateWorkflowNodesAndUpdateValidState(workflowDescriptionClone.getWorkflowNodes(), onWorkflowStart);

        // Should not be required anymore as Mantis Issue #0014726 is fixed; seeb_ol, November 23, 2016
        // for (WorkflowNode n : workflowDescription.getWorkflowNodes()) {
        // if (!MESSAGE_STORE.getMessagesByComponentId(n.getIdentifier()).isEmpty()) {
        // n.setValid(false);
        // }
        // }
    }

    private static void cleanWorkflowDescription(WorkflowDescription workflowDescription,
        WorkflowDescription workflowDescriptionClone) {
        removeDisabledNodesAndMarkTargetsInvalid(workflowDescription, workflowDescriptionClone);
        removeNotAvailableNodesAndMarkTargetsInvalid(workflowDescription, workflowDescriptionClone);
    }

    private static void removeDisabledNodesAndMarkTargetsInvalid(WorkflowDescription workflowDescription,
        WorkflowDescription workflowDescriptionClone) {
        List<WorkflowNode> nodesToDelete = new ArrayList<>();
        for (WorkflowNode node : workflowDescriptionClone.getWorkflowNodes()) {
            if (!node.isEnabled()) {
                nodesToDelete.add(node);
                setTargetNodesInvalid(workflowDescription, node);
            }
        }
        new WorkflowNodeDeleteCommand(workflowDescriptionClone, nodesToDelete).execute();
    }

    private static void removeNotAvailableNodesAndMarkTargetsInvalid(WorkflowDescription workflowDescription,
        WorkflowDescription workflowDescriptionClone) {
        List<WorkflowNode> nodesToDelete = new ArrayList<>();
        for (WorkflowNode node : workflowDescriptionClone.getWorkflowNodes()) {
            if (node.getComponentDescription().getIdentifier().startsWith(ComponentUtils.MISSING_COMPONENT_PREFIX)) {
                nodesToDelete.add(node);
                setTargetNodesInvalid(workflowDescription, node);
            }
        }
        new WorkflowNodeDeleteCommand(workflowDescriptionClone, nodesToDelete).execute();
    }

    private static void validateWorkflowNodesAndUpdateValidState(List<WorkflowNode> nodes, boolean onWorkflowStart) {
        for (WorkflowNode node : nodes) {
            // for the node fix above, this must be commented, but it's not the best for the performance
            if (!node.isValid()) {
                validateComponent(node, onWorkflowStart);
            }
        }
    }

    private static void setTargetNodesInvalid(WorkflowDescription workflowDescription, WorkflowNode node) {
        for (Connection connection : workflowDescription.getConnections()) {
            if (connection.getSourceNode().getIdentifierAsObject().equals(node.getIdentifierAsObject())) {
                connection.getTargetNode().setValid(false);
            }
        }
    }

    /**
     * Validates only one {@link WorkflowNode} and adds the validation messages to the current ones.
     * 
     * @param workflowNode to validate
     * @param onWorkflowStart if this validation is on the start of a workflow
     */
    public static void validateComponent(WorkflowNode workflowNode, boolean onWorkflowStart) {
        List<ComponentValidationMessage> messages;
        messages = ValidationSupport.getInstance().validate(workflowNode.getComponentDescription(), onWorkflowStart);
        MESSAGE_STORE.addValidationMessagesByComponentId(workflowNode.getIdentifierAsObject().toString(), messages);
    }

}
