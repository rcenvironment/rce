/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.login;

/**
 * Constants for test setups.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (merged key constants; changed to resource loading; javadoc)
 */
public final class LoginTestConstants {

    /**
     * Bundle name.
     */
    public static final String BUNDLE_SYMBOLIC_NAME = "de.rcenvironment.rce.login";

    // TODO also delete these files and references

    private static final String USER_1_CERT_PATH = "/usercert_rainertester.pem";

    private static final String USER_1_KEY_PATH = "/userkey_rainertester.pem";

    private static final String USER_2_CERT_PATH = "/usercert_rainerhacker.pem";

    private static final String USER_2_KEY_PATH = "/userkey_rainerhacker.pem";

    private LoginTestConstants() {}
}
