/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;


/**
 * Abstract common base for {@link WorkflowNodeCommand}s.
 *
 * @author Christian Weiss
 */
public abstract class AbstractWorkflowNodeCommand extends WorkflowNodeCommand {

    private boolean done = false;
    
    public AbstractWorkflowNodeCommand() {
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
