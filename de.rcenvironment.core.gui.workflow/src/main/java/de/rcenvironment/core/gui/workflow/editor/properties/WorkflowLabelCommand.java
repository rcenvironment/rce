/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.properties;

import de.rcenvironment.core.component.workflow.model.api.WorkflowLabel;


/**
 * 
 * A command requesting access to {@link WorkflowLabel} data.
 *
 * @author Marc Stammerjohann
 */
public abstract class WorkflowLabelCommand extends WorkflowCommand {

    private WorkflowLabel workflowLabel;

    final void setWorkflowLabel(final WorkflowLabel workflowLabel) {
        this.workflowLabel = workflowLabel;
    }

    protected WorkflowLabel getWorkflowLabel() {
        return workflowLabel;
    }

    /**
     * An executor capable of handling {@link WorkflowLabelCommand}s.
     * 
     * @author Marc Stammerjohann
     */
    public interface Executor {

        /**
         * Executes the given {@link WorkflowLabelCommand}.
         * 
         * @param command the {@link WorkflowLabelCommand} to execute.
         */
        void execute(WorkflowLabelCommand command);

    }
}
