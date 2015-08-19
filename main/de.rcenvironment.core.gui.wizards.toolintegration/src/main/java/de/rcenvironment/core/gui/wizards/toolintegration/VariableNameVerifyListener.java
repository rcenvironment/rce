/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.wizards.toolintegration;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Verifies that endpoint names only contain characters a..z, A..Z, 0..9, _ or blank.
 * 
 * @author Sascha Zur
 */
class VariableNameVerifyListener implements Listener {

    @Override
    public void handleEvent(Event arg0) {
        String string = arg0.text;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')
                && !(c == '_')) {
                arg0.doit = false;
                return;
            }
        }
    }
}
