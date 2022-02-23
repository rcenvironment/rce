/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

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
    public static String invalidIPconfig;

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

    /** Constant. Takes two parameters: Profile ID (String) and path to profile directory (String) */
    public static String profileUpgradeQuery;

    /** Constant. Takes three parameters: Profile ID (String), path to profile directory (String), and current profile version (int) */
    public static String profileUpgradeLogMessage;

    /** Constant. Takes three parameters: Profile ID (String), path to profile directory (String), and current profile version (int) */
    public static String profileUpgradeNoQueryUserHint;

    /** Constant. Takes two parameters: Profile ID (String) and path to profile directory (String) */
    public static String profileUpgradeTriedAndFailedError;

    /** Constant. Takes two parameters: Profile ID (String) and current profile version (int) */
    public static String profileUpgradeNotPossibleError;

    /** Constant. */
    public static String profileVersionValidationSuccess;
    
    /** Constant. Takes one parameter: path to profile directory (String) */
    public static String profileNotAccessibleError;
    
    /** Constant. Takes one parameter: Profile ID (String) */
    public static String profileVersionNotDeterminedError;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

}
