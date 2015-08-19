/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.login;

import junit.framework.TestCase;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test case for the class <code>LoginSettingsImpl</code>.
 * 
 * @author Doreen Seider
 */
public class LoginConfigurationTest extends TestCase {

    private static final String USER_CERT = System.getProperty("user.dir") + "/src/test/resources/usercert_rainertester.pem";

    private static final String USER_KEY = System.getProperty("user.dir") + "/src/test/resources/userkey_rainertester.pem";

    private LoginConfiguration loginConfiguration;


    @Override
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        loginConfiguration = new LoginConfiguration();
        ConfigurationService confMockService = LoginMockFactory.getInstance().getConfigurationServiceMock();
        loginConfiguration = confMockService.getConfiguration(LoginTestConstants.BUNDLE_SYMBOLIC_NAME, LoginConfiguration.class);
    }

    @Override
    public void tearDown() throws Exception {
        loginConfiguration = null;
    }

    /**
     * Tests getting the certificate authorities set for success.
     */
    public void testGetCertificatePathForSuccess() {
        String certificatePath = loginConfiguration.getCertificateFile();
        assertNotNull(certificatePath);
        assertTrue(certificatePath.equals(LoginTestConstants.USER_1_CERTIFICATE_FILENAME));
    }

    /**
     * Tests getting the certificate revocation lists set for success.
     */
    public void testGetKeyPathForSuccess() {
        String keyPath = loginConfiguration.getKeyFile();
        assertNotNull(keyPath);
        assertTrue(keyPath.equals(LoginTestConstants.USER_1_KEY_FILENAME));
    }

    /**
     * Tests getting the certificate revocation lists set for success.
     */
    public void testIsAutoStartEnabledForSuccess() {
        assertTrue(!loginConfiguration.getAutoLogin());
    }

    /**
     * Tests getting the certificate revocation lists set for success.
     */
    public void testGetAutoPasswordForSuccess() {
        assertEquals(loginConfiguration.getAutoLoginPassword(), "test");
    }
    
    /**
     * Tests getting the LDAP revocation lists set for success.
     */
    public void testGetAutoLoginPasswordLdapForSuccess(){
        loginConfiguration.setAutoLoginPassword("test2");
        assertEquals(loginConfiguration.getAutoLoginPassword(), "test2");
    }
    
    /**
     * Tests getting the LDAP revocation lists set for success.
     */
    public void testGetUsernameLDAPForSuccess(){
        loginConfiguration.setLdapUsername("testUser");
        assertEquals(loginConfiguration.getLdapUsername(), "testUser");
    }
    
    /**
     * Tests getting the LDAP revocation lists set for success.
     */
    public void testGetModeForSuccess(){
        loginConfiguration.setAutoLoginMode("testMode");
        assertEquals(loginConfiguration.getAutLoginMode(), "testMode");
    }
    
    /**
     * Tests getting the LDAP revocation lists set for success.
     */
    public void testSetValidityInDaysForSuccess(){
        loginConfiguration.setValidityInDays(7);
        assertEquals(loginConfiguration.getValidityInDays(), 7);
    }
    
}
