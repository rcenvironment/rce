/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Verifies that endpoint names only contain characters a..z, A..Z, 0..9, underscore or blank. Underscore and blank are parameterized.
 * 
 * @author Sascha Zur
 * @author Oliver Seebach
 * 
 */
public class AlphanumericalTextContraintListener implements Listener {

    private boolean acceptBlank = false;

    private boolean acceptUnderscore = false;

    private final char[] forbiddenCharacters;

    public AlphanumericalTextContraintListener(boolean acceptBlank, boolean acceptUnderscore) {
        this.acceptBlank = acceptBlank;
        this.acceptUnderscore = acceptUnderscore;
        forbiddenCharacters = null;
    }

    public AlphanumericalTextContraintListener(char[] acceptedCharacters) {
        acceptBlank = false;
        acceptUnderscore = false;
        this.forbiddenCharacters = acceptedCharacters;
    }

    @Override
    public void handleEvent(Event event) {
        String string = event.text;
        if (string == null) {
            return;
        }
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            boolean inForbiddenChars = false;
            if (forbiddenCharacters != null) {
                for (char accepted : forbiddenCharacters) {
                    if (accepted == c) {
                        inForbiddenChars = true;
                    }
                }
            }
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')
                && !((c == ' ') && acceptBlank) && !((c == '_') && acceptUnderscore) && inForbiddenChars) {
                event.doit = false;
                return;
            }
        }
    }
}
