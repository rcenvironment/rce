/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.workflowintegration.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.integration.workflowintegration.EditWorkflowIntegrationDialog;

/**
 * Handler to open the "Edit Workflow" dialog via the menu.
 * 
 * @author Kathrin Schaffert
 */
public class EditWorkflowIntegrationDialogHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        Shell shell = Display.getDefault().getActiveShell();
        EditWorkflowIntegrationDialog dialog = new EditWorkflowIntegrationDialog(shell);
        dialog.open();
        return null;
    }
}
