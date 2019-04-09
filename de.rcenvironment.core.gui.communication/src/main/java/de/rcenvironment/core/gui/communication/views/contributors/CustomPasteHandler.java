/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.swt.widgets.Text;

/**
 * Paste Host and Port - Network-ConnectionDialog.
 * 
 * @author Oliver Seebach
 * @author Hendrik Abbenhaus
 * @author Lisa Nafeie
 * @author Dominik Schneider
 */

public final class CustomPasteHandler {

    private static final String COLON = ":";

    private static final int MINUS_ONE = -1; // is used to prevent CheckStyle from failing

    private CustomPasteHandler() {}

    /**
     * Splits the given text into host and port strings and sets the corresponding text fields.
     * 
     * @param text The pasted text string.
     * @param host The host text field.
     * @param port The port text field.
     * 
     */
    public static void paste(String text, Text host, Text port) {
        String portString = "";
        String hostString = host.getText().trim();
        if (text.indexOf(COLON) != MINUS_ONE && isValidPort((text.substring(text.indexOf(COLON) + 1)).trim())) {
            portString = (text.substring(text.indexOf(COLON) + 1)).trim();
            hostString = (text.substring(0, text.indexOf(COLON))).trim();
        }

        host.setText(hostString);
        if (!portString.isEmpty()) {
            port.setText(portString);
        }

        port.setFocus();
        port.setSelection(portString.length());

    }

    /**
     * Checks the given string to be a valid port.
     * 
     * @param text The string representing a port.
     * @return True, if valid.
     */
    public static boolean isValidPort(String text) {
        final int maxPort = 65535;
        try {
            int portNum = Integer.valueOf(text);
            if (portNum <= 0 || portNum > maxPort) {
                return false;
            }

        } catch (NumberFormatException ex) {
            if (!text.equals("")) {
                return false;
            }
        }
        return true;
    }

}
