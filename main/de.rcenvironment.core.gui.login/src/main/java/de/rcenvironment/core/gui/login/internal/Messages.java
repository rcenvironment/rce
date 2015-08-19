/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.gui.login.internal;

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
    public static String informationCommandLabel;

    /**
     * Constant.
     */
    public static String informationCommandTooltip;

    /**
     * Constant.
     */
    public static String login;
    
    /**
     * Constant.
     */
    public static String loginDialog;

    /**
     * Constant.
     */
    public static String reLoginDialog;
    
    /**
     * Constant.
     */
    public static String certAandKeyRequiered;
    
    /**
     * Constant.
     */
    public static String keyForCertRequiered;
    
    /**
     * Constant.
     */
    public static String confirmCert;
    
    /**
     * Constant.
     */
    public static String confirmLdapUsername;
    
    /**
     * Constant.
     */
    public static String cert;
    
    /**
     * Constant.
     */
    public static String validTill;
    
    /**
     * Constant.
     */
    public static String chooseValidCert;
    
    /**
     * Constant.
     */
    public static String chooseNewCert;
    
    /**
     * Constant.
     */
    public static String readCert;
    
    /**
     * Constant.
     */
    public static String readSystemCertMemory;
    
    /**
     * Constant.
     */
    public static String privateKey;
    
    /**
     * Constant.
     */
    public static String searchMatchingKey;
    
    /**
     * Constant.
     */
    public static String chooseNewKey;
    
    /**
     * Constant.
     */
    public static String password;
    
    /**
     * Constant.
     */
    public static String validPassword;

    /**
     * Constant.
     */
    public static String certEndsIn;
    
    /**
     * Constant.
     */
    public static String sessionEndsIn;
    
    /**
     * Constant.
     */
    public static String chooseKey;
    
    /**
     * Constant.
     */
    public static String keyRevoked;
    
    /**
     * Constant.
     */
    public static String chooseCert;
    
    /**
     * Constant.
     */
    public static String certRevoked;
    
    /**
     * Constant.
     */
    public static String oneDay;
    
    /**
     * Constant.
     */
    public static String day;
    
    /**
     * Constant.
     */
    public static String certificateTabName;
    
    /**
     * Constant.
     */
    public static String ldapTabName;
    
    /**
     * Constant.
     */
    public static String username;
    
    /**
     * Constant.
     */
    public static String certLoadFailed;

    /**
     * Constant.
     */
    public static String ldapUsernameAndPasswordRequired;
    /**
     * Constant.
     */
    public static String anonymousLogin;

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
