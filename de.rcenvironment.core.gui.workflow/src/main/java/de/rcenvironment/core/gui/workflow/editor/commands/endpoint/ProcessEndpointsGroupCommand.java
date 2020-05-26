/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.commands.endpoint;

import java.util.ArrayList;
import java.util.List;

import de.rcenvironment.core.gui.workflow.editor.properties.Refreshable;
import de.rcenvironment.core.gui.workflow.editor.properties.WorkflowNodeCommand;

/**
 * {@link WorkflowNodeCommand}
 * Group several dynamic endpoint commands in order to act as single
 * undo-redo step.
 *
 * @author Caslav Ilic
 * @author Martin Misiak
 * FIXED 0014347: Primitive commands should not appear on the commandstack.
 * Only the {@link ProcessEndpointsGroupCommand} should. For this {@link #execute()} 
 * does not forward the commands to the {@link #executor}, rather executes them directly.  
 * 
 */
public class ProcessEndpointsGroupCommand extends WorkflowNodeCommand {

    protected WorkflowNodeCommand.Executor executor;

    protected Refreshable[] refreshable;
    
    protected List<WorkflowNodeCommand> commands = new ArrayList<>();

    private boolean executable = true;

    private boolean undoable = false;

    public ProcessEndpointsGroupCommand(WorkflowNodeCommand.Executor executor, Refreshable... refreshable) {
        this.executor = executor;
        this.refreshable = refreshable;
    }

    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute() {
        return executable;
    }

    @Override
    public void execute() {
        if (executable) {
            for (WorkflowNodeCommand command : commands) {
                command.setWorkflowNode(super.getWorkflowNode());
                command.setCommandStack(super.commandStack);
                command.execute();
            }
            executable = false;
            undoable = true;
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    @Override
    public boolean canUndo() {
        return undoable;
    }

    @Override
    public void undo() {
        if (undoable) {
            for (WorkflowNodeCommand command : commands) {
                command.undo();
            }
            executable = true;
            undoable = false;
        }
        if (refreshable != null) {
            for (Refreshable r : refreshable) {
                r.refresh();
            }
        }
    }

    /**
     * Add another command to the group.
     * 
     * @param command 
     */
    public void add(WorkflowNodeCommand command) {
        if (undoable) {
            return;
        }
        commands.add(command);
    }

}
