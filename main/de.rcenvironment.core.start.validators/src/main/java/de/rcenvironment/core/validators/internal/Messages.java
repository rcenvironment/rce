/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.validators.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 * 
 * @author Christian Weiss
 */
public class Messages extends NLS {

    /** Constant. */
    public static String directoryNoConfigurationService;

    /** Constant. */
    public static String directoryCouldNotBeRetrieved;

    /** Constant. */
    public static String directoryRceFolderDoesNotExist;

    /** Constant. */
    public static String directoryRceFolderNotReadWriteAble;

    /** Constant. */
    public static String directoryRceFolderPathTooLong;

    /** Constant. */
    public static String permGenSizeTooLow;

    /** Constant. */
    public static String noValidationMsg;

    /** Constant. */
    public static String couldNotValidateJsonFile;

    /** Constant. */
    public static String instanceIdAlreadyInUse;

    /** Constant. */
    public static String failedToCreateTempFile;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

}
