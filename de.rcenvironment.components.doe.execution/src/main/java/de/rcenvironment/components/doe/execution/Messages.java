/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Field for NLS. */
    public static String noOutputsDefined;

    /** Field for NLS. */
    public static String noOutputsDefinedLong;

    /** Field for NLS. */
    public static String tooManySamples;

    /** Field for NLS. */
    public static String numOutputsG2;

    /** Field for NLS. */
    public static String numOutputsG2Long;

    /** Field for NLS. */
    public static String numLevelsInvalid;

    /** Field for NLS. */
    public static String numLevelsInvalidLong;

    /** Field for NLS. */
    public static String noTable;

    /** Field for NLS. */
    public static String noTableLong;

    /** Field for NLS. */
    public static String tableTooShort;

    /** Field for NLS. */
    public static String tableTooShortLong;

    /** Field for NLS. */
    public static String tableTooLong;

    /** Field for NLS. */
    public static String tableTooLongLong;

    /** Field for NLS. */
    public static String endSampleTooHigh;

    /** Field for NLS. */
    public static String endSampleTooHighLong;

    /** Field for NLS. */
    public static String startSampleNotInteger;

    /** Field for NLS. */
    public static String startSampleG0;

    /** Field for NLS. */
    public static String endSampleNotInteger;

    /** Field for NLS. */
    public static String endSampleG0;

    /** Field for NLS. */
    public static String endSampleGStart;

    /** Field for NLS. */
    public static String startSampleTooHigh;

    /** Field for NLS. */
    public static String undefinedValues;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
