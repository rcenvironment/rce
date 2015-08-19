/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.cluster.configuration.internal;

import org.eclipse.osgi.util.NLS;

/**
 * I18N.
 *
 * @author Doreen Seider
 */
public class Messages extends NLS {

    /** Constant. */
    public static String yes;

    /** Constant. */
    public static String no;
    
    /** Constant. */
    public static String cancel;
    
    /** Constant. */
    public static String deleteConfirmDialogTitle;

    /** Constant. */
    public static String deleteConfirmDialogQuestion;
    
    /** Constant. */
    public static String passwordDialogTitle;
    
    /** Constant. */
    public static String passwordDialogMessage;
    
    /** Constant. */
    public static String newButtonTitle;

    /** Constant. */
    public static String editButtonTitle;
    
    /** Constant. */
    public static String editButtonTitle2;
    
    /** Constant. */
    public static String deleteButtonTitle;
    
    /** Constant. */
    public static String connectButtonTitle;

    /** Constant. */
    public static String cancelButtonTitle;
    
    /** Constant. */
    public static String selectHostDialogTitle;

    /** Constant. */
    public static String selectHostDialogMessage;

    /** Constant. */
    public static String newConfigurationDialogTitle;

    /** Constant. */
    public static String newConfigurationDialogMessage;
    
    /** Constant. */
    public static String queueingSystemLabel;

    /** Constant. */
    public static String hostLabel;

    /** Constant. */
    public static String portLabel;

    /** Constant. */
    public static String usernameLabel;

    /** Constant. */
    public static String passwordLabel;
    
    /** Constant. */
    public static String configurationNameLabel;
    
    /** Constant. */
    public static String savePasswordCheckboxLabel;
    
    /** Constant. */
    public static String useDefaultNameCheckboxLabel;

    /** Constant. */
    public static String createButtonTitle;
    
    /** Constant. */
    public static String maintainHostLabel;

    /** Constant. */
    public static String maintainPortLabel;

    /** Constant. */
    public static String maintainPortNumberLabel;

    /** Constant. */
    public static String maintainUsernameLabel;

    /** Constant. */
    public static String maintainPasswordLabel;
    
    /** Constant. */
    public static String maintainConfigurationNameLabel;
    
    /** Constant. */
    public static String maintainAnotherConfigurationNameLabel;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
