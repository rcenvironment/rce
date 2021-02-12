/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.switchcmp.gui;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * Messages for switch component.
 *
 * @author David Scholz
 */
public class Messages extends NLS {

    /**
     * Messages.
     */
    public static String dataInputString;

    /**
     * Messages.
     */
    public static String conditionInputString;

    /**
     * Messages.
     */
    public static String dataOutputString;

    /**
     * Messages.
     */
    public static String conditionFieldString;

    /**
     * Messages.
     */
    public static String operatorsLabelString;

    /**
     * Messages.
     */
    public static String channelLabelString;

    /**
     * Messages.
     */
    public static String conditionLabelString;

    /**
     * Messages.
     */
    public static String insertButtonString;

    /**
     * Messages.
     */
    public static String validateButtonString;

    /**
     * Messages.
     */
    public static String writeOutputLabel;

    static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

}
