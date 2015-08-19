/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.connections;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Hendrik Abbenhaus
 */
public class Messages extends NLS {

    /**
     * Constant.
     */
    public static String and;

    /**
     * Constant.
     */
    public static String connectionEditor;

    /**
     * Constant.
     */
    public static String connections;

    /**
     * Constant.
     */
    public static String delete;

    /**
     * Constant.
     */
    public static String error;

    /**
     * Constant.
     */
    public static String incompatibleTypes;

    /**
     * Constant.
     */
    public static String source;

    /**
     * Constant.
     */
    public static String target;
    
    /**
     * Constant.
     */
    public static String alreadyConnected;
    
    /**
     * Constant.
     */
    public static String autoConnectInfoText;

    /**
     * Constant.
     */
    public static String filter;

    /**
     * Constant.
     */
    public static String filterTooltip;
    
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
