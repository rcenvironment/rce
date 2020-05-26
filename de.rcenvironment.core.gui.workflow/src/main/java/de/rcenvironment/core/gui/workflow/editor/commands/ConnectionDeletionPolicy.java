/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.requests.GroupRequest;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.ConnectionUtils;
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

        // find and set connection(s)
        ConnectionWrapper connectionWrapper = null;
        WorkflowNode sourceNode = null;
        WorkflowNode targetNode = null;
        if (getHost().getModel() instanceof ConnectionWrapper) {
            connectionWrapper = (ConnectionWrapper) getHost().getModel();
            sourceNode = connectionWrapper.getSource();
            targetNode = connectionWrapper.getTarget();
        }
        List<Connection> connections = ConnectionUtils.getConnectionsFromSourceToTarget(sourceNode, targetNode, description);
        for (Connection connection : connections){
            command.addConnectionForDeletion(connection);
        }

        return command;
    }

}
