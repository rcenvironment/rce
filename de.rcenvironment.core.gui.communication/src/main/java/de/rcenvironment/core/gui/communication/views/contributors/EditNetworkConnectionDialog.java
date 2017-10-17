/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to edit an existing network connection.
 *
 * @author Oliver Seebach
 */
public class EditNetworkConnectionDialog extends AbstractNetworkConnectionDialog {

    private static final String COLON = ":";

    private static final String DIALOG_TITLE = "Edit Connection";

    private static final String HINT = "Note: The connection will not be saved.\n"
        + "To create permanent connections, edit the configuration files.\n"
        + "Changes will be applied after restarting the connection.";

    public EditNetworkConnectionDialog(Shell parentShell, String connectionName, String networkContactPointID) {
        super(parentShell, connectionName, networkContactPointID);
        if (this.networkContactPointID.startsWith(ACTIVEMQ_PREFIX)) {
            this.networkContactPointID = this.networkContactPointID.replace(ACTIVEMQ_PREFIX, "");
        }

        host = this.networkContactPointID.substring(0, this.networkContactPointID.indexOf(COLON));

        if (this.networkContactPointID.contains("(")) {
            int index = this.networkContactPointID.indexOf("(");
            String settings = this.networkContactPointID.substring(index);
            settings = settings.replaceAll("[()]", "");

            setSettingsText(settings);

            port = this.networkContactPointID.substring(this.networkContactPointID.indexOf(COLON) + 1, index);

            String network = this.networkContactPointID.substring(0, index);

            if (this.connectionName.equals(network)) {
                activateDefaultName();
            } else {
                deactivateDefaultName();
            }

        } else {
            port = this.networkContactPointID.substring(this.networkContactPointID.indexOf(COLON) + 1);

            if (this.connectionName.equals(this.networkContactPointID)) {
                activateDefaultName();
            } else {
                deactivateDefaultName();
            }

        }

        this.hint = HINT;
    }

    private void setSettingsText(String settings) {

        ConnectionSettings set = new ConnectionSettings();

        try {

            String numberOnly = settings.replaceAll("[^0-9,.]", "");

            if (numberOnly.charAt(numberOnly.length() - 1) == ',') {
                numberOnly = numberOnly.substring(0, numberOnly.length() - 1);
            }

            if (numberOnly.startsWith(COM)) {
                numberOnly = numberOnly.substring(1);
            }

            int indexFirstCom = numberOnly.indexOf(",");
            int indexSecondCom = numberOnly.indexOf(COM, numberOnly.indexOf(COM) + 1);

            String multi = numberOnly.substring(0, indexFirstCom);
            multi = multi.replaceAll("[^0-9.]", "");

            String initialDelay = numberOnly.substring(indexFirstCom, indexSecondCom);
            initialDelay = initialDelay.replaceAll(DECIMAL, "");

            String maxDelay = numberOnly.substring(indexSecondCom);
            maxDelay = maxDelay.replaceAll(DECIMAL, "");

            settingsText = set.createStringForsettings(Integer.parseInt(initialDelay), Integer.parseInt(maxDelay),
                Double.parseDouble(multi));

        } catch (NumberFormatException ex) {
            final double multi = 1.5;
            final int max = 300;
            settingsText = set.createStringForsettings(5, max, multi);
        }

    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

}
