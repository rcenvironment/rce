/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
