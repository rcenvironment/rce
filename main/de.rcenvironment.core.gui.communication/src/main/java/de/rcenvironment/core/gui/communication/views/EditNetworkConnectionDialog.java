/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.communication.views;

import org.eclipse.swt.widgets.Shell;


/**
 * Dialog to edit an existing network connection.
 *
 * @author Oliver Seebach
 */
public class EditNetworkConnectionDialog extends AbstractNetworkConnectionDialog {


    private static final String DIALOG_TITLE = "Edit Connection";
    private static final String HINT = "Note: The connection will not be saved.\n"
        + "To create permanent connections, edit the configuration files.\n"
        + "Changes will be applied after restarting the connection.";
    
    
    public EditNetworkConnectionDialog(Shell parentShell, String connectionName, String networkContactPointID) {
        super(parentShell);
        this.connectionName = connectionName;
        this.networkContactPointID = networkContactPointID;
        if (this.networkContactPointID.startsWith(ACTIVEMQ_PREFIX)){
            this.networkContactPointID = this.networkContactPointID.replace(ACTIVEMQ_PREFIX, "");
        }
        if (this.connectionName.equals(this.networkContactPointID)){
            activateDefaultName();
        } else {
            deactivateDefaultName();
        }
        
        this.hint = HINT;
    }
 
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }
    
}
