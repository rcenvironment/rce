/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import junit.framework.TestCase;

import org.globus.gsi.OpenSSLKey;

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
     * Getter.
     * 
     * @return the login input object.
     */
    public static LoginInput getLoginInputForCertificate() {
        if (loginInput == null) {
            try {
                OpenSSLKey key = new OpenSSLKeyMock(LoginTestConstants.USER_1_KEY_FILENAME);

                final String password = PASSWORD;

                loginInput = new LoginInput(LoginTestConstants.USER_1_CERTIFICATE, key, password);

            } catch (FileNotFoundException e) {
                TestCase.fail(EXCEPTION_THROWN);
            } catch (GeneralSecurityException e) {
                TestCase.fail(EXCEPTION_THROWN);
            } catch (IOException e) {
                TestCase.fail(EXCEPTION_THROWN);
            }
        }

        return loginInput;
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

    /**
     * Getter.
     * 
     * @return the login input object.
     */
    public static LoginInput getAnotherLoginInputForCertificate() {
        if (anotherLoginInput == null) {
            try {
                OpenSSLKey key = new OpenSSLKeyMock(LoginTestConstants.USER_2_KEY_FILENAME);

                final String password = PASSWORD;

                anotherLoginInput = new LoginInput(LoginTestConstants.USER_2_CERTIFICATE, key, password);

            } catch (FileNotFoundException e) {
                TestCase.fail(EXCEPTION_THROWN);
            } catch (GeneralSecurityException e) {
                TestCase.fail(EXCEPTION_THROWN);
            } catch (IOException e) {
                TestCase.fail(EXCEPTION_THROWN);
            }
        }

        return anotherLoginInput;
    }

}
