/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.doe.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Field for NLS. */
    public static String noOutputsDefined;

    /** Field for NLS. */
    public static String noOutputsDefinedLong;

    /** Field for NLS. */
    public static String numOutputsG2;

    /** Field for NLS. */
    public static String numOutputsG2Long;

    /** Field for NLS. */
    public static String numLevelsInvalid;

    /** Field for NLS. */
    public static String numLevelsInvalidLong;

    /** Field for NLS. */
    public static String startSampleInvalid;

    /** Field for NLS. */
    public static String startSampleNotInteger;

    /** Field for NLS. */
    public static String startSampleG0;

    /** Field for NLS. */
    public static String endSampleNotValid;

    /** Field for NLS. */
    public static String endSampleNotInteger;

    /** Field for NLS. */
    public static String endSampleG0;

    /** Field for NLS. */
    public static String endSampleGStart;

    /** Field for NLS. */
    public static String sectionHeader;

    /** Field for NLS. */
    public static String seedLabel;

    /** Field for NLS. */
    public static String failedRunBehaviorLabel;

    /** Field for NLS. */
    public static String sampleStart;

    /** Field for NLS. */
    public static String sampleEnd;

    /** Field for NLS. */
    public static String saveTableButton;

    /** Field for NLS. */
    public static String loadTableButton;

    /** Field for NLS. */
    public static String codedValuesButton;

    /** Field for NLS. */
    public static String numLevelsLabel;

    /** Field for NLS. */
    public static String desiredRunsLabel;

    /** Field for NLS. */
    public static String sampleNum;

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
    public static String minOneOutput;

    /** Field for NLS. */
    public static String minTwoOutputs;

    /** Field for NLS. */
    public static String tableRowLabel;

    /** Field for NLS. */
    public static String endSampleTooHigh;

    /** Field for NLS. */
    public static String endSampleTooHighLong;

    /** Field for NLS. */
    public static String startSampleTooHigh;

    /** Field for NLS. */
    public static String outputsNote;

    /** Field for NLS. */
    public static String tooMuchElements;

    /** Field for NLS. */
    public static String tooManyElementsForExecution;

    /** Field for NLS. */
    public static String tooManySamples;

    static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
