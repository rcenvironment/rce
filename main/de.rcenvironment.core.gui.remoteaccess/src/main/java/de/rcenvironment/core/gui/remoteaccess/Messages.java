/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.gui.remoteaccess;

import org.eclipse.osgi.util.NLS;


/**
 * Supports language specific messages.
 * 
 * @author Brigitte Boden
 */
public class Messages extends NLS {
    
    /** Constant.  */
    public static String outputs;
    
    /** Constant.  */
    public static String inputs;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
