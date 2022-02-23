/*
 * Copyright 2006-2022 DLR, Germany
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
 */
public class EditSshConnectionDialog extends AbstractSshConnectionDialog {

    private static final String DIALOG_TITLE = "Edit SSH Connection";

    private static final String HINT = "Note: The connection will not be saved.\n"
        + "To create permanent connections, edit the configuration files.\n"
        + "Changes will be applied after restarting the connection.";

    public EditSshConnectionDialog(Shell parentShell, String connectionName, String host, int port, String username,
        String keyfileLocation, boolean usePassphrase, boolean storePassphrase,
        boolean connectImmediately, boolean autoRetry) {
        super(parentShell, connectionName, host, port, username, keyfileLocation, usePassphrase, storePassphrase, connectImmediately,
            autoRetry);
        this.hint = HINT;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

    protected void setPassphrase(String password) {
        this.passphrase = password;
    }

}
