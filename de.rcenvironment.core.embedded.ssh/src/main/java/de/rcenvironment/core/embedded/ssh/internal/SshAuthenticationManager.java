/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.mindrot.jbcrypt.BCrypt;

import de.rcenvironment.core.authentication.AuthenticationException;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccountControl;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * A simple Password Authenticator to be replaced by a public/private key infrastructure.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden (added public key authentication)
 */
public class SshAuthenticationManager implements PasswordAuthenticator, TemporarySshAccountControl, PublickeyAuthenticator {

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
                if (tempUser != null && checkPassword(tempUser, passwordParam)) {
                    loginCorrect = true;
                }
            } else {
                SshAccount user = configuration.getAccountByName(usernameParam, false);
                if (user != null && checkPassword(user, passwordParam)) {
                    loginCorrect = true;
                }
            }
        }
        return loginCorrect;
    }

    /*
     * For public key authentication. Does not perform the actual authentication, but just checks if the given public key is allowed to
     * authenticate.
     *
     */
    @Override
    public boolean authenticate(String userName, PublicKey key, ServerSession session) {
        boolean loginCorrect = false;
        //Check if account with this username exists
        if (configuration.getAccountByName(userName, false) == null) {
            return false;
        }
        PublicKey knownKey = configuration.getAccountByName(userName, false).getPublicKeyObj();
        if (knownKey != null) {
            loginCorrect = key.equals(knownKey);
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
        try {
            if (userRole != null) {
                final boolean commandIsOnWhitelist = command.matches(userRole.getAllowedCommandRegEx());
                final boolean commandIsOnBlacklist = command.matches(userRole.getDisallowedCommandRegEx());
                if (commandIsOnWhitelist && !commandIsOnBlacklist) {
                    isAllowed = true;
                }
            }
        } catch (PatternSyntaxException e) {
            //Should never happen as the allowed command patterns are checked when the SSH server is started
            log.error("Could not verify if user " + username + " is allowed to execute command " + command
                + ". Probable cause: The allowed commands pattern is invalid.");
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
        SshAccount user = configuration.getAccountByName(userName, true);
        SshAccountRole role = null;
        if (user != null) {
            role = configuration.getRoleByName(user.getRole());
        }
        return role;
    }

    /**
     * Returns the {@link SshUser} object matching the given login name, or throws an {@link AuthenticationException} if no such user
     * exists.
     * 
     * @param loginName the login name of the account to fetch
     * @param allowDisabled true if disabled accounts should be returned as well; if false, null is returned for disabled accounts
     * @return the {@link SshAccount} object matching the given login name, or null if no such account exists
     */
    public SshAccount getAccountByLoginName(String loginName, boolean allowDisabled) {
        // TODO nothing prevents "weird" SSH names to be defined via the configuration file at this time (e.g. starting with space)
        return configuration.getAccountByName(loginName, allowDisabled);
    }

    /**
     * @return all accounts in a sorted map, with their login names as map key
     */
    public SortedMap<String, SshAccount> getAllAcountsByLoginName() {
        SortedMap<String, SshAccount> result = new TreeMap<>();
        for (SshAccountImpl account : configuration.getAccounts()) {
            result.put(account.getLoginName(), account);
        }
        return result;
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
                if (tempUser.getLoginName().equals(name)) {
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
        tempAccount.setLoginName(username);
        // create password
        tempAccount.setPassword(RandomStringUtils.randomAlphanumeric(SshConstants.TEMP_USER_PASSWORD_RANDOM_LENGTH));
        // set Path
        tempAccount.setVirtualScpRootPath("/temp/" + username);
        try {
            tempAccount.setLocalScpRootPath(TempFileServiceAccess.getInstance().createManagedTempDir("temp-scp-" + randomAccountNamePart));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary SCP directory", e);
        }
        log.debug(StringUtils.format("Created temporary SCP account '%s' with virtual SCP path '%s' mapped to local path '%s'",
            tempAccount.getLoginName(), tempAccount.getVirtualScpRootPath(), tempAccount.getLocalScpRootPath().getAbsolutePath()));

        temporaryAccounts.add(tempAccount);
        return tempAccount;
    }

    @Override
    @Deprecated
    public void discardTemporarySshAccount(String name) {
        for (int i = 0; i < temporaryAccounts.size(); i++) {
            TemporarySshAccount tempUser = temporaryAccounts.get(i);
            if (tempUser.getLoginName().equals(name)) {
                temporaryAccounts.remove(i);
            }
        }
    }

    private boolean checkPassword(SshAccount account, String password) {
        if (account.getPasswordHash() != null) {
            // TODO move to common utility function
            final boolean result = BCrypt.checkpw(password, account.getPasswordHash());
            log.debug(StringUtils.format("Used password hash to check login attempt for user \"%s\" - accepted = %s",
                account.getLoginName(), result));
            return result;
        }
        if (account.getPassword() != null) {
            final boolean result = account.getPassword().equals(password);
            log.warn(StringUtils.format("Used clear-text password to check login attempt for user \"%s\" - accepted = %s",
                account.getLoginName(), result));
            return result;
        }
        log.error("Consistency error: SSH login attempt with a password for user \"" + account.getLoginName()
            + "\", but the local account has neither a clear-text nor a hashed password");
        return false;
    }

}
