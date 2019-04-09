/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

/**
 * 
 * Factory for <code>LoginInputs</code>.
 * 
 * @author Doreen Seider
 */
public final class LoginInputFactory {

    /**
     * Constant.
     */
    private static final String PASSWORD = "password";

    /**
     * Constant.
     */
    private static final String USER_ID = "f_rcelda";

    /**
     * Constant.
     */
    private static final String LDAP_PASSWORD = "test987!";

    /**
     * Exception message.
     */
    private static final String EXCEPTION_THROWN = "Exception must not be thrown.";

    /**
     * Test {@link LoginInput}.
     */
    private static LoginInput loginInput = null;

    /**
     * Another test {@link LoginInput}.
     */
    private static LoginInput anotherLoginInput = null;

    /**
     * Private constructor.
     */
    private LoginInputFactory() {

    }

    /**
     * 
     * Getter.
     * 
     * @return the login input object.
     */
    public static LoginInput getLoginInputForLDAP() {
        // with the if-clause, the test don't get to the part
        // if (loginInput == null) {
        final String userID = USER_ID;
        final String password = LDAP_PASSWORD;

        loginInput = new LoginInput(userID, password);
        // }

        return loginInput;
    }

}
