/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.editor.handlers;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;


/**
 * Opens the connection view.
 *
 * @author Oliver Seebach
 * 
 */
public class OpenConnectionsViewHandler extends AbstractWorkflowNodeEditHandler {
    
    public OpenConnectionsViewHandler() { }
       
    @Override
    void edit() {
        viewer.deselectAll();
        try {
            IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().
                    getActivePage().showView("org.eclipse.ui.views.PropertySheet");
            view.setFocus();
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        } 
    }
}
