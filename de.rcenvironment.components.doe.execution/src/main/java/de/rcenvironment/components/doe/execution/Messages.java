/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
    public static String tooManySamples;

    /** Field for NLS. */
    public static String minTwoOutputs;

    /** Field for NLS. */
    public static String minOneOutput;

    /** Field for NLS. */
    public static String numLevelsInvalid;

    /** Field for NLS. */
    public static String noTable;

    /** Field for NLS. */
    public static String cannotReadTable;

    /** Field for NLS. */
    public static String tableTooShort;

    /** Field for NLS. */
    public static String tableTooLong;

    /** Field for NLS. */
    public static String noRunNumber;

    /** Field for NLS. */
    public static String noStartSample;

    /** Field for NLS. */
    public static String noEndSample;

    /** Field for NLS. */
    public static String undefinedValues;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
