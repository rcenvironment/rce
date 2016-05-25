/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.connections.ConnectionDialogController;

/**
 * Command that opens the connection dialog.
 * 
 * @author Heinrich Wendel
 * @author Oliver Seebach
 * 
 */
public class ConnectionCreateCommand extends Command {

    /** Property that is fired when a WorkflowNode was removed. */
    public static final String PROPERTY_CONNECTIONS = "de.rcenvironment.rce.component.workflow.WorkflowDescriptionsConnections";

    /** The workflow those nodes belong to. */
    private WorkflowDescription workflowDescription;

    /** The source node. **/
    private WorkflowNode sourceWorkflowNode;

    /** The target node. **/
    private WorkflowNode targetWorkflowNode;

    private WorkflowDescription modifiedWorkflowDescription;

    private WorkflowDescription originalWorkflowDescription;

    /**
     * Constructor.
     * 
     * @param model The workflow those nodes belong to.
     * @param sourceNode The source node.
     */
    public ConnectionCreateCommand(WorkflowDescription model, WorkflowNode sourceNode) {
        this.workflowDescription = model;
        this.sourceWorkflowNode = sourceNode;
        originalWorkflowDescription = model.clone();
        modifiedWorkflowDescription = model.clone();
    }

    @Override
    public void execute() {
        ConnectionDialogController dialogControler = new ConnectionDialogController(
            workflowDescription, sourceWorkflowNode, targetWorkflowNode, false);

        if (sourceWorkflowNode != null && targetWorkflowNode != null) {
            if (dialogControler.open() == 1) {
                undo();
            }
        }

        modifiedWorkflowDescription = dialogControler.getWorkflowDescription().clone();
    }

    @Override
    public void undo() {
        boolean connectionsHaveChanged = !(CollectionUtils.isEqualCollection(workflowDescription.getConnections(),
            originalWorkflowDescription.getConnections()));
        if (connectionsHaveChanged) {
            workflowDescription.replaceConnections(originalWorkflowDescription.getConnections());
        }
    }

    @Override
    public void redo() {
        boolean connectionsHaveChanged = !(CollectionUtils.isEqualCollection(workflowDescription.getConnections(),
            originalWorkflowDescription.getConnections()));
        if (connectionsHaveChanged) {
            workflowDescription.replaceConnections(modifiedWorkflowDescription.getConnections());
        }
    }

    /**
     * Sets the target node.
     * 
     * @param target The target node.
     */
    public void setTarget(WorkflowNode target) {
        this.targetWorkflowNode = target;
    }

}
