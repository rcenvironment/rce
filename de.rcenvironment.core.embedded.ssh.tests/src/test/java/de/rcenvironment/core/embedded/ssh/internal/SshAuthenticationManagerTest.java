/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.security.PublicKey;
import java.util.List;

import org.junit.Test;

import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import junit.framework.TestCase;

/**
 * Test for the class ConsoleAuthenticator.
 * 
 * @author Sebastian Holtappels
 */
public class SshAuthenticationManagerTest extends TestCase {

    private SshConfiguration configuration = null;

    private SshAuthenticationManager authenticationManager = null;

    public SshAuthenticationManagerTest() {
        configuration = SshTestUtils.getValidConfig();
        // null = no session tracker needed; 3 = allowed number of login attempts, set to not interfere with testing
        authenticationManager = new SshAuthenticationManager(configuration, null, 3);
    }

    /**
     * 
     * Test for authenticate method with correct credentials.
     * 
     */
    @Test
    public void testCorrectCredentials() {
        List<SshAccountImpl> users = configuration.listAccounts();
        for (SshAccount user : users) {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                assertTrue("User " + user.getLoginName() + " was not accepted.",
                    authenticationManager.authenticate(user.getLoginName(), user.getPassword(), null));
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
        SshAccount user = configuration.listAccounts().get(0);
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
        SshAccount user = configuration.listAccounts().get(0);
        String password = "kjdfskjdshjbne";
        assertFalse("Authenticator accepted a existing user with incorrect password. (Note: Do not define a test user with the password "
            + password + ")",
            authenticationManager.authenticate(user.getLoginName(), password, null));
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
     * Test for authenticate method with disabled account.
     * 
     */
    @Test
    public void testDisabledAccount() {
        SshAccountImpl user = SshTestUtils.getDisabledUser();
        user.setEnabled(false);
        assertFalse("Authenticator accepted a disabled account.",
            authenticationManager.authenticate(user.getLoginName(), user.getPassword(), null));
    }

    /**
     * 
     * Test authentication of user with no password but public key.
     * 
     */
    @Test
    public void testPwAuthForPublicKeyUser() {
        List<SshAccountImpl> users = configuration.listAccounts();
        for (SshAccount user : users) {
            if (user.getPublicKey() != null && !user.getPublicKey().isEmpty()) {
                assertFalse("Authenticator accepted user that does not have a password.",
                    authenticationManager.authenticate(user.getLoginName(), user.getPassword(), null));
            }
        }
    }

    /**
     * 
     * Test authentication of user with correct public key.
     * 
     */
    @Test
    public void testCorrectCredentialsForPublicKeyUser() {
        List<SshAccountImpl> users = configuration.listAccounts();
        for (SshAccount user : users) {
            if (user.getPublicKey() != null && !user.getPublicKey().isEmpty()) {
                assertTrue("Public key user " + user.getLoginName() + " was not accepted.",
                    authenticationManager.authenticate(user.getLoginName(), user.getPublicKeyObj(), null));
            }
        }
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong user).
     * 
     */
    @Test
    public void testIncorrectUserForPublicKeyUser() {
        SshAccount user = SshTestUtils.getValidPublicKeyUser();
        String userName = "sadkflsas";
        assertFalse("Authenticator accepted a wrong username (with correct password). (Note: Do not define a test user with the name "
            + userName
            + ")",
            authenticationManager.authenticate(userName, user.getPublicKeyObj(), null));
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong key).
     * 
     */
    @Test
    public void testIncorrectKeyForPublicKeyUser() {
        SshAccount user = SshTestUtils.getValidPublicKeyUser();
        PublicKey key = SshTestUtils.createIncorrectPublicKey();
        assertFalse("Authenticator accepted a existing user with incorrect key.",
            authenticationManager.authenticate(user.getLoginName(), key, null));
    }

    /**
     * 
     * Test for authenticate method with incorrect credentials (wrong user and key).
     * 
     */
    @Test
    public void testIncorrectUserAndPwForPublicKeyUser() {
        String userName = "sadkflsas";
        PublicKey key = SshTestUtils.createIncorrectPublicKey();

        assertFalse("Authenticator accepted a wrong username (with worng password). (Note: Do not define a test user with the name "
            + userName + ")",
            authenticationManager.authenticate(userName, key, null));
    }

    /**
     * 
     * Test for authenticate method with disabled account.
     * 
     */
    @Test
    public void testDisabledPublicKeyAccount() {
        SshAccountImpl user = SshTestUtils.getDisabledPublicKeyUser();
        user.setEnabled(false);
        assertFalse("Authenticator accepted a disabled account.",
            authenticationManager.authenticate(user.getLoginName(), user.getPublicKeyObj(), null));
    }

    /**
     * 
     * Test role management with correct values.
     * 
     */
    @Test
    public void testRoleManagement() {
        SshAccount admin = configuration.getAccountByName("admin", false);
        SshAccount user = configuration.getAccountByName("user", false);
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
        SshAccount user = configuration.getAccountByName("user", false);
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
        assertEquals("User (name=" + user.getLoginName() + ") is not allowed to execute command " + command, expected,
            authenticationManager.isAllowedToExecuteConsoleCommand(user.getLoginName(), command));
    }
}
