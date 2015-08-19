/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.incubator;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * Listener for a {@link Text} Text widget, which prohibits the use of letters oder symbols in the
 * text field, if there shall only be float or integer input.
 * 
 * @author Sascha Zur
 */
public class NumericalTextConstraintListener implements VerifyListener {

    /**
     * Option if there shall be no restriction.
     */
    public static final int NONE = WidgetGroupFactory.NONE;

    /**
     * Option if a textfield should only allow float inputs (e.g. no letters).
     */
    public static final int ONLY_FLOAT = WidgetGroupFactory.ONLY_FLOAT;

    /**
     * Option if a textfield should only allow integer inputs (e.g. no letters).
     */
    public static final int ONLY_INTEGER = WidgetGroupFactory.ONLY_INTEGER;

    private static final String SMALL_E = "e";

    private static final String MINUS = "-";

    private static final String E = "E";

    private int function;

    public NumericalTextConstraintListener(Text text, int function) {
        this.function = function;
    }

    @Override
    public void verifyText(VerifyEvent e) {

        Text text = (Text) e.getSource();
        // get old text and create new text by using the VerifyEvent.text
        final String oldS = text.getText();
        String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
        boolean isFloat = true;
        if (!newS.isEmpty()) {
            if ((function & WidgetGroupFactory.ONLY_FLOAT) > 0) {
                try {
                    Float.parseFloat(newS);
                } catch (NumberFormatException ex) {
                    isFloat = false;
                }
                if (!isFloat && e.text.length() == 1 && (e.text.contains(SMALL_E) || e.text.contains(E))) {
                    if (!oldS.contains(SMALL_E) && !oldS.contains(E)) {
                        isFloat = true;
                    }
                }
                if (!isFloat && e.text.length() == 1 && (e.text.contains(MINUS) || e.text.contains("."))) {
                    if (!(oldS.length() > 0)) {
                        isFloat = true;
                    }
                    if (e.text.contains(MINUS) && newS.lastIndexOf('-') - 1 >= 0
                        && newS.substring(newS.lastIndexOf('-') - 1, newS.lastIndexOf('-')).equalsIgnoreCase(E)) {
                        isFloat = true;
                    }

                }
                if (!isFloat && newS.length() == 1 && (newS.contains(MINUS) || newS.contains("."))) {
                    isFloat = true;
                }
                if ((newS.length() > 1 && newS.substring(newS.length() - 1).equalsIgnoreCase(E))
                    || (newS.length() > 2 && newS.substring(newS.length() - 2, newS.length() - 1).equalsIgnoreCase(E)
                    && newS.substring(newS.length() - 1).equalsIgnoreCase(MINUS))) {
                    isFloat = true;
                }
            }
            boolean isInt = true;
            if ((function & WidgetGroupFactory.ONLY_INTEGER) > 0) {
                try {
                    Integer.parseInt(newS);
                } catch (NumberFormatException ex) {
                    isInt = false;
                }
                if (!isInt && e.text.length() == 1 && (e.text.contains(MINUS))) {
                    if (!(oldS.length() > 0)) {
                        isInt = true;
                    }
                }
                if (!isInt && newS.length() == 1 && newS.contains(MINUS)) {
                    isInt = true;
                }
            }
            if (!isFloat || !isInt) {
                e.doit = false;
            }
            if (!e.doit && e.text.length() > 1) {
                e.doit = true;
            }
            if ((oldS.contains(E) || oldS.contains(SMALL_E)) && e.text.equalsIgnoreCase(E)) {
                e.doit = false;
            }
            if (e.text.length() > 1) {
                try {
                    if ((function & WidgetGroupFactory.ONLY_FLOAT) > 0) {
                        Float.parseFloat(e.text);
                    } else {
                        Integer.parseInt(e.text);
                    }
                } catch (NumberFormatException e1) {
                    e.doit = false;
                }
            }
            if (oldS.length() == 0 && (e.text.charAt(0) > 0 && (e.text.charAt(0) == 'E' || e.text.charAt(0) == 'e'))) {
                e.doit = false;
            }
            if (e.text.equalsIgnoreCase(E) && newS.length() == 1) {
                e.doit = false;
            }
        }
    }
}
