/*
 * Copyright 2006-2022 DLR, Germany
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
 * @author Kathrin Schaffert (#17494, #17714, #17951)
 */
public class EditNetworkConnectionDialog extends AbstractNetworkConnectionDialog {

    protected static final String ERROR_MESSAGE = "One of the configured connection settings was invalid and was not passed to the dialog.";

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
            if (attributes.containsKey(AUTO_RETRY)) {
                connectionSettings.setAutoRetry(Boolean.valueOf(attributes.get(AUTO_RETRY)));
            }
            if (attributes.containsKey(USE_DEFAULT_SETTINGS)) {
                connectionSettings.setUseDefaultSettings(Boolean.valueOf(attributes.get(USE_DEFAULT_SETTINGS)));
            }
            connectionSettings.setUseDefaultSettings(
                !attributes.containsKey(AUTO_RETRY_INITIAL_DELAY_STR) && !attributes.containsKey(AUTO_RETRY_MAXI_DELAY_STR)
                    && !attributes.containsKey(AUTO_RETRY_DELAY_MULTIPL));

            // if any configuration is missing, set error message
            if (!attributes.containsKey(AUTO_RETRY_INITIAL_DELAY_STR) || !attributes.containsKey(AUTO_RETRY_MAXI_DELAY_STR)
                || !attributes.containsKey(AUTO_RETRY_DELAY_MULTIPL)) {
                errorMessage = ERROR_MESSAGE;
            }
            if (attributes.containsKey(AUTO_RETRY_INITIAL_DELAY_STR)
                && checkIfInputIsPositiveLong(attributes.get(AUTO_RETRY_INITIAL_DELAY_STR))) {
                connectionSettings.setAutoRetryInitialDelay(
                    Long.valueOf(attributes.get(AUTO_RETRY_INITIAL_DELAY_STR)));
            } else {
                // if initial delay is not an integer, set error message and leave field empty (set to zero)
                connectionSettings.setAutoRetryInitialDelay(0);
                errorMessage = ERROR_MESSAGE;
            }
            if (attributes.containsKey(AUTO_RETRY_MAXI_DELAY_STR)
                && checkIfInputIsPositiveLong(attributes.get(AUTO_RETRY_MAXI_DELAY_STR))) {
                connectionSettings
                    .setAutoRetryMaximumDelay(Long.valueOf(attributes.get(AUTO_RETRY_MAXI_DELAY_STR)));
            } else {
                // if maximum delay is not an integer, set error message and leave field empty (set to zero)
                connectionSettings.setAutoRetryMaximumDelay(0);
                errorMessage = ERROR_MESSAGE;
            }
            if (attributes.containsKey(AUTO_RETRY_DELAY_MULTIPL)
                && checkIfInputIsValidMultiplier(attributes.get(AUTO_RETRY_DELAY_MULTIPL))) {
                connectionSettings
                    .setAutoRetryDelayMultiplier(Double.valueOf(attributes.get(AUTO_RETRY_DELAY_MULTIPL)));
            } else {
                // if delay multiplier is < 1.0, set error message and leave field empty (set to zero)
                // Note: delay multiplier is always a valid double > see #17993
                // K.Schaffert, 23.11.2022
                connectionSettings.setAutoRetryDelayMultiplier(0);
                errorMessage = ERROR_MESSAGE;
            }
            if (Boolean.FALSE.equals(Boolean.valueOf(attributes.get(AUTO_RETRY)))) {
                // if autoRetry is not true, remove error message
                errorMessage = null;
            }
        } catch (NumberFormatException ex) {
            // should never happen
            throw new NumberFormatException(
                "The connection settings format is incorrect. The edit dialog cannot be displayed." + ex.toString());
        }
    }

    private boolean checkIfInputIsPositiveLong(String newS) {
        try {
            return Long.parseLong(newS) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkIfInputIsValidMultiplier(String input) {
        return Double.valueOf(input) >= 1.0 && !input.equals("Infinity") && !input.equals("-Infinity");
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DIALOG_TITLE);
    }

}
