/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;

/**
 * 
 * Abstract base class for implementing a property editor for a workflow component.
 *
 * @author Marc Stammerjohann
 */
public abstract class WorkflowPropertySection extends AbstractPropertySection {

    private CommandStack commandStack;

    protected void setCommandStack(CommandStack commandStack) {
        this.commandStack = commandStack;
    }

    protected CommandStack getCommandStack() {
        return commandStack;
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection) {
        super.setInput(part, selection);
        setCommandStack((CommandStack) part.getAdapter(CommandStack.class));
    }

    /**
     * A wrapper class to wrap {@link WorkflowCommand}s in GEF {@link Command}s.
     * 
     * @author Christian Weiss
     * @author Marc Stammerjohann
     */
    protected static class CommandWrapper extends Command {

        /** The backing command, invokations are forwarded to. */
        private final WorkflowCommand command;

        public CommandWrapper(final WorkflowCommand command) {
            this.command = command;
        }

        @Override
        public boolean canExecute() {
            return command.canExecute();
        }

        @Override
        public void execute() {
            command.execute();
        }

        @Override
        public void redo() {
            command.redo();
        }

        @Override
        public boolean canUndo() {
            return command.canUndo();
        }

        @Override
        public void undo() {
            command.undo();
        }
    }

}
