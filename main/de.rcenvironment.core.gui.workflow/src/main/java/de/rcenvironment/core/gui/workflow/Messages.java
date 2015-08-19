/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow;

import org.eclipse.osgi.util.NLS;


/**
 * Supports language specific messages.
 *
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String activeWorkflowsTitle;

    /** Constant. */
    public static String activeWorkflowsMessage;
    
    /** Constant. */
    public static String incompatibleVersionTitle;
    
    /** Constant. */
    public static String incompatibleVersionMessage;
        
    /** Constant. */
    public static String updateIncompatibleVersionSilently;
    
    /** Constant. */
    public static String workflowUpdateFailureTitle;
    
    /** Constant. */
    public static String workflowUpdateFailureMessage;

    /** Constant. */
    public static String silentWorkflowUpdateFailureMessage;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
