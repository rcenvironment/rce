/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;

/**
 * Opens the {@link ToolIntegrationWizard}.
 * 
 * @author Sascha Zur
 */
public class ShowIntegrationWizardHandler extends AbstractHandler {

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final IWizard integrationWizard = new ToolIntegrationWizard(true, ToolIntegrationConstants.NEW_WIZARD);
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        final WizardDialog wizardDialog = ToolIntegrationWizardDialog.createIntegrationWizard(display.getActiveShell(), integrationWizard);
        wizardDialog.setBlockOnOpen(false);
        wizardDialog.open();
        return null;
    }

}
