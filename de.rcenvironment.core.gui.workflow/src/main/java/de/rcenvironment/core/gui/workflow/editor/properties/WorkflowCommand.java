/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.gef.commands.CommandStack;

/**
 * 
 * A command requesting access to the data of a workflow component.
 *
 * @author Marc Stammerjohann
 */
public abstract class WorkflowCommand {

    protected CommandStack commandStack;

    public final void setCommandStack(final CommandStack commandStack) {
        this.commandStack = commandStack;
    }

    /**
     * Performs initialization tasks BEFORE pushing the command on the stack.
     * 
     */
    public abstract void initialize();

    /**
     * Returns, whether the command can be executed.
     * 
     * @return true, if the command can be executed
     */
    public abstract boolean canExecute();

    /**
     * Returns, whether the command can be undone.
     * 
     * @return true, if the command can be undone.
     */
    public abstract boolean canUndo();

    /**
     * Executes the command.
     * 
     */
    public abstract void execute();

    /**
     * Re-executes the command.
     * 
     */
    public void redo() {
        execute();
    }

    /**
     * Undoes the changes performed during {@link #execute())}.
     * 
     */
    public abstract void undo();

}
