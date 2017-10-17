/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.rbac;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;


/**
 * Test cases for the class <code>Role</code>.
 * 
 * @author Andre Nurzenski
 */
public class RoleTest extends TestCase {

    /**
     * A permission name.
     */
    private static final String START_PERMISSION = "de.rcenvironment.rce.communication:start";

    /**
     * A permission name.
     */
    private static final String STOP_PERMISSION = "de.rcenvironment.rce.communication:stop";

    /**
     * A role name.
     */
    private static final String DLR_ROLE = "de.dlr.sc";

    /**
     * A role name.
     */
    private static final String SCAI_ROLE = "de.fraunhofer.scai";

    /**
     * A bundle name.
     */
    private static final String ROLE_DESCRIPTION = "SC geeks";

    /**
     * The class under test.
     */
    private Role myRole = null;

    @Override
    protected void setUp() throws Exception {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(new Permission(START_PERMISSION));

        myRole = new Role(DLR_ROLE, ROLE_DESCRIPTION, permissions);
    }

    @Override
    protected void tearDown() throws Exception {
        myRole = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testGetPermissionsForSuccess() {
        myRole.getPermissions();
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testHasPermissionForSuccess() {
        Permission permission = new Permission(START_PERMISSION);
        myRole.hasPermission(permission);
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testEqualsForSuccess() {
        Role role = new Role(SCAI_ROLE, new HashSet<Permission>());
        myRole.equals(role);
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testToStringForSuccess() {
        myRole.toString();
    }

    /*
     * #################### Test for failure ####################
     */

    /*
     * #################### Test for sanity ####################
     */

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testGetPermissionsForSanity() {
        Set<Permission> permissions = myRole.getPermissions();
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        
        Role role = new Role("role", "decrtiption", null);
        permissions = role.getPermissions();
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
        
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testHasPermissionForSanity() {
        Permission startPermission = new Permission(START_PERMISSION);
        Permission stopPermission = new Permission(STOP_PERMISSION);

        assertTrue(myRole.hasPermission(startPermission));
        assertFalse(myRole.hasPermission(stopPermission));
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testEqualsForSanity() {
        Role role = new Role(SCAI_ROLE, new HashSet<Permission>());

        assertTrue(myRole.equals(myRole));
        assertFalse(myRole.equals(role));

        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(new Permission(START_PERMISSION));
        role = new Role(DLR_ROLE, permissions);

        assertTrue(myRole.equals(role));
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testToStringForSanity() {
        String role = myRole.toString();
        
        assertEquals("de.dlr.sc - [de.rcenvironment.rce.communication:start]", role);
    }

}
