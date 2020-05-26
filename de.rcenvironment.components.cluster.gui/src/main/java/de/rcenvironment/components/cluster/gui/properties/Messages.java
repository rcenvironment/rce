/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.cluster.gui.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Doreen Seider
 */
public class Messages extends NLS {

    /** Constant. */
    public static String configureQueuingSystem;
    
    /** Constant. */
    public static String queueingSystemLabel;
    
    /** Constant. */
    public static String scriptname;

    /** Constant. */
    public static String isScriptProvided;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
