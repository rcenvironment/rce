/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import java.util.List;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.gui.workflow.editor.commands.WorkflowNodeRenameCommand;
import de.rcenvironment.core.gui.workflow.parts.WorkflowNodePart;

/**
 * Handles paste part of copy/cut & paste.
 * 
 * @author Doreen Seider
 */
public class WorkflowNodeRenameHandler extends AbstractWorkflowNodeEditHandler {

    @Override
    void edit() {
        @SuppressWarnings("rawtypes") List selection = viewer.getSelectedEditParts();
        if (selection.get(0) instanceof WorkflowNodePart) {
            WorkflowNodePart part = (WorkflowNodePart) selection.get(0);
            WorkflowNode wn = (WorkflowNode) part.getModel();
            String oldName = wn.getName();
            commandStack.execute(new WorkflowNodeRenameCommand(wn, (WorkflowDescription) viewer.getContents().getModel()));
            if (wn.getName().equals(oldName)) {
                commandStack.undo();
            }
        }
    }

}
