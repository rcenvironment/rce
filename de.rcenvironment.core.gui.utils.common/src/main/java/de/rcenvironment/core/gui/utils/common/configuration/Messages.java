/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common.configuration;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String propertyKey;

    /** Constant. */
    public static String propertyValue;

    /** Constant. */
    public static String trueLabel;

    /** Constant. */
    public static String falseLabel;

    /** Constant. */
    public static String exportToExcelDialogTitle;
    
    /** Constant. */
    public static String exportToExcelDialogText;
    
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
