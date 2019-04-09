/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.properties;

/**
 * 
 * Abstract common base for {@link WorkflowLabelCommand}s.
 *
 * @author Marc Stammerjohann
 */
public abstract class AbstractWorkflowLabelCommand extends WorkflowLabelCommand {

    private boolean done = false;

    public AbstractWorkflowLabelCommand() {
        // do nothing
    }

    @Override
    public void initialize() {
        // do nothing
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
    public void execute() {
        try {
            execute2();
        } finally {
            done = true;
        }
    }

    protected abstract void execute2();

    @Override
    public void undo() {
        try {
            undo2();
        } finally {
            done = false;
        }
    }

    protected abstract void undo2();

}
