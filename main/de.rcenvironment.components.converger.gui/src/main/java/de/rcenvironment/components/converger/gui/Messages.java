/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.converger.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Constant. */
    public static String absoluteConvergenceMessage;

    /** Constant. */
    public static String relativeConvergenceMessage;

    /** Constant. */
    public static String add;

    /** Constant. */
    public static String edit;

    /** Constant. */
    public static String remove;

    /** Constant. */
    public static String name;

    /** Constant. */
    public static String dataType;

    /** Constant. */
    public static String startValue;

    /** Constant. */
    public static String addInput;

    /** Constant. */
    public static String editInput;

    /** Constant. */
    public static String hasStartValue;

    /** Constant. */
    public static String none;

    /** Constant. */
    public static String parameterTitle;

    /** Constant. */
    public static String inputTitle;

    /** Constant. */
    public static String noInput;

    /** Constant. */
    public static String propertyIncorrectFloat;

    /** Constant. */
    public static String propertyIncorrectInt;

    /** Constant. */
    public static String smallerZero;

    /** Constant. */
    public static String smallerEqualsZero;

    /** Constant. */
    public static String inputs;

    /** Constant. */
    public static String outputs;

    /** Constant. */
    public static String maxIterations;
    
    /** Constant. */
    public static String iterationsToConsider;

    /** Constant. */
    public static String noMaxIterations;

    static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
