/*
 * Copyright (C) 2006-2016 DLR, Germany
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
 * @author Brigitte Boden
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
    public static String note;

    /** Text for OutputLocation Table. */
    public static String inputsForOutputLocation;

    /** Title for OutputLocation Edit Dialog. */
    public static String outputLocationEditDialogTitle;

    /** Title for OutputLocation Add Dialog. */
    public static String outputLocationAddDialogTitle;

    /** Title for OutputLocation Pane. */
    public static String outputLocationPaneTitle;

    /** Root folder section title. */
    public static String rootFolderSectionTitle;

    /** Text for root folder checkbox. */
    public static String selectAtStart;

    /** Text for root folder dialog. */
    public static String selectRootFolder;

    /**
     * Labels for settings in OutputWriterEndpointEditDialog and OutputLocation
     * Edit Dialog.
     */

    /** Name field. */
    // public static String name;

    /** Filename field. */
    public static String outputLocFilename;

    /** Filename field. */
    public static String inputTargetName;

    /** Target folder field. */
    public static String targetFolder;

    /** Subfolder field. */
    public static String subFolder;

    /** Text for "insert" button. */
    public static String insertButtonText;

    /** Text for "header" field. */
    public static String header;

    /** Text for "format String" field. */
    public static String format;

    /** Text for "Handle existing file" field. */
    public static String handleExisting;

    /** Text for add button in outputLocation Dialog. */
    public static String add;

    /** Text for add button in outputLocation Dialog. */
    public static String edit;

    /** Text for add button in outputLocation Dialog. */
    public static String remove;

    /** Message: Optional. */
    public static String optionalMessage;

    /** Message: Only one subfolder. */
    public static String onlyOneSubfolderMessage;

    /** Message: Handling option only for file from previous iteration. */
    public static String previousIterationMessage;

    /** Confirm dialog on input changes. */
    public static String editingInputWithOutputLocationDialogTitle;

    /** Confirm dialog on input changes. */
    public static String editingInputWithOutputLocationDialogText;

    /** Confirm dialog on input changes. */
    public static String deletingInputWithOutputLocationDialogText;

    /** Example for header field. */
    public static String headerMessage;

    /** Example for format field. */
    public static String formatMessage;

    /** Warning on empty inputs table. */
    public static String emptyInputTable;

    /** Title for options group. */
    public static String groupTitleTargetFile;

    /** Title for options group. */
    public static String groupTitleFormat;

    /** Title for options group. */
    public static String groupTitleInputs;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    /*
     * public static String Type(Type type){ switch (type) { case FILE: return
     * addfile; case FOLDER: return addfolder; default: return null; } }
     * 
     * public static Type getTypeByText(String text){ if
     * (text.contains(addfile)){ return Type.FILE; }else
     * if(text.contains(addfolder)){ return Type.FOLDER; }else{ return null; }
     * 
     * }
     */

}
