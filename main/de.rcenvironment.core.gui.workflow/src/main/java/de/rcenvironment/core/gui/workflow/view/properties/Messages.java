/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 */
public class Messages extends NLS {
    
    /** Constant. */
    public static String inputs;
    
    /** Constant. */
    public static String latestInput;
    
    /** Constant. */
    public static String processedInputs;
    
    /** Constant. */
    public static String inputQueue;
    
    /** Constant. */
    public static String scrollLock;
    
    /** Constant. */
    public static String componentInstanceUnknown;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
