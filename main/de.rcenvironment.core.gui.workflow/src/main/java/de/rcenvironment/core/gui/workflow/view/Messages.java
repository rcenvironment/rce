/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 */
public class Messages extends NLS {
    
    /** Constant. */
    public static String executionViewTitleSuffix;
    
    /** Constant. */
    public static String platform;

    /** Constant. */
    public static String starttime;

    /** Constant. */
    public static String name;

    /** Constant. */
    public static String controllerPlatform;
    
    /** Constant. */
    public static String local;

    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
