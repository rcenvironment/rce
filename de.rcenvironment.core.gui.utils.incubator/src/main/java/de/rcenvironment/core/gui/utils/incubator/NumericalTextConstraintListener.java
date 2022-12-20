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
 * Listener for a {@link Text} Text widget, which prohibits the use of letters oder symbols in the text field, if there shall only be float
 * or integer input.
 * 
 * @author Sascha Zur
 * @author Jascha Riedel (#14005)
 * @author Kathrin Schaffert (bug fixes, refactoring)
 */
public class NumericalTextConstraintListener implements VerifyListener {

    protected static final String SMALL_E = "e";

    protected static final String E = "E";

    private static final String MINUS = "-";

    private static final String DOT = ".";

    private final int function;

    public NumericalTextConstraintListener(int function) {
        this.function = function;
    }

    @Override
    public void verifyText(VerifyEvent e) {

        Text text = (Text) e.getSource();
        String oldS = text.getText();
        String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);

        boolean valid = true;
        if ((function & WidgetGroupFactory.ONLY_FLOAT) > 0) {
            valid &= checkIfInputIsFloat(newS);
        }

        if ((function & WidgetGroupFactory.ONLY_INTEGER) > 0) {
            valid &= checkIfInputIsInt(newS);
        }

        if ((function & WidgetGroupFactory.GREATER_ZERO) > 0 && valid) {
            valid &= checkIfGreaterZero(newS);
        }

        if ((function & WidgetGroupFactory.GREATER_OR_EQUAL_ZERO) > 0 && valid) {
            valid &= checkIfGreaterOrEqualZero(newS);
        }

        if ((function & WidgetGroupFactory.GREATER_OR_EQUAL_ONE) > 0 && valid) {
            valid &= checkIfGreaterOrEqualOne(newS);
        }

        // allow leading dot
        if ((newS.equals(DOT) || newS.equals(MINUS + DOT))
            && ((function & WidgetGroupFactory.ONLY_FLOAT) > 0) && ((function & WidgetGroupFactory.GREATER_OR_EQUAL_ONE) <= 0)) {
            valid = true;
        }

        // Allow deleting text entry
        if (newS.isEmpty() && !oldS.isEmpty()) {
            valid = true;
        }

        // Allow minus operation if not >= 0
        if (newS.equals(MINUS) && !((function & (WidgetGroupFactory.GREATER_OR_EQUAL_ZERO | WidgetGroupFactory.GREATER_ZERO
            | WidgetGroupFactory.GREATER_OR_EQUAL_ONE)) > 0)) {
            valid = true;
        }

        // Allow minus operation for exponent
        if (newS.endsWith(MINUS) && (oldS.endsWith(SMALL_E) || oldS.endsWith(E))) {
            valid = true;
        }

        // Check for new exponent entry
        if ((function & WidgetGroupFactory.ONLY_FLOAT) > 0 && !(oldS.contains(E) || oldS.contains(SMALL_E) || oldS.endsWith("."))
            && (newS.endsWith(E) || newS.endsWith(SMALL_E))) {
            valid = checkIfInputIsFloat(oldS);
        }
        // Allow deleting after "E"
        if ((function & WidgetGroupFactory.ONLY_FLOAT) > 0 && oldS.length() > newS.length()
            && (newS.endsWith(E) || newS.endsWith(SMALL_E))) {
            valid = checkIfInputIsFloat(oldS);
        }

        // Allow removing if expononent and minus are at the end
        if (checkIfInputIsFloat(oldS) && (newS.endsWith(E + MINUS) || newS.endsWith(SMALL_E + MINUS)) && oldS.length() > newS.length()) {
            valid = true;
        }

        // allow removing minus before exponent
        if ((oldS.endsWith(E + MINUS) || oldS.endsWith(SMALL_E + MINUS) && (newS.endsWith(E) || newS.endsWith(SMALL_E)))) {
            valid = true;
        }
        // disallow empty chars
        if (e.text.contains(" ") || (e.text.endsWith("d") || e.text.endsWith("D") || e.text.endsWith("f") || e.text.endsWith("F"))) {
            valid = false;
        }
        e.doit = valid;
    }

    private boolean checkIfGreaterOrEqualOne(String newS) {
        try {
            float result = Float.parseFloat(newS);
            return result >= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkIfGreaterOrEqualZero(String newS) {
        try {
            float result = Float.parseFloat(newS);
            return result >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkIfGreaterZero(String newS) {
        try {
            float result = Float.parseFloat(newS);
            return result > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkIfInputIsInt(String newS) {
        try {
            Long.parseLong(newS);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean checkIfInputIsFloat(String newS) {
        try {
            if ((String.valueOf(Double.parseDouble(newS)).equals("Infinity"))
                || (String.valueOf(Double.parseDouble(newS)).equals("-Infinity"))) {
                return false;
            }
            Float.parseFloat(newS);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
