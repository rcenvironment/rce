/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization.internal;

import java.util.HashSet;

import junit.framework.TestCase;
import de.rcenvironment.core.authorization.rbac.Permission;
import de.rcenvironment.core.authorization.rbac.Role;
import de.rcenvironment.core.authorization.rbac.Subject;

/**
 * 
 * Test cases for <code>AuthorizationServiceImpl</code>.
 * 
 * @author Doreen Seider
 */
public class AuthorizationServiceImplTest extends TestCase {

    /**
     * Class under test.
     */
    private AuthorizationServiceImpl myService = null;

    @Override
    public void setUp() throws Exception {
        myService = new AuthorizationServiceImpl();
        myService.bindConfigurationService(AuthorizationMockFactory.getConfigurationService());
        myService.activate(AuthorizationMockFactory.getBundleContextMock());
    }
     
    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetPermissionForSuccess() throws Exception {
        myService.getPermission(AuthorizationStoreDummy.PERMISSION_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetPermissionsForSuccess() throws Exception {
        myService.getPermissions(AuthorizationStoreDummy.SUBJECT_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRoleForSuccess() throws Exception {
        myService.getRole(AuthorizationStoreDummy.ROLE_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRolesForSuccess() throws Exception {
        myService.getRoles(AuthorizationStoreDummy.SUBJECT_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetSubjectForSuccess() throws Exception {
        myService.getSubject(AuthorizationStoreDummy.SUBJECT_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasPermissionForSuccess() throws Exception {
        myService.hasPermission(AuthorizationStoreDummy.SUBJECT_ID, AuthorizationStoreDummy.PERMISSION);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasRoleForSuccess() throws Exception {
        myService.hasRole(AuthorizationStoreDummy.SUBJECT_ID, AuthorizationStoreDummy.ROLE);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception if the test fails.
     */
    public void testGetPermissionForSanity() throws Exception {
        Permission permission = myService.getPermission(AuthorizationStoreDummy.PERMISSION_ID);
        assertEquals(AuthorizationStoreDummy.PERMISSION, permission);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetPermissionsForSanity() throws Exception {
        myService.getPermissions(AuthorizationStoreDummy.SUBJECT_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRoleForSanity() throws Exception {
        Role role = myService.getRole(AuthorizationStoreDummy.ROLE_ID);
        assertEquals(AuthorizationStoreDummy.ROLE, role);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRolesForSanity() throws Exception {
        myService.getRoles(AuthorizationStoreDummy.SUBJECT_ID);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetSubjectForSanity() throws Exception {
        Subject subject = myService.getSubject(AuthorizationStoreDummy.SUBJECT_ID);
        assertEquals(AuthorizationStoreDummy.SUBJECT, subject);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasPermissionForSanity() throws Exception {
        boolean hasPermission = myService.hasPermission(AuthorizationStoreDummy.SUBJECT_ID, AuthorizationStoreDummy.PERMISSION);
        assertTrue(hasPermission);
        hasPermission = myService.hasPermission(AuthorizationStoreDummy.SUBJECT_ID, new Permission(AuthorizationStoreDummy.PERMISSION_ID
                + "sjak"));
        assertFalse(hasPermission);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasRoleForSanity() throws Exception {
        boolean hasRole = myService.hasRole(AuthorizationStoreDummy.SUBJECT_ID, AuthorizationStoreDummy.ROLE);
        assertTrue(hasRole);
        hasRole = myService.hasRole(AuthorizationStoreDummy.SUBJECT_ID, new Role("unknown.role", new HashSet<Permission>()));
        assertFalse(hasRole);
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetPermissionForFailure() throws Exception {
        Permission permission = myService.getPermission(AuthorizationStoreDummy.PERMISSION_ID);
        assertEquals(AuthorizationStoreDummy.PERMISSION, permission);

    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetPermissionsForFailure() throws Exception {
        try {
            myService.getPermissions(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRoleForFailure() throws Exception {
        try {
            myService.getRole(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetRolesForFailure() throws Exception {
        try {
            myService.getRoles(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testGetSubjectForFailure() throws Exception {
        try {
            myService.getSubject(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasPermissionForFailure() throws Exception {
        try {
            myService.hasPermission(null, AuthorizationStoreDummy.PERMISSION);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            myService.hasPermission(AuthorizationStoreDummy.SUBJECT_ID, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * 
     * Test.
     * 
     * @throws Exception
     *             if the test fails.
     */
    public void testHasRoleForFailure() throws Exception {
        try {
            myService.hasRole(null, AuthorizationStoreDummy.ROLE);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            myService.hasRole(AuthorizationStoreDummy.SUBJECT_ID, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
