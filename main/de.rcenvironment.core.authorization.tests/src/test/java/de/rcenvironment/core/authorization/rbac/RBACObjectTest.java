/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

import junit.framework.TestCase;


/**
 * Test cases for the class <code>AuthorizationObject</code>.
 * 
 * @author Andre Nurzenski
 */
public class RBACObjectTest extends TestCase {

    /**
     * The name of the authorization object to test.
     */
    private static final String AUTHORIZATION_OBJECT_NAME_1 = "de.rcenvironment.samples.one";

    /**
     * The name of the authorization object to test.
     */
    private static final String AUTHORIZATION_OBJECT_NAME_2 = "de.rcenvironment.samples.two";

    /**
     * The name of the authorization object to test.
     */
    private static final String AUTHORIZATION_OBJECT_DESCRIPTION = "test object";

    /**
     * The class under test.
     */
    private RBACObject myAuthorizationObject = null;

    /**
     * The class under test.
     */
    private RBACObject myAuthorizationObjectWithDescription = null;

    /**
     * 
     * Creates a <code>SecurityObject</code> for the tests.
     * 
     */
    public RBACObjectTest() {
        myAuthorizationObject = new RBACObject(AUTHORIZATION_OBJECT_NAME_1) {

            /**
             * 
             */
            private static final long serialVersionUID = -6781448941475751210L;

        };

        myAuthorizationObjectWithDescription = new RBACObject(AUTHORIZATION_OBJECT_NAME_2, AUTHORIZATION_OBJECT_DESCRIPTION) {

            /**
             * 
             */
            private static final long serialVersionUID = -6781448941475751210L;

        };
    }


    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test the method for success.
     * 
     */
    public final void testGetIDForSuccess() {
        myAuthorizationObject.getID();
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public final void testGetDescriptionForSuccess() {
        myAuthorizationObject.getDescription();
        myAuthorizationObjectWithDescription.getDescription();
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public final void testEqualsObjectForSuccess() {
        myAuthorizationObject.equals(myAuthorizationObject);
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public final void testToStringForSuccess() {
        myAuthorizationObject.toString();
    }

    /*
     * #################### Test for failure ####################
     */

    // Nothing to do here.
    /*
     * #################### Test for sanity ####################
     */

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public final void testGetIDForSanity() {
        String name = myAuthorizationObject.getID();

        assertNotNull(name);
        assertEquals(AUTHORIZATION_OBJECT_NAME_1, name);
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public final void testGetDescriptionForSanity() {
        String description = myAuthorizationObject.getDescription();

        assertNotNull(description);
        assertEquals("", description);

        description = myAuthorizationObjectWithDescription.getDescription();

        assertNotNull(description);
        assertEquals(AUTHORIZATION_OBJECT_DESCRIPTION, description);

    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public final void testEqualsObjectForSanity() {
        boolean isEqual = myAuthorizationObject.equals(myAuthorizationObject);
        assertTrue(isEqual);

        isEqual = myAuthorizationObject.equals(null);
        assertFalse(isEqual);

        isEqual = myAuthorizationObject.equals(new String(""));
        assertFalse(isEqual);

        isEqual = myAuthorizationObject.equals(myAuthorizationObjectWithDescription);
        assertFalse(isEqual);
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public final void testToStringForSanity() {
        String securityObjectString = myAuthorizationObject.toString();

        assertNotNull(securityObjectString);
        assertEquals(AUTHORIZATION_OBJECT_NAME_1, securityObjectString);
    }

}
