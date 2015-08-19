/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.authentication;

import java.util.Calendar;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.authentication.User.Type;


/**
 * Test class for the User.
 *
 * @author Alice Zorn
 */
public class UserTest {
    
    /**
     * Dummy class extending abstract {@link User}.
     *
     * @author Alice Zorn
     */
    private class TestUser extends User {

        private static final long serialVersionUID = -6745646455466320734L;
        private final String userId;
        private final String domain;
        
        /**
         * 
         * Constructor.
         * 
         * @param userId 
         * @param domain 
         */
        public TestUser(String userId, String domain, int validityInDays){
            super(validityInDays);
            this.userId = userId;
            this.domain = domain;
        }
        
        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public String getDomain() {
            return domain;
        }

        @Override
        public Type getType() {
            return null;
        }
        
    }
    
    
    private TestUser myUser;
    private int validityInDays = 7;

    /**
     * Set up the test.
     * 
     * @throws Exception if an error occurs.
     */
    @Before
    public void setUp() throws Exception {
        myUser = new TestUser("k", "i", validityInDays);
    }
    
    /**
     * Test if the certificate user is still valid after almost one day (which should be the minimum validity).
     * 
     */
    @Test
    public void testTimeUntilValidForSuccess() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 1);
        // subtract 10min to ensure validity of the User even if it is only valid for 1 day.
        final int safetyThreshold = -10;
        cal.add(Calendar.MINUTE, safetyThreshold);
        Assert.assertTrue(myUser.getTimeUntilValid().after(cal.getTime()));
    }

    /**
     * Test if the validityInDays input variable is checked for > 0.
     */
    @Test
    public void testUser(){
        try {
            new TestUser("test", "domain", 0);
            Assert.fail();
        } catch (IllegalArgumentException e){
            e = null;
        }
        
        final int validity = -1;
        try {
            new TestUser("test", "domain", validity);
            Assert.fail();
        } catch (IllegalArgumentException e){
            e = null;
        }
        
    }
    
    /**
     * Test if the certificate user is still valid in context to the RCE-System.
     */
    @Test
    public void testIsValidForSuccess() {
        Assert.assertTrue(myUser.isValid());
    }
    
    /**
     * Test getValidityInDays.
     */
    @Test
    public void testGetValidityInDaysForSuccess(){
        Assert.assertTrue(myUser.getValidityInDays() == validityInDays);
    }
    
    /**
     * Test toString.
     */
    @Test
    public void testToStringForSuccess() {
        Assert.assertNotNull(myUser.toString());
    }

    /**
     * Test getTimestamp.
     */
    @Test
    public void testGetTimestampForSuccess() {
        final Date now = new Date();
        Assert.assertTrue(myUser.getTimeUntilValid().after(now));
    }
    
    /**
     * Test Enum Type (only to improve coverage).
     */
    @Test
    public void testEnumType(){
        Type.certificate.toString();
        Type.ldap.toString();
    }

    /**
     * Test if the Users are the same.
     */
    @Test
    public void testSameForSuccess(){
        final User user1 = new TestUser("a", "b", validityInDays);
        final User user2 = new TestUser("a", "b", validityInDays);
        final User user3 = new TestUser("c", "d", validityInDays);
        
        Assert.assertTrue(user1.same(user2));
        Assert.assertFalse(user1.same(user3));
    }
}
