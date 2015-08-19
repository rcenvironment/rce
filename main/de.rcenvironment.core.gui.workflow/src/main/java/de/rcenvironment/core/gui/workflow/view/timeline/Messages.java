/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.timeline;

import org.eclipse.osgi.util.NLS;

/**
 * Messages class.
 *
 * @author Hendrik Abbenhaus
 */
public class Messages extends NLS {

    /***/
    public static String selectComponents;

    /***/
    public static String filterDialogTitle;

    /***/
    public static String filterDialogFilterDefault;

    /***/
    public static String filterDialogToolTipText;

    /***/
    public static String zoomin;

    /***/
    public static String zoomout;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
