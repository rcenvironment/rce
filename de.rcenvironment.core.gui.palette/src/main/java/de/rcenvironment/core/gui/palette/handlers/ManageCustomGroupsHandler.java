/*
 * Copyright 2021-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.palette.view.dialogs.ManageCustomGroupsAction;

/**
 * Handler for Custom Group menu item.
 *
 * @author Kathrin Schaffert
 * 
 */
public class ManageCustomGroupsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.findView("de.rcenvironment.core.gui.palette.view.PaletteView");
        if (view != null) {
            new ManageCustomGroupsAction((PaletteView) view).run();
        }
        return null;
    }

}
