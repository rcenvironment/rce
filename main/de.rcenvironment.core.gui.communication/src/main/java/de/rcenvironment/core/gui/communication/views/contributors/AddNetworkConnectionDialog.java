/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to add an network connection.
 *
 * @author Oliver Seebach
 */
public class AddNetworkConnectionDialog extends AbstractNetworkConnectionDialog {


    private static final String DIALOG_TITLE = "Add Connection";
    private static final String HINT = "Note: The connection will not be saved.\n"
           + "To create permanent connections, edit the configuration files.";
    
    protected AddNetworkConnectionDialog(Shell parentShell) {
        super(parentShell);
        this.hint = HINT;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }
    
}
