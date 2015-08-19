/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.parts.ConnectionWrapper;

/**
 * ConnectionEditPolicy required to delete connections from the graphical workflow editor.
 * 
 * @author Oliver Seebach
 */
public class ConnectionDeletionPolicy extends ConnectionEditPolicy {

    @Override
    protected Command getDeleteCommand(GroupRequest groupRequest) {

        Object parent = getHost().getParent().getViewer().getContents().getModel();
        WorkflowDescription description = null;
        if (parent instanceof WorkflowDescription) {
            description = (WorkflowDescription) parent;
        }

        ConnectionDeleteCommand command = new ConnectionDeleteCommand();

        // set description
        command.setOriginalModel(description);

        // find and set connection
        ConnectionWrapper connectionWrapper = null;
        WorkflowNode sourceNode = null;
        WorkflowNode targetNode = null;
        if (getHost().getModel() instanceof ConnectionWrapper) {
            connectionWrapper = (ConnectionWrapper) getHost().getModel();
            sourceNode = connectionWrapper.getSource();
            targetNode = connectionWrapper.getTarget();
        }
        if (sourceNode != null && targetNode != null && description != null) {
            for (Connection connection : description.getConnections()) {
                if ((connection.getSourceNode() == sourceNode && connection.getTargetNode() == targetNode) 
                    || (connection.getSourceNode() == targetNode && connection.getTargetNode() == sourceNode)) {
                    command.addConnectionForDeletion(connection);
                }
            }
        }

        return command;
    }

}
