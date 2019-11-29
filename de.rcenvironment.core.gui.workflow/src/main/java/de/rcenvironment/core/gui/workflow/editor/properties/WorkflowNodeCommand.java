/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;

/**
 * A command requesting access to {@link WorkflowNode} data.
 * 
 * @author Christian Weiss
 */
public abstract class WorkflowNodeCommand extends WorkflowCommand {

    private String label;

    private WorkflowNode workflowNode;

    protected void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Returns the label.
     * 
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    public final void setWorkflowNode(final WorkflowNode workflowNode) {
        this.workflowNode = workflowNode;
    }

    protected WorkflowNode getWorkflowNode() {
        return workflowNode;
    }

    protected ComponentInstanceProperties getProperties() {
        if (commandStack == null || workflowNode == null) {
            throw new IllegalStateException("Property input not set");
        }
        return workflowNode;
    }

    /**
     * An executor capable of handling {@link WorkflowNodeCommand}s.
     * 
     * @author Christian Weiss
     */
    public interface Executor {

        /**
         * Executes the given {@link WorkflowNodeCommand}.
         * 
         * @param command the {@link WorkflowNodeCommand} to execute.
         */
        void execute(WorkflowNodeCommand command);

    }

}
