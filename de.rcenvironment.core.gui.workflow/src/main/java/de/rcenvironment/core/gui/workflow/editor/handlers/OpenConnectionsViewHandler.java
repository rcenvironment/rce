/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
