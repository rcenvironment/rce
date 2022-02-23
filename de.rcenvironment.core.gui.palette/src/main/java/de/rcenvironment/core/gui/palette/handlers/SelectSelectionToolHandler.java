/*
 * Copyright 2006-2022 DLR, Germany
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
import org.eclipse.ui.PlatformUI;

import de.rcenvironment.core.gui.palette.view.PaletteView;


/**
 * Handler to to select "selectionTool".
 *
 * @author Jan Flink
 */

public class SelectSelectionToolHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        
        final IViewPart viewPart = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getActivePage().findView("de.rcenvironment.core.gui.palette.view.PaletteView");
        if (viewPart instanceof PaletteView) {
            PaletteView paletteView = (PaletteView) viewPart;
            paletteView.selectSelectionToolNode();
        }
        return null;
    }

}
