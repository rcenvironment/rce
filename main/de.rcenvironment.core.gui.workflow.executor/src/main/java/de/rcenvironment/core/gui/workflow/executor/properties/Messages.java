/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.executor.properties;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Doreen Seider
 */
public class Messages extends NLS {

    /** Constant. */
    public static String configureHost;
    
    /** Constant. */
    public static String configureUploadFiles;
    
    /** Constant. */
    public static String configureUploadInputs;
    
    /** Constant. */
    public static String configureDownloadFiles;
    
    /** Constant. */
    public static String configureDownloadOutputs;
    
    /** Constant. */
    public static String fileInputNote;
    
    /** Constant. */
    public static String hostLabel;

    /** Constant. */
    public static String portLabel;
    
    /** Constant. */
    public static String sandboxRootLabel;
    
    /** Constant. */
    public static String add;
    
    /** Constant. */
    public static String edit;
    
    /** Constant. */
    public static String remove;
    
    /** Constant. */
    public static String configureScript;
    
    /** Constant. */
    public static String threeDots;
    
    /** Constant. */
    public static String remotePath;
    
    /** Constant. */
    public static String directory;
    
    /** Constant. */
    public static String useLocalScript;
    
    /** Constant. */
    public static String localScript;
    
    /** Constant. */
    public static String writeScriptHere;
    
    /** Constant. */
    public static String openInEditor;
    
    /** Constant. */
    public static String scriptFileName;
    
    /** Constant. */
    public static String remoteTargetPath;
    
    /** Constant. */
    public static String addNewFile;
    
    /** Constant. */
    public static String localTargetPath;
    
    /** Constant. */
    public static String output;
    
    /** Constant. */
    public static String input;
    
    /** Constant. */
    public static String localTarget;
    
    /** Constant. */
    public static String uploadInputFiles;
    
    /** Constant. */
    public static String downloadOutputFiles;
    
    /** Constant. */
    public static String downloadFiles;
    
    /** Constant. */
    public static String uploadFiles;
    
    /** Constant. */
    public static String remoteTarget;
    
    /** Constant. */
    public static String localPath;
    
    /** Constant. */
    public static String errorMissing;
    
    /** Constant. */
    public static String warningUploadFileListEmpty;
    
    /** Constant. */
    public static String warningUploadInputListEmpty;
    
    /** Constant. */
    public static String warningDownloadFileListEmpty;
    
    /** Constant. */
    public static String warningDownloadInputListEmpty;
    
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
