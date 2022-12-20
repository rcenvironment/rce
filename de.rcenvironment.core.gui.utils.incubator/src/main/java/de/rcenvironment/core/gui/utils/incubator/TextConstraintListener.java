/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * Listener for a {@link Text} Text widget, which limits the text length of a text field.
 * 
 * @author Tim Rosenbach
 */
public class TextConstraintListener implements VerifyListener {

    private final int maxLength;

    public TextConstraintListener(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void verifyText(VerifyEvent e) {

        Text text = (Text) e.getSource();
        String oldS = text.getText();
        String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);

        boolean valid = true;
        if (oldS.length() >= maxLength) {
            valid = false;
        }

        if (oldS.length() > newS.length()) {
            valid = true;
        }

        e.doit = valid;
    }
}
