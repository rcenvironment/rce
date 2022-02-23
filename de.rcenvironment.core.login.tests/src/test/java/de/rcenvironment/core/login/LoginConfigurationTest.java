/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
