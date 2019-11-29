/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.swt.widgets.Shell;


/**
 * Dialog for adding SSH connections via GUI.
 *
 * @author Brigitte Boden
 * @author Oliver Seebach
 */
public class AddSshConnectionDialog extends AbstractSshConnectionDialog {
    private static final String DIALOG_TITLE = "Add SSH Connection";
    private static final String HINT = "Note: The connection will not be saved.\n"
           + "To create permanent connections, edit the configuration files.";
    
    protected AddSshConnectionDialog(Shell parentShell) {
        super(parentShell);
        this.hint = HINT;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }
   
}
