/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.components.script.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {
    
    /** Constant. */
    public static String chooseLanguage;

    /** Constant. */
    public static String noScript;

    /** Constant. */
    public static String defaultScript;

    /** Constant. */
    public static String noScriptMessage;

    /** Constant. */
    public static String defaultScriptMessage;

    /** Constant. */
    public static String wrongUsage;

    /** Constant. */
    public static String wrongUsageMessage;
    
    /** Constant. */
    public static String scriptname;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
