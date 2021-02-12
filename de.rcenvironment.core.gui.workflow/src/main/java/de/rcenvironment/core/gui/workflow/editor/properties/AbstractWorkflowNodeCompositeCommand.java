/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Abstract common base for {@link WorkflowNodeCommand}s composed of several {@link WorkflowNodeSimpleCommand}s.
 *
 * @author Christian Weiss
 */
public abstract class AbstractWorkflowNodeCompositeCommand extends AbstractWorkflowNodeCommand {

    private final List<WorkflowNodeSimpleCommand> commands = new LinkedList<WorkflowNodeSimpleCommand>();
    
    public AbstractWorkflowNodeCompositeCommand(final WorkflowNodeSimpleCommand... commands) {
        if (commands.length == 0) {
            throw new IllegalArgumentException();
        }
        this.commands.clear();
        for (final WorkflowNodeSimpleCommand command : commands) {
            this.commands.add(command);
        }
    }

    public AbstractWorkflowNodeCompositeCommand(final Collection<WorkflowNodeSimpleCommand> commands) {
        if (commands.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.commands.clear();
        this.commands.addAll(commands);
    }

    @Override
    public final void execute2() {
        if (canExecute()) {
            final List<WorkflowNodeSimpleCommand> executed = new LinkedList<WorkflowNodeSimpleCommand>();
            try {
                final WorkflowNode workflowNode = getWorkflowNode();
                for (final WorkflowNodeSimpleCommand command : commands) {
                    command.setWorkflowNode(workflowNode);
                    command.execute();
                    executed.add(command);
                }
            } catch (RuntimeException e) {
                for (final WorkflowNodeSimpleCommand command : executed) {
                    try {
                        command.undo();
                    } catch (RuntimeException e2) {
                        // TODO log
                        e2 = null;
                    }
                }
            }
        }
    }
    
    @Override
    public final void undo2() {
        if (canUndo()) {
            for (final WorkflowNodeSimpleCommand command : commands) {
                try {
                    command.undo();
                } catch (RuntimeException e2) {
                    // TODO log
                    e2 = null;
                }
            }
        }
    }
    
    /**
     * Minimal interface for {@link WorkflowNodeCommand}.
     *
     * @author Christian Weiss
     */
    public abstract static class WorkflowNodeSimpleCommand {
        
        private WorkflowNode workflowNode;
        
        private void setWorkflowNode(final WorkflowNode workflowNode) {
            this.workflowNode = workflowNode;
        }
        
        protected WorkflowNode getWorkflowNode() {
            return workflowNode;
        }
        
        /**
         * Executes this command.
         *
         */
        public abstract void execute();
        
        /**
         * Undoes this command.
         *
         */
        public abstract void undo();
        
    }

}
