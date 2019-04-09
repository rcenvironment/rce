/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */


package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;

/**
 * Handler to to select "connectionTool" and focus worklfowEditor.
 *
 * @author Jascha Riedel
 */

public class SelectConnectionToolHandler extends AbstractHandler {

    private WorkflowEditor editor;
    
    private PaletteViewer paletteViewer;
    
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        
        final IWorkbenchPart activePart = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activePart instanceof WorkflowEditor) {
            editor = (WorkflowEditor) activePart;
            this.paletteViewer = editor.getPaletteViewer();
            
            editor.setFocus();
            switchToConnectionTool();
        }     
        return null;
    }
    
    
    private void switchToConnectionTool() {
        if (paletteViewer != null && !isConnectionToolSelected()) {
            for (Object paletteGroupObject : paletteViewer.getPaletteRoot().getChildren()) {
                if (paletteGroupObject instanceof PaletteGroup) {
                    PaletteGroup paletteGroup = (PaletteGroup) paletteGroupObject;
                    for (Object paletteEntryObject : paletteGroup.getChildren()) {
                        if (paletteEntryObject instanceof ConnectionCreationToolEntry){
                            paletteViewer.setActiveTool((ConnectionCreationToolEntry) paletteEntryObject);
                        }
                    }
                }
            }
        }
    }
    
    private boolean isConnectionToolSelected(){
        return (paletteViewer.getActiveTool() instanceof ConnectionCreationToolEntry);
    }
}
