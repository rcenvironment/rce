/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * 
 * Test for the class SshUser.
 * 
 * @author Sebastian Holtappels
 */
public class SshAccountImplTest extends TestCase {

    private Log logger = LogFactory.getLog(SshAccountImplTest.class);

    /** Test. */
    @Test
    public void testValidationPositive() {
        List<SshAccountImpl> users = SshTestUtils.getValidUsers();
        for (SshAccountImpl user : users) {
            assertTrue("SshUser.validateUser returned false but true was expected",
                user.validate(SshTestUtils.getValidRoles(), logger));
        }
    }

    /** Test. */
    @Test
    public void testValidationUsername() {
        SshAccountImpl user = SshTestUtils.getValidUser();
        user.setUsername(null);
        assertFalse("SshUser.validateUser() returned true but false was expected", user.validate(SshTestUtils.getValidRoles(), logger));
        user.setUsername("");
        assertFalse("SshUser.validateUser() returned true but false was expected", user.validate(SshTestUtils.getValidRoles(), logger));
    }

    // if ((password == null || password.isEmpty())
    /** Test. */
    @Test
    public void testValidationPassword() {
        SshAccountImpl user = SshTestUtils.getValidUser();
        user.setPassword(null);
        assertFalse("SshUser.validateUser returned true but false was expected.", user.validate(SshTestUtils.getValidRoles(), logger));
        user.setPassword("");
        assertFalse("SshUser.validateUser returned true but false was expected.", user.validate(SshTestUtils.getValidRoles(), logger));
    }

    // if (role == null)
    /** Test. */
    @Test
    public void testValidationRole() {
        SshAccountImpl user = SshTestUtils.getValidUser();
        user.setRole(null);
        assertFalse("SshUser.validateUser returned true but false was expected ", user.validate(SshTestUtils.getValidRoles(), logger));
    }

    // role of user exist
    /** Test. */
    @Test
    public void testValidationExistingRoles() {
        SshAccountImpl user = SshTestUtils.getValidUser();
        user.setRole("hkjfhjgdfsghj");
        assertFalse("SshUser.validateUser() returned true but false was expected. "
            + "(Note: Do not define a test role with the name hkjfhjgdfsghj)",
            user.validate(SshTestUtils.getValidRoles(), logger));
    }
}
