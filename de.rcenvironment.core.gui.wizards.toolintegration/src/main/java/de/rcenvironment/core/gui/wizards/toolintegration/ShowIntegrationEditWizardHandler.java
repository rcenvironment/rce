/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.component.model.impl.ToolIntegrationConstants;

/**
 * Opens the {@link ToolIntegrationWizard} as edit wizard.
 * 
 * @author Sascha Zur
 */
public class ShowIntegrationEditWizardHandler extends AbstractHandler {

    private Optional<String> toolname;

    public ShowIntegrationEditWizardHandler() {
        this.toolname = Optional.empty();
    }

    public ShowIntegrationEditWizardHandler(String toolname) {
        this.toolname = Optional.of(toolname);
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        Display display = getDisplay();

        final WizardDialog wizardDialog = createWizardDialog(display);

        wizardDialog.setBlockOnOpen(false);
        wizardDialog.open();

        return null;
    }

    private WizardDialog createWizardDialog(Display display) {
        if (toolname.isPresent()) {
            final IWizard integrationWizard = new ToolIntegrationWizard(true, ToolIntegrationConstants.EDIT_WIZARD, toolname.get());
            return 
                ToolIntegrationWizardDialog.createIntegrationEditWizardWithPreselectedTool(display.getActiveShell(), integrationWizard);
        } else {
            final IWizard integrationWizard = new ToolIntegrationWizard(true, ToolIntegrationConstants.EDIT_WIZARD);
            return 
                ToolIntegrationWizardDialog.createIntegrationEditWizardWithoutPreselectedTool(display.getActiveShell(), integrationWizard);
        }
    }

    private Display getDisplay() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }

}
