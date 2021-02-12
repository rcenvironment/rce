/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.login;

import de.rcenvironment.core.authentication.User.Type;
import junit.framework.TestCase;

/**
 * 
 * Test case for the implementation of the <code>AuthenticationService</code>.
 * 
 * @author Doreen Seider
 */
public class LoginInputTest extends TestCase {

    /**
     * Constant.
     */
    private static final String PASSWORD = "password";

    /**
     * Exception message.
     */
    private static final String EXCEPTION_THROWN = "Exception must not be thrown.";

    /**
     * 4 Exception message.
     */
    private static final String EXCEPTION_HAS_TO_BE_THROWN = "Exception has to be thrown.";

    private static LoginInput loginInputLdap;

    /**
     * Test.
     */
    @Override
    public void setUp() {

        loginInputLdap = new LoginInput("f_rcelda", "test987!");
    }

    /**
     * Test.
     */
    public void testGetUsernameLDAP() {
        assertEquals(loginInputLdap.getUsernameLDAP(), "f_rcelda");
    }

    /**
     * Test.
     */
    public void testGetPassword() {
        assertEquals(loginInputLdap.getPassword(), "test987!");
    }

    /**
     * Test.
     */
    public void testGetType() {
        assertEquals(loginInputLdap.getType(), Type.ldap);

    }

}
