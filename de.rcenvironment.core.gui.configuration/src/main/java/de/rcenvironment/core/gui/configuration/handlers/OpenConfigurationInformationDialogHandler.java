/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.configuration.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

import de.rcenvironment.core.gui.configuration.ConfigurationInformationDialog;

/**
 * Handler that opens a dialog with information about the configuration.
 * 
 * @author Oliver Seebach
 *
 */
public class OpenConfigurationInformationDialogHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        ConfigurationInformationDialog dialog = new ConfigurationInformationDialog(Display.getCurrent().getActiveShell());
        dialog.open();

        return null;
    }

}
