/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Jascha Riedel
 */
public class Messages extends NLS {

    /** Constant. */
    public static String pythonExecutionTestError;

    /** Constant. */
    public static String pythonExecutionTestErrorRelative;

    /** Constant. */
    public static String pythonExecutionUnsupportedVersionRelative;

    /** Constant. */
    public static String noScript;

    /** Constant. */
    public static String defaultScript;

    /** Constant. */
    public static String noScriptMessage;

    /** Constant. */
    public static String defaultScriptMessage;
    
    /** Constant. */
    public static String scriptInconsistentIndentation;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
