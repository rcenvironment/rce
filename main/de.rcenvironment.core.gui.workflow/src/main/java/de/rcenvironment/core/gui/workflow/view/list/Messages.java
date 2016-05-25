/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.view.list;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 */
public class Messages extends NLS {

    /** Constant. */
    public static String additionalInformationColon;

    /** Constant. */
    public static String pause;
    
    /** Constant. */
    public static String resume;
    
    /** Constant. */
    public static String cancel;

    /** Constant. */
    public static String dispose;

    /** Constant. */
    public static String name;

    /** Constant. */
    public static String status;

    /** Constant. */
    public static String workflows;
    
    /** Constant. */
    public static String fetchingWorkflows;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
