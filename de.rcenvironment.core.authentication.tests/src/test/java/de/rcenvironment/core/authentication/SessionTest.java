/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Test case {@link Session}.
 *
 * @author Doreen Seider
 */
public class SessionTest extends TestCase  {

    /**
     * Test {@link User}.
     */
    private User user;
    

    @Override
    public void setUp() throws Exception {
        user = EasyMock.createNiceMock(User.class);
    }

    /**
     * Test.
     *
     * @throws Exception Thrown if an error occur.
     */
    public void testGetProxyCertificate() throws Exception {
        try {
            Session.getInstance().getUser();
            fail();
        } catch (AuthenticationException e) {
            assertTrue(true);
        }
        Session.create(user);
        assertTrue(user == Session.getInstance().getUser());
        Session.getInstance().destroy();
    }
    
    /**
     * Test.
     *
     * @throws Exception Thrown if an error occur.
     */
    public void testCreate() throws Exception {
        try {
            Session.getInstance();
        } catch (AuthenticationException e) {
            assertTrue(true);
        }
        
        // test create with User as input
        Session.create(user);
        assertNotNull(Session.getInstance());
        
        
        // test create with userID as input
        int validityInDays = 7;
        
        Session.create("testUserID", validityInDays);
        assertNotNull(Session.getInstance());
        
        Session.getInstance().destroy();
    }
    
    /**
     * Test.
     *
     * @throws Exception Thrown if an error occur.
     */
    public void testGetInstance() throws Exception {
        Session.create(user);
        Session.getInstance().destroy();
        try {
            Session.getInstance();
            fail();
        } catch (AuthenticationException e) {
            assertTrue(true);
        }
        Session.create(user);
        Session.getInstance();
        Session.create(user);
        assertTrue(Session.getInstance() != null);
        Session.getInstance().destroy();
    }
    
    /**
     * Test.
     * 
     * @throws Exception Thrown if an error occur.
     */
    public void testDestroy() throws Exception {
        Session.create(user);
        
        Session.getInstance().destroy();
        try {
            Session.getInstance();
            fail();
        } catch (AuthenticationException e) {
            assertTrue(true);
        }

    }
}
