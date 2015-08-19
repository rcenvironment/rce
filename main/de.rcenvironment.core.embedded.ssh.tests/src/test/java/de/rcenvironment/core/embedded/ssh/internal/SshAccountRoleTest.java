/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * 
 * Test for the class ConsoleRole.
 * 
 * @author Sebastian Holtappels
 */
public class SshAccountRoleTest extends TestCase {

    private Log logger = LogFactory.getLog(SshAccountRoleTest.class);

    /** Test. */
    @Test
    public void testAllowedCommandRegex() {
        List<String> allowedCommands = new ArrayList<String>();
        allowedCommands.add("first");
        allowedCommands.add("second");
        SshAccountRole role = new SshAccountRole("test", allowedCommands, new ArrayList<String>());
        String commandRegEx = role.getAllowedCommandRegEx();
        assertTrue("Regular exprssion for allowed command is invalid.", "first".matches(commandRegEx));
        assertTrue("Regular exprssion for allowed command is invalid!", "second".matches(commandRegEx));
        assertTrue("Regular exprssion for allowed command is invalid", "exit".matches(commandRegEx));
        assertTrue("Regular exprssion for allowed command is invalid.", "help".matches(commandRegEx));
        assertFalse("Regular exprssion for allowed command is invalid!", "helpsecond".matches(commandRegEx));
        assertFalse("Regular exprssion for allowed command is invalid", "firstexit".matches(commandRegEx));
        assertFalse("Regular exprssion for allowed command is invalid ", "hjdshbd".matches(commandRegEx));
    }

    /** Test. */
    @Test
    public void testEqualsPositive() {
        SshAccountRole role1 = new SshAccountRole("role1", new ArrayList<String>(), new ArrayList<String>());
        SshAccountRole role2 = new SshAccountRole("role1", new ArrayList<String>(), new ArrayList<String>());
        assertTrue("ConsoleRole.equals returned false but true was expected", role1.equals(role2));
    }

    /** Test. */
    @Test
    public void testEqualsNegative() {
        SshAccountRole role3 = new SshAccountRole("role3", new ArrayList<String>(), new ArrayList<String>());
        SshAccountRole role4 = new SshAccountRole("role4", new ArrayList<String>(), new ArrayList<String>());
        assertFalse("ConsoleRole.equals returned true but false was expected", role3.equals(role4));
    }

    /** Test. */
    @Test
    public void testValidateRolePositive() {
        List<SshAccountRole> roles = SshTestUtils.getValidRoles();
        for (SshAccountRole role : roles) {
            assertTrue("ConsoleRole.validateRole returned false but true was expected", role.validateRole(logger));
        }
    }

    /** Test. */
    @Test
    public void testValidateRoleNegative() {
        SshAccountRole role = SshTestUtils.getValidRole();
        // role name is null
        role.setRoleName(null);
        assertFalse("ConsoleRole.validateRole returned true but false was expected ", role.validateRole(logger));

        // allowedCommandPatterns is null
        role = SshTestUtils.getValidRole();
        role.setAllowedCommandPatterns(null);
        assertFalse("ConsoleRole.validateRole returned true but false was expected ", role.validateRole(logger));

        // allowedCommandPatterns is empty
        role.setAllowedCommandPatterns(new ArrayList<String>());
        assertFalse("ConsoleRole.validateRole returned true but false was expected", role.validateRole(logger));

        // allowedScpPath contains an element with null
        role = SshTestUtils.getValidRole();
        List<String> invalidPaths = role.getAllowedScpPath();
        invalidPaths.add(null);
        role.setAllowedScpPath(invalidPaths);
        assertFalse("ConsoleRole.validateRole() returned true but false was expected", role.validateRole(logger));

        // allowedScpPath contains an empty string
        role = SshTestUtils.getValidRole();
        invalidPaths = role.getAllowedScpPath();
        invalidPaths.add("");
        role.setAllowedScpPath(invalidPaths);
        assertFalse("ConsoleRole.validateRole() returned true but false was expected ", role.validateRole(logger));
    }
}
