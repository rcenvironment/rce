/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeDisEnableCommand;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;



/**
 * Handle copy part of copy&paste for nodes, labels and connections.
 *
 * @author Doreen Seider
 */
public class WorkflowNodeDisEnableHandler extends AbstractWorkflowNodeEditHandler {

    void edit() {
        @SuppressWarnings("unchecked")
        Iterator<WorkflowNodePart> iterator = viewer.getSelectedEditParts().iterator();
        Set<WorkflowNode> nodes = new HashSet<>();
        while (iterator.hasNext()) {
            nodes.add((WorkflowNode) ((WorkflowNodePart) iterator.next()).getModel());
        }
        commandStack.execute(new WorkflowNodeDisEnableCommand(nodes));
    }

}
