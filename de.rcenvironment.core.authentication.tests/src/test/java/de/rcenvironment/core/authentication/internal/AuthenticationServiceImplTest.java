/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authentication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.authentication.AuthenticationService.LDAPAuthenticationResult;
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
