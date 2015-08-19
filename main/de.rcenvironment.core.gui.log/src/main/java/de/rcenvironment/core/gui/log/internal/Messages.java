/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.log.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 */
public class Messages extends NLS {

    /** Constant. */
    public static String bundle;

    /** Constant. */
    public static String platform;

    /** Constant. */
    public static String clear;

    /** Constant. */
    public static String debug;

    /** Constant. */
    public static String error;
    
    /** Constant. */
    public static String info;

    /** Constant. */
    public static String level;

    /** Constant. */
    public static String message;

    /** Constant. */
    public static String search;

    /** Constant. */
    public static String timestamp;

    /** Constant. */
    public static String warn;

    /** Constant. */
    public static String copy;

    /** Constant. */
    public static String scrollLock;
    
    /** Constant. */
    public static String fetchingPlatforms;
    
    /** Constant. */
    public static String fetchingLogs;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
