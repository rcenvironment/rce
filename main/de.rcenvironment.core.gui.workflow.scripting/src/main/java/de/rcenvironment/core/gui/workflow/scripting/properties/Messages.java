/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.scripting.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String initInvocationSectionTitle;

    /** Constant. */
    public static String runInvocationSectionTitle;

    /** Constant. */
    public static String languagesLabel;

    /** Constant. */
    public static String variablesLabel;

    /** Constant. */
    public static String variablesInsertButtonLabel;

    /** Constant. */
    public static String doInitCommandLabel;

    /** Constant. */
    public static String variablesInputPattern;

    /** Constant. */
    public static String variablesOutputPattern;

    /** Constant. */
    public static String scriptMissingRelative;

    /** Constant. */
    public static String scriptMissingAbsolute;

    /** Constant. */
    public static String languageMissingRelative;

    /** Constant. */
    public static String languageMissingAbsolute;
    
    /** Constant. */
    public static String workingDir;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    /**
     * Binds the message to the given parameters.
     * 
     * @param message the message
     * @param bindings the bindings
     * @return the bound message
     */
    public static String bind2(final String message, final Object... bindings) {
        return NLS.bind(message, bindings);
    }

}
