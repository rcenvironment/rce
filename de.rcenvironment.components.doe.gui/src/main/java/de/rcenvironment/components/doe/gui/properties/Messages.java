/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
    public static String startSampleInvalid;

    /** Field for NLS. */
    public static String endSampleNotValid;

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
    public static String minOneOutput;

    /** Field for NLS. */
    public static String minTwoOutputs;

    /** Field for NLS. */
    public static String tableRowLabel;

    /** Field for NLS. */
    public static String outputsNote;

    /** Field for NLS. */
    public static String tooMuchElements;

    /** Field for NLS. */
    public static String tooManyElementsForExecution;

    /** Field for NLS. */
    public static String noTableLong;

    static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
