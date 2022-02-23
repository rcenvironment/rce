/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication.internal;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authentication.LDAPUser;
import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.authentication.User.Type;


/**
 * Test cases for {@link LDAPUser}.
 *
 * @author Alice Zorn
 */
public class LDAPUserTest {

    /**
     * A proxy certificate for the tests.
     */
    private User myUser = null;
    
    private String domain = "testDomain";
    
    private String userId = "testUserId";
    
    private int validityInDays = 7;


    /**
     * Set up test.
     * 
     * @throws Exception if an error occurs.
     */
    @Before
    public void setUp() throws Exception {
        myUser = new LDAPUser(userId, validityInDays, domain);
    }

    /**
     * Tear down test.
     * 
     * @throws Exception if an error occurs.
     */
    @After
    public void tearDown() throws Exception {
        myUser = null;
    }

    /**
     * Test if the user id can be retrieved from a LDAP user.
     */
    @Test
    public void testGetUserIDForSuccess() {
        Assert.assertEquals(userId, myUser.getUserId());
    }
    
    /**
     * Test if the domain can be retrieved from a LDAP user.
     */
    @Test
    public void testGetDomainForSuccess() {
        Assert.assertEquals(domain, myUser.getDomain());
    }
    
    /**
     * Test if the type is correct.
     */
    @Test
    public void testGetTypeForSuccess(){
        Assert.assertEquals(Type.ldap, myUser.getType());
    }
}
