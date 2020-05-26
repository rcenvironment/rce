/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.gef.commands.CommandStack;

/**
 * 
 * This class implements a composite of two {@link WorkflowCommand}. Use this class e.g. if two commands are to be undone at the same time.
 *
 * @author Kathrin Schaffert
 */

class CompositeCommand extends WorkflowCommand {

    private boolean done = false;

    private final WorkflowCommand command1;

    private final WorkflowCommand command2;

    CompositeCommand(WorkflowCommand command1, WorkflowCommand command2) {
        super();
        this.command1 = command1;
        this.command2 = command2;
    }
    
    @Override
    public void setCommandStack(final CommandStack commandStack) {
        super.setCommandStack(commandStack);
        command1.setCommandStack(commandStack);
        command2.setCommandStack(commandStack);
    }

    public WorkflowCommand getCommand1() {
        return command1;
    }

    public WorkflowCommand getCommand2() {
        return command2;
    }

    public CommandStack getCommandStack() {
        return super.commandStack;
    }

    @Override
    public void initialize() {
        command1.initialize();
        command2.initialize();
    }

    @Override
    public boolean canExecute() {
        return !canUndo();
    }

    @Override
    public boolean canUndo() {
        return done;
    }

    @Override
    public void redo() {
        execute();
    }

    @Override
    public void execute() {
        try {
            command1.execute();
            command2.execute();
        } finally {
            done = true;
        }
    }

    @Override
    public void undo() {
        command2.undo();
        command1.undo();
        done = false;
    }
}
