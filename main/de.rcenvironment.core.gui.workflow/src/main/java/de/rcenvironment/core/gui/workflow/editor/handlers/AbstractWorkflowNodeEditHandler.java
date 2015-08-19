/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;


/**
 * Contains common logic of component handlers.
 *
 * @author Doreen Seider
 */
public abstract class AbstractWorkflowNodeEditHandler extends AbstractHandler {

    protected GraphicalViewer viewer;
    
    protected CommandStack commandStack;
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchPart activePart = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof WorkflowEditor) {
            WorkflowEditor editor = (WorkflowEditor) activePart;            
            viewer = editor.getViewer();
            commandStack = (CommandStack) editor.getAdapter(CommandStack.class);
            edit();
        }
        return null;
    }
    
    abstract void edit();
}
