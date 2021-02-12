/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;


/**
 * Handler to to select "selectionTool" and focus worklfowEditor.
 *
 * @author Jascha Riedel
 */

public class SelectSelectionToolHandler extends AbstractHandler {

    private WorkflowEditor editor;
    
    private PaletteViewer paletteViewer;
    
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        
        final IWorkbenchPart activePart = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activePart instanceof WorkflowEditor) {
            editor = (WorkflowEditor) activePart;
            editor.setFocus();
            this.paletteViewer = editor.getPaletteViewer();
            
            if (paletteViewer != null){
                paletteViewer.setActiveTool(paletteViewer.getPaletteRoot().getDefaultEntry());
            }
            
        }
        
        return null;
    }

}
