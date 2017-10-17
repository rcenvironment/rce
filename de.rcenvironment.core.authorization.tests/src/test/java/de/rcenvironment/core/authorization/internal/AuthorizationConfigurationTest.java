/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import junit.framework.TestCase;

/**
 * 
 * Test case for the class <code>AuthorizationConfiguration</code>.
 *
 * @author Doreen Seider
 */
public class AuthorizationConfigurationTest extends TestCase  {
    
    /**
     * The class under test.
     */
    private AuthorizationConfiguration myAuthorizationConfiguration = null;
    
    @Override
    public void setUp() throws Exception {
        myAuthorizationConfiguration = new AuthorizationConfiguration();
        myAuthorizationConfiguration.setStore(AuthorizationStoreDummy.XML_STORE);
    }
   
    /**
     * 
     * Tests getting the store for success.
     *
     */
    public void testGetStoreForSuccess() {
        String store = myAuthorizationConfiguration.getStore();
        assertNotNull(store);
    }
    
    /**
     * 
     * Tests getting the store for sanity.
     *
     */
    public void testGetStoreForSanity() {
        String store = myAuthorizationConfiguration.getStore();
        assertEquals(AuthorizationStoreDummy.XML_STORE, store);
    }
}
