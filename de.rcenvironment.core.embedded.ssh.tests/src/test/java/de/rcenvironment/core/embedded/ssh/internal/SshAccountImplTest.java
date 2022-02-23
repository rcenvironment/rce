/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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

    private static final String SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED = 
        "SshUser.validateUser returned true but false was expected.";
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
        user.setLoginName(null);
        assertFalse("SshUser.validateUser() returned true but false was expected", user.validate(SshTestUtils.getValidRoles(), logger));
        user.setLoginName("");
        assertFalse("SshUser.validateUser() returned true but false was expected", user.validate(SshTestUtils.getValidRoles(), logger));
    }

    // if ((password == null || password.isEmpty())
    /** Test. */
    @Test
    public void testValidationPassword() {
        SshAccountImpl user = SshTestUtils.getValidUser();
        user.setPassword(null);
        assertFalse(SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED, user.validate(SshTestUtils.getValidRoles(), logger));
        user.setPassword("");
        assertFalse(SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED, user.validate(SshTestUtils.getValidRoles(), logger));
    }
    
    /** Test. */
    @Test
    public void testValidationPublicKey() {
        SshAccountImpl user = SshTestUtils.getValidPublicKeyUser();
        user.setPublicKey(null);
        assertFalse(SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED, user.validate(SshTestUtils.getValidRoles(), logger));
        user.setPublicKey("");
        assertFalse(SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED, user.validate(SshTestUtils.getValidRoles(), logger));
        user.setPublicKey("some_invalid_key");
        assertFalse(SSH_USER_VALIDATE_USER_RETURNED_TRUE_BUT_FALSE_WAS_EXPECTED, user.validate(SshTestUtils.getValidRoles(), logger));
    }
  
}
