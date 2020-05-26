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

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;

/**
 * Command to delete a WorkflowLabel.
 * 
 * @author Sascha Zur
 */
public class WorkflowLabelDeleteCommand extends Command {

    /** The parent. **/
    private final WorkflowDescription model;

    /** The child. **/
    private final List<WorkflowLabel> labels;

    /**
     * Constructor.
     * 
     * @param model The parent.
     * @param label The child.
     */
    public WorkflowLabelDeleteCommand(WorkflowDescription model, List<WorkflowLabel> labels) {
        this.model = model;
        this.labels = labels;
    }

    @Override
    public void execute() {
        redo();
    }

    @Override
    public void redo() {
        model.removeWorkflowLabels(labels);
    }

    @Override
    public void undo() {
        model.addWorkflowLabels(labels);
    }

}
