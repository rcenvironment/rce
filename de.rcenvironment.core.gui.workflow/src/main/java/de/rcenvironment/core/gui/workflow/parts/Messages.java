/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.parts;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Doreen Seider
 */
public class Messages extends NLS {
    
    /** Constant. */
    public static String localExecutionOnly;
    
    /** Constant. */
    public static String imitationMode;
    
    /** Constant. */
    public static String deprecated;
    
    /** Constant. */
    public static String initializingComponentState;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
