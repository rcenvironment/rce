/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication;

import java.io.File;

/**
 * Test constants for the information tests.
 * 
 * @author Heinrich Wendel
 */
public final class AuthenticationTestConstants {
    
    /** Default CA certificate file. */
    public static final String CA_FILE = "/cacert.pem";

    /** Default crl file. */
    public static final String CRL_FILE = "/cacrl.pem";
    
    /** CA files. */
    public static final String OTHER_CA_FILE = "ca.pem";

    /** Crl files. */
    public static final String OTHER_CRL_FILE = "crl.pem";
    
    /** Constant. */
    public static final String USER_DIR = "user.dir";
    
    /** Constant. */
    public static final String TESTRESOURCES_DIR = File.separator + "src" + File.separator
        + "test" + File.separator + "resources";

    /** Constant. */
    public static final String PASSWORD_UNKNOWN_USER = "sesis4all";

    /** Constant. */
    public static final String KEY_UNKNOWN_USER_PEM = "/unknownkey.pem";

    /** Constant. */
    public static final String CERT_UNKNOWN_USER_PEM = "/unknowncert.pem";

    /** Constant. */
    public static final String PASSWORD_RCE_ENGINEER = "rcekannwat";

    /** Constant. */
    public static final String USERKEY_RCE_ENGINEER_PEM = "/engineerkey.pem";

    /** Constant. */
    public static final String USERCERT_RCE_ENGINEER_PEM = "/engineercert.pem";

    /** Constant. */
    public static final String PASSWORD_RCE_ENEMY = "tataaa";

    /** Constant. */
    public static final String KEY_RCE_ENEMY_PEM = "/enemykey.pem";

    /** Constant. */
    public static final String CERT_RCE_ENEMY_PEM = "/enemycert.pem";
    
    /** Constant. s*/
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.authentication";

    
    private AuthenticationTestConstants() {}

}
