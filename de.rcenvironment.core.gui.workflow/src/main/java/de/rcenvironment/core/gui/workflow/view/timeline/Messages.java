/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.workflow.view.timeline;

import org.eclipse.osgi.util.NLS;

/**
 * Contains all {@link String} Messages.
 *
 * @author Hendrik Abbenhaus
 */
public class Messages extends NLS {

    /**
     * Dialog string select Components.
     */
    public static String selectComponents;

    /**
     * Filter Dialog title.
     */
    public static String filterDialogTitle;

    /**
     * Default Filter for Filter Dialog.
     */
    public static String filterDialogFilterDefault;

    /**
     * Tooltip text for Filter Dialog.
     */
    public static String filterDialogToolTipText;

    /**
     * Zoom in text.
     */
    public static String zoomin;

    /**
     * Zoom out text.
     */
    public static String zoomout;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
