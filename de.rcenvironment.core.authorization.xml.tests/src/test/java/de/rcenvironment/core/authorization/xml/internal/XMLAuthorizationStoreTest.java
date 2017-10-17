/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.xml.internal;

import java.util.Set;

import org.junit.Ignore;

import junit.framework.TestCase;
import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test case for <code>XMLAuthorizationStore</code>.
 * 
 * @author Doreen Seider
 */
// note: test deactivated until the (currently unused) bundle is reworked or removed - misc_ro
@Ignore
public class XMLAuthorizationStoreTest extends TestCase {

    /**
     * Constant.
     */
    private static final String SUBJECT_ID = "CN=Rainer Tester,OU=SC,O=DLR,L=Cologne,ST=NRW,C=DE";

    /**
     * Constant.
     */
    private static final String ROLE_ID = "de.dlr.sc";

    /**
     * Constant.
     */
    private static final String PERMISSION_ID = "de.rcenvironment.foo:bla";

    /**
     * Constant.
     */
    private static final String EXCEPTION_HAS_TO_BE_THROWN = "An exception has to be thrown.";

    /**
     * Class under test.
     */
    private XMLAuthorizationStore myStore;

    @Override
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        myStore = new XMLAuthorizationStore();
        myStore.bindConfigurationService(XMLAuthorizationMockFactory.getConfigurationService());
        myStore.activate(XMLAuthorizationMockFactory.getBundleContextMock());        
        myStore.initialize();
    }

    /**
     * 
     * Test for success.
     * 
     * @throws Exception if an error occurs.
     * 
     */
    public void testInitialize() throws Exception {
        myStore.bindConfigurationService(XMLAuthorizationMockFactory.getAnotherConfigurationService());
        myStore.activate(XMLAuthorizationMockFactory.getBundleContextMock());        
        myStore.initialize();
    }
    
    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupPermissionForSuccess() {
        myStore.lookupPermission(PERMISSION_ID);
    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupRoleForSuccess() {
        myStore.lookupRole(ROLE_ID);

    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupSubjectForSuccess() {

        myStore.lookupSubject(SUBJECT_ID);

    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupPermissionForSanity() {

        Permission permission = myStore.lookupPermission(PERMISSION_ID);
        assertTrue(permission.getID().equals(PERMISSION_ID));
        assertTrue(permission.getDescription().equals("Do bla"));

    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupRoleForSanity() {

        Role role = myStore.lookupRole(ROLE_ID);
        assertTrue(role.getID().equals(ROLE_ID));
        assertTrue(role.getDescription().equals("SC Geeks"));
        Set<Permission> permissions = role.getPermissions();
        assertEquals(8, permissions.size());
        assertTrue(permissions.contains(new Permission(PERMISSION_ID)));
        assertTrue(permissions.contains(new Permission("de.rcenvironment.foo:wuff")));

    }

    /**
     * 
     * Test for sanity.
     * 
     */
    public void testLookupSubjectForSanity() {

        Subject subject = myStore.lookupSubject(SUBJECT_ID);
        assertTrue(subject.getID().equals(SUBJECT_ID));
        assertTrue(subject.getDescription().equals("Rainer Tester"));
        Set<Role> roles = subject.getRoles();
        assertEquals(4, roles.size());
        assertTrue(roles.contains(new Permission(ROLE_ID)));
        assertTrue(roles.contains(new Permission("de.fraunhofer.scai")));

    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupPermissionForFailure() {
        try {
            myStore.lookupPermission(null);
            fail(EXCEPTION_HAS_TO_BE_THROWN);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupRoleForFailure() {
        try {
            myStore.lookupRole(null);
            fail(EXCEPTION_HAS_TO_BE_THROWN);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test for success.
     * 
     */
    public void testLookupSubjectForFailure() {
        try {
            myStore.lookupPermission(null);
            fail(EXCEPTION_HAS_TO_BE_THROWN);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
