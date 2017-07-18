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
 * 
 * Test cases for the class <code>Subject</code>.
 * 
 * @author Andre Nurzenski
 */
public class SubjectTest extends TestCase {

    /**
     * A role name for the tests.
     */
    private static final String DLR_ROLE_NAME = "de.dlr.sc";

    /**
     * A role name for the tests.
     */
    private static final String GNS_SYSTEMS_ROLE_NAME = "de.gns-systems";

    /**
     * A permission name for the tests.
     */
    private static final String PERMISSION_FOO_BLUB = "de.rcenvironment.foo:blubb";

    /**
     * A permission name for the tests.
     */
    private static final String PERMISSION_SHIP_ENTER = "de.rcenvironment.ship:enter";

    /**
     * A permission name for the tests.
     */
    private static final String PERMISSION_FOO_BLA = "de.rcenvironment.foo:bla";

    /**
     * A permission name for the tests.
     */
    private static final String PERMISSION_SHIP_LANDING = "de.rcenvironment.ship:landing";

    /**
     * Constant for a user ID.
     */
    private static final String TEST_SUBJECT_ID = "CN=Rainer Tester,OU=SC,O=DLR,L=Cologne,ST=NRW,C=DE";

    /**
     * The class under test.
     */
    private Subject mySubject = null;

    /**
     * A role for the tests.
     */
    private Role myDLRRole = null;

    /**
     * A role for the tests.
     */
    private Role myGNSSystemsRole = null;

    @Override
    protected void setUp() throws Exception {
        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(new Permission(PERMISSION_SHIP_LANDING));
        permissions.add(new Permission(PERMISSION_FOO_BLA));
        permissions.add(new Permission(PERMISSION_SHIP_ENTER));
        permissions.add(new Permission(PERMISSION_FOO_BLUB));
        myDLRRole = new Role(DLR_ROLE_NAME, permissions);
        permissions = new HashSet<Permission>();
        permissions.add(new Permission(PERMISSION_SHIP_LANDING));
        permissions.add(new Permission(PERMISSION_SHIP_ENTER));
        myGNSSystemsRole = new Role(GNS_SYSTEMS_ROLE_NAME, permissions);

        Set<Role> roles = new HashSet<Role>();
        roles.add(myDLRRole);
        roles.add(myGNSSystemsRole);
        mySubject = new Subject(TEST_SUBJECT_ID, roles);
    }

    @Override
    protected void tearDown() throws Exception {
        mySubject = null;
        myDLRRole = null;
        myGNSSystemsRole = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testHasRoleForSuccess() {
        mySubject.hasRole(myDLRRole);

    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testGetRolesForSuccess() {
        mySubject.getRoles();
    }

    /**
     * 
     * Test the method for success.
     * 
     */
    public void testToStringForSuccess() {
        mySubject.toString();
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
    public void testHasRoleForSanity() {
        assertTrue(mySubject.hasRole(myDLRRole));

        Set<Permission> permissions = new HashSet<Permission>();
        permissions.add(new Permission(PERMISSION_SHIP_LANDING));
        Role adminRole = new Role("no.name", permissions);
        assertFalse(mySubject.hasRole(adminRole));

        permissions = new HashSet<Permission>();
        permissions.add(new Permission(PERMISSION_SHIP_LANDING));
        permissions.add(new Permission(PERMISSION_SHIP_ENTER));
        Role userRole = new Role(GNS_SYSTEMS_ROLE_NAME, permissions);
        assertTrue(mySubject.hasRole(userRole));

        permissions = new HashSet<Permission>();
        permissions.add(new Permission(PERMISSION_SHIP_LANDING));
        permissions.add(new Permission(PERMISSION_FOO_BLA));
        Role debugRole = new Role("Debugger", permissions);
        assertFalse(mySubject.hasRole(debugRole));
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testGetRolesForSanity() {
        Set<Role> roles = mySubject.getRoles();
        assertNotNull(roles);
        assertFalse(roles.isEmpty());

        Subject subject = new Subject("subject", "description", null);
        roles = subject.getRoles();
        assertNotNull(roles);
        assertTrue(roles.isEmpty());

    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testToStringForSanity() {
        String entity = mySubject.toString();

        assertTrue(entity.contains(TEST_SUBJECT_ID));
        assertTrue(entity.contains(DLR_ROLE_NAME));
        assertTrue(entity.contains(GNS_SYSTEMS_ROLE_NAME));
        assertTrue(entity.contains(PERMISSION_FOO_BLA));
        assertTrue(entity.contains(PERMISSION_FOO_BLUB));
        assertTrue(entity.contains(PERMISSION_SHIP_ENTER));
        assertTrue(entity.contains(PERMISSION_SHIP_LANDING));
        // the test doesn't cover that the permissions are associated to the correct roles in the Subject's string representation; as this
        // is "just" the string representation and as the classes are currently not in use and are likely to be removed anyway if
        // authorization is actually added, this lack of coverage is acceptable -- seid_do
    }

    /**
     * 
     * Test the method for sanity.
     * 
     */
    public void testEqualsForSanity() {
        boolean isEqual = false;

        isEqual = mySubject.equals(mySubject);
        assertTrue(isEqual);

        isEqual = mySubject.equals(new String());
        assertFalse(isEqual);

        isEqual = mySubject.equals(new Subject(TEST_SUBJECT_ID, new HashSet<Role>()));
        assertTrue(isEqual);
    }

}
