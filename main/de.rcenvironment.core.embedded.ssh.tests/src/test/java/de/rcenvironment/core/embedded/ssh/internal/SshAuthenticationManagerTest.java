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

import org.junit.Test;

import de.rcenvironment.core.embedded.ssh.api.SshAccount;

/**
 * Test for the class ConsoleAuthenticator.
 * 
 * @author Sebastian Holtappels
 */
public class SshAuthenticationManagerTest extends TestCase {

    private SshConfiguration configuration = null;

    private SshAuthenticationManager authenticationManager = null;

    // TODO add test for "enabled" flag - misc_ro

    public SshAuthenticationManagerTest() {
        configuration = SshTestUtils.getValidConfig();
        authenticationManager = new SshAuthenticationManager(configuration);
    }

    /**
     * 
     * Test for authenticate method with correct credentials.
     * 
     */
    @Test
    public void testCorrectCredentials() {
        List<SshAccountImpl> users = configuration.getAccounts();
        for (SshAccount user : users) {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                assertTrue("User " + user.getUsername() + " was not accepted.",
                    authenticationManager.authenticate(user.getUsername(), user.getPassword(), null));
            }
        }
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong user).
     * 
     */
    @Test
    public void testIncorrectUser() {
        SshAccount user = configuration.getAccounts().get(0);
        String userName = "bnmslejnds";
        assertFalse("Authenticator accepted a wrong username (with correct password). (Note: Do not define a test user with the name "
            + userName
            + ")",
            authenticationManager.authenticate(userName, user.getPassword(), null));
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong password).
     * 
     */
    @Test
    public void testIncorrectPassword() {
        SshAccount user = configuration.getAccounts().get(0);
        String password = "kjdfskjdshjbne";
        assertFalse("Authenticator accepted a existing user with incorrect password. (Note: Do not define a test user with the password "
            + password + ")",
            authenticationManager.authenticate(user.getUsername(), password, null));
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong user and password).
     * 
     */
    @Test
    public void testIncorrectUserAndPw() {
        String userName = "bnmslejnds";
        String password = "password";
        assertFalse("Authenticator accepted a wrong username (with worng password). (Note: Do not define a test user with the name "
            + userName + ")",
            authenticationManager.authenticate(userName, password, null));
    }

    /**
     * 
     * Test authentication of user with no password but public key.
     * 
     */
    @Test
    public void testPwAuthForPublicKeyUser() {
        List<SshAccountImpl> users = configuration.getAccounts();
        for (SshAccount user : users) {
            if (user.getPublicKey() != null && !user.getPublicKey().isEmpty()) {
                assertFalse("Authenticator accepted user that does not have a password.",
                    authenticationManager.authenticate(user.getUsername(), user.getPassword(), null));
            }
        }
    }

    /**
     * 
     * Test role management with correct values.
     * 
     */
    @Test
    public void testRoleManagement() {
        SshAccount admin = configuration.getAccountByName("admin");
        SshAccount user = configuration.getAccountByName("user");
        assertNotNull("No user with name admin foud (should have a role, which allowes the execution of all commands)", admin);
        assertNotNull("No user with name user foud (should have a role, which allowes the execution of the command: stats, task, net)",
            user);
        testRoleManagement(admin, "osgi", true);
        testRoleManagement(admin, "tasks", true);
        testRoleManagement(admin, "net", true);
        testRoleManagement(admin, "net all", true);
        testRoleManagement(user, "stats", true);
        testRoleManagement(user, "tasks", true);
        testRoleManagement(user, "net", true);
        testRoleManagement(user, "net all", true);
    }

    // - Test if user is blocked, if he has to

    /**
     * 
     * Test role management with forbidden values.
     * 
     */
    @Test
    public void testRoMaForbidden() {
        SshAccount user = configuration.getAccountByName("user");
        assertNotNull("No user with name user foud (should have a role, which allowes the execution of the command: stats, task, net)",
            user);
        testRoleManagement(user, "wf", false);
        testRoleManagement(user, "wf run test.wf", false);
        testRoleManagement(user, "osgi", false);
        testRoleManagement(user, "falseCommand", false);
    }

    // - Test incorrect user name value

    /**
     * 
     * Test role management with non existing user.
     * 
     */
    @Test
    public void testRoMaWrongUser() {
        assertFalse("unknown user is allowed to execute command (that's bad; Do not define a test user with the name sdhjdfsh5412546)",
            authenticationManager.isAllowedToExecuteConsoleCommand("sdhjdfsh5412546", "stats"));
    }

    private void testRoleManagement(SshAccount user, String command, boolean expected) {
        assertEquals("User (name=" + user.getUsername() + ") is not allowed to execut COMMAND", expected,
            authenticationManager.isAllowedToExecuteConsoleCommand(user.getUsername(), command));
    }
}
