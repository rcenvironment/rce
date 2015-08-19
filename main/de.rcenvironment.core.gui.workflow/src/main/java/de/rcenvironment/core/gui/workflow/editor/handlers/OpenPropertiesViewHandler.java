/*
 * Copyright (C) 2006-2014 DLR, Germany
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
 * Opens the properties tab of the selected component.
 *
 * @author Oliver Seebach
 */
public class OpenPropertiesViewHandler extends AbstractWorkflowNodeEditHandler {

    @Override
    void edit() {
        try {
            IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().
                    getActivePage().showView("org.eclipse.ui.views.PropertySheet");
            view.setFocus();
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        } 
    }

}
