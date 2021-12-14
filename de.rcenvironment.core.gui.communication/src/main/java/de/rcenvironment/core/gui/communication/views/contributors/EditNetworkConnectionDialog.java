/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.communication.utils.NetworkContactPointUtils;

/**
 * Dialog to edit an existing network connection.
 *
 * @author Oliver Seebach
 * @author Kathrin Schaffert (#17494)
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

            setConnectionSettings(settings);

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

    private void setConnectionSettings(String settings) {

        try {

            Map<String, String> attributes = NetworkContactPointUtils.parseAttributePart(settings);

            if (attributes.containsKey(CONNECT_ON_STARTUP)) {
                connectionSettings.setConnectOnStartup(Boolean.valueOf(attributes.get(CONNECT_ON_STARTUP)));
            }
            if (attributes.containsKey(USE_DEFAULT_SETTINGS)) {
                connectionSettings.setUseDefaultSettings(Boolean.valueOf(attributes.get(USE_DEFAULT_SETTINGS)));
            }
            if (attributes.containsKey(AUTO_RETRY_INITIAL_DELAY_STR)) {
                connectionSettings.setAutoRetryInitialDelay(
                    Integer.valueOf(attributes.get(AUTO_RETRY_INITIAL_DELAY_STR)));
            }
            if (attributes.containsKey(AUTO_RETRY_MAXI_DELAY_STR)) {
                connectionSettings
                    .setAutoRetryMaximumDelay(Integer.valueOf(attributes.get(AUTO_RETRY_MAXI_DELAY_STR)));
            }
            if (attributes.containsKey(AUTO_RETRY_DELAY_MULTIPL)) {
                connectionSettings
                    .setAutoRetryDelayMultiplier(Double.valueOf(attributes.get(AUTO_RETRY_DELAY_MULTIPL)));
            }

        } catch (NumberFormatException ex) {
            // should never happen
            throw new NumberFormatException(
                "The connection settings format is incorrect. The edit dialog cannot be displayed." + ex.toString());
        }

    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

}
