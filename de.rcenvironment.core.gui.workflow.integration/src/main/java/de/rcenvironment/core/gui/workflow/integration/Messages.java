/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Constant. */
    public static String toolNameLabel;

    /** Constant. */
    public static String toolIntegratorNameLabel;

    /** Constant. */
    public static String toolIntegratorEmailLabel;

    /** Constant. */
    public static String toolDescriptionLabel;

    /** Constant. */
    public static String infoSection;

    /** Constant. */
    public static String preScriptSection;

    /** Constant. */
    public static String postScriptSection;

    /** Constant. */
    public static String commandScriptSection;

    /** Constant. */
    public static String tempDirectorySection;

    /** Constant. */
    public static String propertyConfiguration;

    /** Constant. */
    public static String propGroupsLabel;

    /** Constant. */
    public static String name;

    /** Constant. */
    public static String value;

    /** Constant. */
    public static String properties;
    
    /** Constant. */
    public static String mockModeNotAvailable;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
