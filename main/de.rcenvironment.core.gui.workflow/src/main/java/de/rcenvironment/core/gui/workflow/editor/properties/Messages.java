/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Tobias Menden
 * @author Sascha Zur
 */
public class Messages extends NLS {

    /** Constant. */
    public static String itemSelected;

    /** Constant. */
    public static String noItemSelected;

    /** Constant. */
    public static String property;

    /** Constant. */
    public static String defaultConfigMap;

    /** Constant. */
    public static String selectTitle;

    /** Constant. */
    public static String manageTitle;

    /** Constant. */
    public static String newProfile;

    /** Constant. */
    public static String inheritedFrom;   

    /** Constant. */
    public static String name;

    /** Constant. */
    public static String dataType;

    /** Constant. */
    public static String inputs;

    /** Constant. */
    public static String outputs;

    /** Constant. */
    public static String configure;

    /** Constant. */
    public static String configurationHeader;

    /** Constant. */
    public static String noConfig;

    /** Constant. */
    public static String invalidDataTypeDialogTitle;

    /** Constant. */
    public static String invalidDataTypeDialogMessage;

    /** Constant. */
    public static String input;

    /** Constant. */
    public static String output;

    /** Constant. */
    public static String nestedLoopTitle;

    /** Constant. */
    public static String isNestedLoop;

    /** Constant. */
    public static String dataItemTitle;

    /** Constant. */
    public static String storeDataItem;

    /** Constant. */
    public static String dataItemNote;
    
    /** Constant. */
    public static String connections;

    /** Constant. */
    public static String title;

    /** Constant. */
    public static String textAlignment;
    
    /** Constant. */
    public static String labelPosition;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
