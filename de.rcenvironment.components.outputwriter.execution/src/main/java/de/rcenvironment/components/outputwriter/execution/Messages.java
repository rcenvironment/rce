/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Validation Message. */
    public static String noRootChosen;

    /** Warning if not input is connected to an output. */
    public static String noInputForOutput;

    /** Warning if input does not exist. */
    public static String missingInput;

    /** Warning if placeholder cannot be matched. */
    public static String unmatchedFormatPlaceholder;

    /** Warning if header placeholder cannot be matched. */
    public static String unmatchedHeaderPlaceholder;
    
    /** Warning if a syntax error has been found. */
    public static String syntaxError;

    /** Warning if input is connected to no output. */
    public static String noOutputForInput;

    /**
     * Warning if output location has connected as well as unconnected inputs.
     */
    public static String connectedAndUnconnectedInputs;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
