/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for editing SSH uplink connections via GUI.
 *
 * @author Brigitte Boden
 */
public class EditSshUplinkConnectionDialog extends AbstractUplinkConnectionDialog {

    private static final String DIALOG_TITLE = "Edit Uplink Connection";

    private static final String HINT = "Note: The connection will not be saved.\n"
        + "To create permanent connections, edit the configuration files.\n"
        + "Changes will be applied after restarting the connection.";

    public EditSshUplinkConnectionDialog(Shell parentShell, String connectionName, String host, int port, String qualifier, String username,
        String keyfileLocation, boolean usePassphrase, boolean storePassphrase,
        boolean connectImmediately, boolean autoRetry, boolean isGateway) {
        super(parentShell, connectionName, host, port, qualifier, username, keyfileLocation, usePassphrase, storePassphrase,
            connectImmediately, autoRetry, isGateway);
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
