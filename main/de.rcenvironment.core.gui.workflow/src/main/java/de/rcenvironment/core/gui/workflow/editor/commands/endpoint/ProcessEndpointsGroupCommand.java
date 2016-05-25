/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
                executor.execute(command);
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
