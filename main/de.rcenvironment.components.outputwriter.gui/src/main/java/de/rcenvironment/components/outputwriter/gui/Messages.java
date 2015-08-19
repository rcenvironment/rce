/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Contains the messages of Outputwriter GUI.
 * 
 * @author Hendrik Abbenhaus
 * 
 */
public class Messages extends NLS {

    /** title of PropertyTab. */
    public static String propertyTabTitle;

    /** title of inputsTab. */
    public static String inputs;

    /** Name of Group in PropertyTable. */
    public static String groupname;

    /** Name for handle: do nothing. */
    public static String handleDoNothing;

    /** Name for handle: override. */
    public static String handleOverride;

    /** Name for handle: append. */
    public static String handleAppend;

    /** Name for handle: autorename. */
    public static String handleAutoRename;

    /** Text of ok Button in AddDialog. */
    public static String okButton;

    /** Text of Label in AddDialog. */
    public static String typeinaName;

    // public static String setRootButton;

    /** Text for column. */
    public static String structuretreeInputs;

    /** inputs in json String. */
    public static String jsonInputs;

    /** propertytable group general. */
    public static String propertytableGroupGeneral;

    /** Constant. */
    public static String noRootChosen;

    /** Constant. */
    public static String note;

    private static final String BUNDLE_NAME = Messages.class.getPackage()
        .getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    /*
     * public static String Type(Type type){ switch (type) { case FILE: return addfile; case FOLDER:
     * return addfolder; default: return null; } }
     * 
     * public static Type getTypeByText(String text){ if (text.contains(addfile)){ return Type.FILE;
     * }else if(text.contains(addfolder)){ return Type.FOLDER; }else{ return null; }
     * 
     * }
     */

}
