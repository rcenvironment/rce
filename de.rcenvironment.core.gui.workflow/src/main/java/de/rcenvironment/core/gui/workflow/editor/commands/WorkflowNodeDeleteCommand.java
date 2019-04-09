/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;

import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;


/**
 * Command to delete a WorkflowNode.
 *
 * @author Heinrich Wendel
 */
public class WorkflowNodeDeleteCommand extends Command {
    
    /** The parent. **/
    private WorkflowDescription model;
    
    /** The child. **/
    private List<WorkflowNode> nodes;
    
    /** Connections of this node for redo. */
    private List<Connection> connections = new ArrayList<Connection>();
    
    /**
     * Constructor.
     * 
     * @param model The parent.
     * @param node The child.
     */
    public WorkflowNodeDeleteCommand(WorkflowDescription model, List<WorkflowNode> nodes) {
        this.model = model;
        this.nodes = nodes;
    }
    
    @Override
    public void execute() {
        redo();
    }
    
    @Override
    public void redo() {
        connections = model.removeWorkflowNodesAndRelatedConnections(nodes);
    }
    
    @Override
    public void undo() {
        model.addWorkflowNodesAndConnections(nodes, connections);
    }
    
}
