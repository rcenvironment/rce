/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.globus.gsi.OpenSSLKey;

import de.rcenvironment.core.authentication.User.Type;

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

    private static LoginInput loginInputCert;

    private static LoginInput loginInputLdap;

    private static X509Certificate certificate2 = null;

    private static OpenSSLKey key2 = null;

    /**
     * Test.
     */
    public void setUp() {

        certificate2 = EasyMock.createNiceMock(X509Certificate.class);
        key2 = EasyMock.createNiceMock(OpenSSLKey.class);

        loginInputCert = new LoginInput(certificate2, key2, "kannwas");
        loginInputLdap = new LoginInput("f_rcelda", "test987!");
    }

    /**
     * Test.
     */
    public void testGetCertificate() {
        assertEquals(loginInputCert.getCertificate(), certificate2);
    }

    /**
     * Test.
     */
    public void testGetKey() {
        assertEquals(loginInputCert.getKey(), key2);
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
        assertEquals(loginInputCert.getPassword(), "kannwas");
        assertEquals(loginInputLdap.getPassword(), "test987!");
    }

    /**
     * Test.
     */
    public void testGetType() {
        assertEquals(loginInputCert.getType(), Type.certificate);
        assertEquals(loginInputLdap.getType(), Type.ldap);

    }

    /**
     * Test.
     */
    public void testAuthenticateForSuccess() {

        try {
            X509Certificate certificate = LoginTestConstants.USER_1_CERTIFICATE;
            OpenSSLKey key = new OpenSSLKeyMock(LoginTestConstants.USER_1_KEY_FILENAME);

            final String password = PASSWORD;

            LoginInput input = new LoginInput(certificate, key, password);

            assertTrue(certificate.getIssuerDN().equals(input.getCertificate().getIssuerDN()));
            assertNotNull(input.getKey());
            assertTrue(password.equals(input.getPassword()));

        } catch (GeneralSecurityException e) {
            fail(EXCEPTION_THROWN);
        } catch (IOException e) {
            fail(EXCEPTION_THROWN + e);
        }

    }

    /**
     * Test.
     */
    public void testAuthenticateForFailure() {

        // no key
        try {
            X509Certificate certificate = LoginTestConstants.USER_1_CERTIFICATE;
            final String password = PASSWORD;

            new LoginInput(certificate, null, password);

            fail(EXCEPTION_HAS_TO_BE_THROWN);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        // no certificate
        try {
            OpenSSLKey key = new OpenSSLKeyMock(LoginTestConstants.USER_1_KEY_FILENAME);
            final String password = PASSWORD;

            new LoginInput(null, key, password);

            fail(EXCEPTION_HAS_TO_BE_THROWN);
        } catch (GeneralSecurityException e) {
            fail(EXCEPTION_THROWN);
        } catch (IOException e) {
            fail(EXCEPTION_THROWN);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

}

/**
 * Mock of <code>OpenSSLKey</code> for the purpose of testing.
 * 
 * @author Doreen Seider
 */
class OpenSSLKeyMock extends OpenSSLKey {

    /**
     * Constructor.
     * 
     * @param file An empty string.
     * @throws GeneralSecurityException if an exception occurs.
     * @throws IOException if an exception occurs.
     * @throws GeneralSecurityException
     */
    public OpenSSLKeyMock(String file) throws IOException, GeneralSecurityException {
        super(file);
    }

    public OpenSSLKeyMock(InputStream is) throws IOException, GeneralSecurityException {
        super(is);
    }

    @Override
    protected byte[] getEncoded(PrivateKey arg0) {
        return null;
    }

    @Override
    protected PrivateKey getKey(String arg0, byte[] arg1) throws GeneralSecurityException {
        return null;
    }

}
