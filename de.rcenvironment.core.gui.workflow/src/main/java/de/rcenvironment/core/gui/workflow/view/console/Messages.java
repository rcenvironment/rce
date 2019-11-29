/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.console;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Tobias Menden
 */
public class Messages extends NLS {

    /** Constant. */
    public static String clear;

    /** Constant. */
    public static String scrollLock;

    /** Constant. */
    public static String stdout;

    /** Constant. */
    public static String stderr;

    /** Constant. */
    public static String compLog;

    /** Constant. */
    public static String search;

    /** Constant. */
    public static String copyMessage;
    
    /** Constant. */
    public static String copyLine;

    /** Constant. */
    public static String type;

    /** Constant. */
    public static String timestamp;

    /** Constant. */
    public static String message;

    /** Constant. */
    public static String component;

    /** Constant. */
    public static String workflow;

    /** Constant. */
    public static String fetchingConsoleOutputs;
    
    /** Constant. */
    public static String openConsoleOutputs;
    
    /** Constant. */
    public static String resetSearch;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
