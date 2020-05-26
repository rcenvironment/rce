/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.editor;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Tobias Menden
 */
public class Messages extends NLS {

    /** Constant. */
    public static String components;

    /** Constant. */
    public static String select;
    
    /** Constant. */
    public static String connection;

    /** Constant. */
    public static String connectionEditor;

    /** Constant. */
    public static String openConnection;

    /** Constant. */
    public static String openConnectionEditor;

    /** Constant. */
    public static String createWorkflow;

    /** Constant. */
    public static String fileWorkflow;

    /** Constant. */
    public static String newConnection;

    /** Constant. */
    public static String newWorkflow;

    /** Constant. */
    public static String rename;

    /** Constant. */
    public static String tools;

    /** Constant. */
    public static String copy;

    /** Constant. */
    public static String paste;

    /** Constant. */
    public static String fetchingComponents;

    /** Constant. */
    public static String memoryExceededWarningMessage;

    /** Constant. */
    public static String memoryExceededWarningHeading;

    /** Constant. */
    public static String labelDescription;

    /** Constant. */
    public static String label;
    
    /** Constant. */
    public static String openWorkflow;
    
    /** Constant. */
    public static String loadingComponents;

    /** Constant. */
    public static String undo;

    /** Constant. */
    public static String redo;

    /** Constant. */
    public static String undoShortcut;

    /** Constant. */
    public static String redoShortcut;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
