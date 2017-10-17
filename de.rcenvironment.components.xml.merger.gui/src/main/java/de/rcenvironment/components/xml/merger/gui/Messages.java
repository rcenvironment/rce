/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.xml.merger.gui;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Markus Kunde
 */
public class Messages extends NLS {

    /** Constant. */
    public static String fileChoosingSectionName;

    /** Constant. */
    public static String fileLinkButtonLabel;

    /** Constant. */
    public static String fileEditorButtonLabel;

    /** Constant. */
    public static String loadTitle;

    /** Constant. */
    public static String loadMessage;

    /** Constant. */
    public static String actuallyLoadedLabel = null;

    /** Constant. */
    public static String cannotReadContentFromFile;

    /** Constant. */
    public static String refreshProjectExplorer;

    /** Constant. */
    public static String logReadFromFileError;

    /** Constant. */
    public static String dialogMessageReadFromFileError;

    /** Constant. */
    public static String mappingFileAsInputButton;

    /** Constant. */
    public static String mappingFileLoadedButton;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
