/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

import junit.framework.TestCase;


/**
 * 
 * Test cases for the class <code>Permission</code>.
 * 
 * @author Andre Nurzenski
 */
public class PermissionTest extends TestCase {

    /**
     * The name of a permission.
     */
    private static final String START_PERMISSION = "de.rcenvironment.rce.communication:start";

    /**
     * The name of a permission.
     */
    private static final String STOP_PERMISSION = "de.rcenvironment.rce.communication:stop";

    /**
     * The name of a permission.
     */
    private static final String PERMISSION_DESCRIPTION = "permission to start and stop the communication bundle";

    /**
     * The class under test.
     */
    private Permission myPermission = null;

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
        myPermission = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test if an object can be constructed.
     * 
     */
    public void testPermissionForSuccess() {
        myPermission = new Permission(START_PERMISSION, PERMISSION_DESCRIPTION);
        myPermission = new Permission(START_PERMISSION);
    }

    /*
     * #################### Test for failure ####################
     */

    /*
     * #################### Test for sanity ####################
     */

    /**
     * 
     * Test if an object can be constructed.
     * 
     */
    public void testPermissionForSanity() {
        myPermission = new Permission(START_PERMISSION);
        Permission newPermission = new Permission(START_PERMISSION);
        Permission stopPermission = new Permission(STOP_PERMISSION);

        assertNotNull(myPermission);
        assertEquals(START_PERMISSION, myPermission.getID());
        assertEquals(newPermission, myPermission);
        assertFalse(myPermission.equals(stopPermission));
    }

}
