/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.sql.common;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Markus Kunde
 */
public class Messages extends NLS {

    /** Constant. */
    public static String inputModeBlockLabel;

    /** Constant. */
    public static String columnTypeIgnoreLabel;

    /** Constant. */
    public static String columnTypeIntegerLabel;

    /** Constant. */
    public static String columnTypeDoubleLabel;

    /** Constant. */
    public static String columnTypeStringLabel;

    /** Constant. */
    public static String columnTypeClobLabel;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
