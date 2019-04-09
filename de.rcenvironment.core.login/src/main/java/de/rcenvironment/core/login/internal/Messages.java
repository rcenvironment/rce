/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.login.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Supports language specific messages.
 *
 * @author Tobias Menden
 */

public class Messages extends NLS {
    
    /**
     * Constant.
     */
    public static String assertionsParameterNull;

    /**
     * Constant.
     */
    public static String authenticationFailed;
    
    /**
     * Constant.
     */
    public static String passwordRequiered;

    /**
     * Constant.
     */
    public static String passwordIncorrect;

    /**
     * Cosntant.
     */
    public static String key;

    /**
     * Constant.
     */
    public static String keyNotMatched;

    /**
     * Constant.
     */
    public static String certificate;

    /**
     * Constant.
     */
    public static String certNotSigned;

    /**
     * Constant.
     */
    public static String certRevoced;

    /**
     * Constant.
     */
    public static String unknownReason;

    /**
     * Constant.
     */
    public static String authenticationSucced;

    /**
     * Constant.
     */
    public static String autoLoginFailed;
    
    /**
     * Constant.
     */
    public static String passwordInvalid;
    
    /**
     * Constant.
     */
    public static String passwordOrUsernameIncorrect;

    /**
     * Constant.
     */
    public static String certOrKeyIncorrect;

    /**
     * Constant.
     */
    public static String validityAtLeastOneDay;

    /**
     * Constant.
     */
    public static String valityInDays;

    /**
     * Constant.
     */
    public static String userReset;

    /**
     * Constant.
     */
    public static String usernameLDAP;
    
    /**
     * Constant.
     */
    public static Object password;

    /**
     * Constant.
     */
    public static String assertionsStringEmpty;

    /**
     * Constant.
     */
    public static String keyOrCertCouldNotBeLoaded;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}

