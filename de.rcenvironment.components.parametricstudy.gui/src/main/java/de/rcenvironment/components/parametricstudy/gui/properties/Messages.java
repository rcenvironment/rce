/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.gui.properties;

import org.eclipse.osgi.util.NLS;


/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Constant. */
    public static String fromMsg;

    /** Constant. */
    public static String toMsg;

    /** Constant. */
    public static String stepSizeMsg;

    /** Constant. */
    public static String inStepMsg;

    /** Constant. */
    public static String stepsMsg;

    /** Constant. */
    public static String noInput;

    /** Constant. */
    public static String noValue;
    
    /** Constant. */
    public static String rangeMsg;
    
    /** Constant. */
    public static String inputs;
    
    /** Constant. */
    public static String outputs;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
