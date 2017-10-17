/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

import org.globus.gsi.CertUtil;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.authentication.AuthenticationService.LDAPAuthenticationResult;
import de.rcenvironment.core.authentication.AuthenticationService.X509AuthenticationResult;
import de.rcenvironment.core.authentication.AuthenticationTestConstants;
import de.rcenvironment.core.authentication.User;

/**
 * Test case for the implementation of the <code>AuthenticationService</code>.
 * 
 * @author Doreen Seider
 * @author Alice Zorn
 */
public class AuthenticationServiceImplTest {
    
    private AuthenticationServiceImpl authService;
    
    private int validityInDays = 7;

    /** Set up test environment. */
    @Before
    public void setUp() {
        authService = new AuthenticationServiceImpl();
        authService.bindConfigurationService(AuthenticationMockFactory.getConfigurationService());
        authService.activate(AuthenticationMockFactory.getBundleContextMock());
    }

    /**
     * Tests authentication for success.
     * 
     * Tests fail due to expired test certificates. As the code is currently not used and probably won't be used in the future, the tests
     * are ignored to reduce the maintenance effort. The related methods in are deprecated.
     * 
     * @throws Exception in error
     */
    @Test
    @Ignore
    public void testAuthenticateForSuccess() throws Exception {

        X509Certificate certificate = CertUtil.loadCertificate(getClass()
            .getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));
        OpenSSLKey key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.USERKEY_RCE_ENGINEER_PEM));

        X509AuthenticationResult result = authService.authenticate(certificate, key, AuthenticationTestConstants
            .PASSWORD_RCE_ENGINEER);
        assertEquals(X509AuthenticationResult.AUTHENTICATED, result);

    }

    /**
     * Tests authentication for failure.
     * 
     * Tests fail due to expired test certificates. As the code is currently not used and probably won't be used in the future, the tests
     * are ignored to reduce the maintenance effort. The related methods in are deprecated.
     * 
     * @throws Exception on error
     */
    @Test
    @Ignore
    public void testAuthenticateForSanity() throws Exception {

        // incorrect password

        X509Certificate certificate = CertUtil.loadCertificate(getClass()
            .getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));
        OpenSSLKey key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.USERKEY_RCE_ENGINEER_PEM));

        X509AuthenticationResult result = authService
            .authenticate(certificate, key, AuthenticationTestConstants.PASSWORD_RCE_ENEMY);
        assertEquals(X509AuthenticationResult.PASSWORD_INCORRECT, result);

        // private and public key do not belong together

        certificate = CertUtil.loadCertificate(getClass().getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));
        key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.KEY_RCE_ENEMY_PEM));

        result = authService.authenticate(certificate, key, AuthenticationTestConstants.PASSWORD_RCE_ENEMY);
        assertEquals(X509AuthenticationResult.PRIVATE_KEY_NOT_BELONGS_TO_PUBLIC_KEY, result);

        // not signed by trusted CA

        certificate = CertUtil.loadCertificate(getClass().getResourceAsStream(AuthenticationTestConstants.CERT_UNKNOWN_USER_PEM));
        key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.KEY_UNKNOWN_USER_PEM));

        result = authService.authenticate(certificate, key, AuthenticationTestConstants.PASSWORD_UNKNOWN_USER);
        assertEquals(X509AuthenticationResult.NOT_SIGNED_BY_TRUSTED_CA, result);

        // revoked

        certificate = CertUtil.loadCertificate(getClass().getResourceAsStream(AuthenticationTestConstants.CERT_RCE_ENEMY_PEM));
        key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.KEY_RCE_ENEMY_PEM));

        result = authService.authenticate(certificate, key, AuthenticationTestConstants.PASSWORD_RCE_ENEMY);
        assertEquals(X509AuthenticationResult.CERTIFICATE_REVOKED, result);

        // no password, but encrypted key

        certificate = CertUtil.loadCertificate(getClass().getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));
        key = new BouncyCastleOpenSSLKey(getClass().getResourceAsStream(AuthenticationTestConstants.USERKEY_RCE_ENGINEER_PEM));

        result = authService.authenticate(certificate, key, null);
        assertEquals(X509AuthenticationResult.PASSWORD_REQUIRED, result);

    }

    /**
     * Tests authentication for failure.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    @Ignore
    public void testAuthenticateForFailure() throws Exception {

        // no certificate
        try {
            OpenSSLKey key = new BouncyCastleOpenSSLKey(getClass()
                .getResourceAsStream(AuthenticationTestConstants.USERKEY_RCE_ENGINEER_PEM));
            authService.authenticate(null, key, AuthenticationTestConstants.PASSWORD_RCE_ENEMY);
            fail();

        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        // no private key
        try {
            X509Certificate certificate = CertUtil.loadCertificate(getClass()
                .getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));

            authService.authenticate(certificate, null, AuthenticationTestConstants.PASSWORD_RCE_ENEMY);

            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

    /**
     * Tests getting a CertificateUser for success.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testGetCertificateUserForSuccess() throws Exception {

        X509Certificate certificate = CertUtil.loadCertificate(getClass()
            .getResourceAsStream(AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM));
        User certificateUser = authService.createUser(certificate, validityInDays);

        assertTrue(certificateUser.isValid());
    }
    
    /**
     * Tests getting an LDAPUser for success.
     * 
     * @throws Exception if the test fails.
     */
    @Test
    public void testGetLdapUserForSuccess() throws Exception {
        User ldapUser = authService.createUser("testUser", validityInDays);
        assertTrue(ldapUser.isValid());
    }

    /**
     * Tests getting a proxy certificate for failure.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testGetProxyCertificateForFailure() throws Exception {

        // no certificate
        try {
            authService.createUser((X509Certificate) null, validityInDays);

            fail();

        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

    /**
     * Tests getting a proxy certificate for success.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testLoadCertificateForSuccess() throws Exception {

        X509Certificate certificate = authService.loadCertificate(System.getProperty(AuthenticationTestConstants.USER_DIR)
            + AuthenticationTestConstants.TESTRESOURCES_DIR + AuthenticationTestConstants.USERCERT_RCE_ENGINEER_PEM);

        assertNotNull(certificate);

    }

    /**
     * Tests getting a proxy certificate for failure.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testLoadCertificateForFailure() throws Exception {
        try {
            authService.loadCertificate(null);

            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * Tests getting a proxy certificate for success.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testLoadCertificateRevocationListsForSuccess() throws Exception {

        OpenSSLKey key = authService.loadKey(System.getProperty(AuthenticationTestConstants.USER_DIR)
            + AuthenticationTestConstants.TESTRESOURCES_DIR + AuthenticationTestConstants.USERKEY_RCE_ENGINEER_PEM);

        assertNotNull(key);

    }

    /**
     * Tests getting a proxy certificate for failure.
     * 
     * @throws Exception
     *             if the test fails.
     */
    @Test
    public void testLoadCertificateRevocationListsForFailure() throws Exception {

        try {
            authService.loadKey(null);

            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
    
    /**
     * Tests arguments of password and user id for failure.
     */
    @Test
    public void testLdapArgumentForFailure(){
        String uid = "";
        String password = "test";
        assertEquals(LDAPAuthenticationResult.PASSWORD__OR_USERNAME_INVALID, authService.authenticate(uid, password));

        uid = "_";
        password = "";
        assertEquals(LDAPAuthenticationResult.PASSWORD__OR_USERNAME_INVALID, authService.authenticate(uid, password));
    }
    
    
    /**
     * Tests authentication at LDAP for success.
     * 
     * Test server available. For data see: rce-closed-source/development/testing/unittests/servers/ldap.txt
     */
    @Test
    @Ignore // as data must be manually substituted before execution, this test is ignored during automated testing
    public void testLdapAuthenticationForSuccess(){
        String uid = "username";
        String password = "password";
        
        // if the intra-net is not available, don't perform the test
        try {
            InetAddress.getByName("server");
        } catch (UnknownHostException e) {
            return;
        }
        assertEquals(LDAPAuthenticationResult.AUTHENTICATED, authService.authenticate(uid, password));
    }
    
    /**
     * Tests authentication at LDAP for success.
     */
    @Test
    public void testCreateUser(){
        User user = authService.createUser(4);
        assertEquals(4, user.getValidityInDays());
    }
    
}
