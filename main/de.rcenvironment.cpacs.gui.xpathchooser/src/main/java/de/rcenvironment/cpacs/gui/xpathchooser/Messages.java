/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.cpacs.gui.xpathchooser;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Markus Kunde
 */
public class Messages extends NLS {

    
    
    /** Constant. */
    public static String outputPaneName;
    
    /** Constant. */
    public static String inputPaneName;
    
    /** Constant. */
    public static String selectButton;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
