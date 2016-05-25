/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.gef.NodeEditPart;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDisEnableCommand;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Handle copy part of copy&paste for nodes, labels and connections.
 * 
 * @author Doreen Seider
 */
public class WorkflowNodeDisEnableHandler extends AbstractWorkflowNodeEditHandler {

    @Override
    void edit() {
        @SuppressWarnings("unchecked") Iterator<WorkflowNodePart> iterator = viewer.getSelectedEditParts().iterator();
        Set<WorkflowNode> nodes = new HashSet<>();
        while (iterator.hasNext()) {
            NodeEditPart next = iterator.next();
            if (next.getModel() instanceof WorkflowNode) {
                nodes.add((WorkflowNode) next.getModel());
            }
        }
        commandStack.execute(new WorkflowNodeDisEnableCommand(nodes));
    }

}
