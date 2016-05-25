/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;

/**
 * Opens the {@link ToolIntegrationWizard} as edit wizard.
 * 
 * @author Sascha Zur
 */
public class ShowIntegrationEditWizardHandler extends AbstractHandler {

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final IWizard integrationWizard = new ToolIntegrationWizard(true, ToolIntegrationConstants.EDIT_WIZRAD_COMMON);
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        final WizardDialog wizardDialog = new ToolIntegrationWizardDialog(display.getActiveShell(), integrationWizard, true);
        wizardDialog.setBlockOnOpen(false);
        wizardDialog.open();
        return null;
    }

}
