/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccountControl;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * A simple Password Authenticator to be replaced by a public/private key infrastructure.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class SshAuthenticationManager implements PasswordAuthenticator, TemporarySshAccountControl {

    private SshConfiguration configuration;

    private List<TemporarySshAccount> temporaryAccounts;

    private final Log log = LogFactory.getLog(getClass());

    public SshAuthenticationManager(SshConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    // implementation of MINA PasswordAuthenticator
    public boolean authenticate(String usernameParam, String passwordParam, ServerSession session) {
        boolean loginCorrect = false;
        if (usernameParam != null && !usernameParam.isEmpty() && passwordParam != null && !passwordParam.isEmpty()) {
            if (usernameParam.startsWith(SshConstants.TEMP_USER_PREFIX)) {
                TemporarySshAccount tempUser = getTemporaryAccountByName(usernameParam);
                if (tempUser != null && tempUser.getPassword().equals(passwordParam)) {
                    loginCorrect = true;
                }
            } else {
                SshAccount user = configuration.getAccountByName(usernameParam);
                if (user != null && user.getPassword().equals(passwordParam)) {
                    loginCorrect = true;
                }
            }
        }
        return loginCorrect;
    }

    /**
     * 
     * Used to determine if a user has the rights to execute a command.
     * 
     * @param username the user who wants to execute the command
     * @param command the command to be executed
     * @return true (is allowed) false (not allowed)
     */
    public boolean isAllowedToExecuteConsoleCommand(String username, String command) {
        boolean isAllowed = false;
        SshAccountRole userRole = getRoleForUser(username);
        if (userRole != null && command.matches(userRole.getAllowedCommandRegEx())) {
            isAllowed = true;
        }
        return isAllowed;
    }

    /**
     * 
     * Used to determine if a user has the rights to use scp to copy files to the given destinations.
     * 
     * @param username - The name of the active user
     * @param destination - the destinations of the scp command
     * @return - true if user is allowed
     */
    public boolean isAllowedToUseScpDestination(String username, String destination) {
        boolean isAllowed = false;
        TemporarySshAccount tempUser = getTemporaryAccountByName(username);
        if (tempUser != null) {
            isAllowed = destination != null && destination.startsWith(tempUser.getVirtualScpRootPath()) && !destination.contains("../")
                && !destination.contains("..\\");
        }
        return isAllowed;
    }

    /**
     * @param username the account name to check
     * @return if this account name matches an existing temporary account
     */
    @Deprecated
    public boolean isTemporaryAccountName(String username) {
        return getTemporaryAccountByName(username) != null;
    }

    private SshAccountRole getRoleForUser(String userName) {
        SshAccount user = configuration.getAccountByName(userName);
        SshAccountRole role = null;
        if (user != null) {
            role = configuration.getRoleByName(user.getRole());
        }
        return role;
    }

    /**
     * Returns the {@link SshUser} object matching the given username, or throws an {@link AuthenticationException} if no such user exists.
     * 
     * @param userName the login name of the account to fetch
     * @return the {@link SshAccount} object matching the given username
     * @throws AuthenticationException if no matching account was found
     */
    public SshAccount getAccountByUsername(String userName) throws AuthenticationException {
        SshAccount user = configuration.getAccountByName(userName);
        if (user != null) {
            return user;
        } else {
            throw new AuthenticationException(String.format("No SSH account for username \"%s\"", userName));
        }
    }

    /**
     * 
     * Return the user with the given name.
     * 
     * @param name - The name of the user
     * @return - the user
     */
    @Deprecated
    public TemporarySshAccount getTemporaryAccountByName(String name) {
        TemporarySshAccount result = null;
        if (temporaryAccounts != null) {
            for (TemporarySshAccount tempUser : temporaryAccounts) {
                if (tempUser.getUsername().equals(name)) {
                    result = tempUser;
                }
            }
        }
        return result;
    }

    @Deprecated
    public List<TemporarySshAccount> getTemporaryAccounts() {
        return temporaryAccounts;
    }

    @Deprecated
    public void setTemporaryAccounts(List<TemporarySshAccount> tempUsers) {
        this.temporaryAccounts = tempUsers;
    }

    // TODO add synchronization - misc_ro
    @Override
    @Deprecated
    public TemporarySshAccount createTemporarySshAccount() {
        if (temporaryAccounts == null) {
            temporaryAccounts = new CopyOnWriteArrayList<TemporarySshAccount>();
        }
        TemporarySshAccountImpl tempAccount = new TemporarySshAccountImpl();
        // create name
        String randomAccountNamePart = RandomStringUtils.randomAlphanumeric(SshConstants.TEMP_USER_NAME_RANDOM_LENGTH);
        String username = SshConstants.TEMP_USER_PREFIX + randomAccountNamePart;
        tempAccount.setUsername(username);
        // create password
        tempAccount.setPassword(RandomStringUtils.randomAlphanumeric(SshConstants.TEMP_USER_PASSWORD_RANDOM_LENGTH));
        // set Path
        tempAccount.setVirtualScpRootPath("/temp/" + username);
        try {
            tempAccount.setLocalScpRootPath(TempFileServiceAccess.getInstance().createManagedTempDir("temp-scp-" + randomAccountNamePart));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary SCP directory", e);
        }
        log.debug(String.format("Created temporary SCP account '%s' with virtual SCP path '%s' mapped to local path '%s'",
            tempAccount.getUsername(), tempAccount.getVirtualScpRootPath(), tempAccount.getLocalScpRootPath().getAbsolutePath()));

        temporaryAccounts.add(tempAccount);
        return tempAccount;
    }

    @Override
    @Deprecated
    public void discardTemporarySshAccount(String name) {
        for (int i = 0; i < temporaryAccounts.size(); i++) {
            TemporarySshAccount tempUser = temporaryAccounts.get(i);
            if (tempUser.getUsername().equals(name)) {
                temporaryAccounts.remove(i);
            }
        }
    }

}
