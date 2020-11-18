/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
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
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccount;
import de.rcenvironment.core.embedded.ssh.api.TemporarySshAccountControl;
import de.rcenvironment.core.utils.common.AuditLog;
import de.rcenvironment.core.utils.common.AuditLog.LogEntry;
import de.rcenvironment.core.utils.common.AuditLogIds;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * A simple Password Authenticator to be replaced by a public/private key infrastructure.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class SshAuthenticationManager implements PasswordAuthenticator, TemporarySshAccountControl, PublickeyAuthenticator {

    private static final String REFUSAL_REASON_NO_SUCH_USER = "account not found or disabled";

    private static final String REFUSAL_REASON_AUTH_FAILURE = "auth failure";

    private static final String REFUSAL_REASON_UNDEFINED = "<undefined>";

    private static final String EVENT_LOG_KEY_CONNECTION_TYPE = "type";

    private static final String EVENT_LOG_KEY_REFUSAL_REASON = "reason";

    private static final String EVENT_LOG_VALUE_CONNECTION_TYPE = "ssh/uplink";

    private SshConfiguration configuration;

    private List<TemporarySshAccount> temporaryAccounts;

    private final Log log = LogFactory.getLog(getClass());

    public SshAuthenticationManager(SshConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    // implementation of MINA PasswordAuthenticator
    public boolean authenticate(String loginName, String passwordParam, ServerSession session) {
        boolean success = false;
        String refusalReason = REFUSAL_REASON_UNDEFINED;

        if (passwordParam == null || passwordParam.isEmpty()) {
            refusalReason = "empty password";
        } else if (loginName == null || loginName.isEmpty()) {
            refusalReason = "empty login name";
        } else if (loginName.startsWith(SshConstants.TEMP_USER_PREFIX)) {
            // currently disabled
            refusalReason = "unsupported temp user";
        } else {
            // normal login check
            SshAccount account = configuration.getAccountByName(loginName, false);
            if (account != null) {
                if (checkPassword(account, passwordParam)) {
                    success = true;
                } else {
                    refusalReason = REFUSAL_REASON_AUTH_FAILURE;
                }
            } else {
                // no matching account found
                refusalReason = REFUSAL_REASON_NO_SUCH_USER;
            }
        }

        writeAuditLogEntry(session, loginName, "password", success, refusalReason);

        return success;
    }

    /*
     * For public key authentication. Does not perform the actual authentication, but just checks if the given public key is allowed to
     * authenticate.
     *
     */
    @Override
    public boolean authenticate(String loginName, PublicKey key, ServerSession session) {
        boolean success = false;
        String refusalReason = REFUSAL_REASON_UNDEFINED;

        SshAccount account = configuration.getAccountByName(loginName, false);
        if (account != null) {
            PublicKey knownKey = account.getPublicKeyObj();
            if (knownKey != null) {
                success = key.equals(knownKey);
            }
            if (!success) {
                refusalReason = REFUSAL_REASON_AUTH_FAILURE;
            }
        } else {
            // no matching account found
            refusalReason = REFUSAL_REASON_NO_SUCH_USER;
        }

        writeAuditLogEntry(session, loginName, "publickey", success, refusalReason);

        return success;
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
            // Should never happen as the allowed command patterns are checked when the SSH server is started
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
     * Used to determine if the user is allowed to access a command shell or run any command.
     * 
     * @param username - The name of the user
     * @return true if allowed
     */
    public boolean isAllowedToOpenShell(String username) {
        // TODO check for potential NPE
        return getRoleForUser(username).isAllowedToOpenShell();
    }

    /**
     * Used to determine if the user is allowed to use uplink connections.
     * 
     * @param username - The name of the user
     * @return true if allowed
     */
    public boolean isAllowedToUseUplink(String username) {
        return getRoleForUser(username).isAllowedToUseUplink();
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
    public SortedMap<String, SshAccount> getStaticAccountsByLoginName() {
        SortedMap<String, SshAccount> result = new TreeMap<>();
        for (SshAccountImpl a : configuration.getStaticAccounts()) {
            result.put(a.getLoginName(), a);
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

    private void writeAuditLogEntry(ServerSession session, String usernameParam, String authMethod, boolean success, String refusalReason) {
        if (session == null) {
            if (!RuntimeDetection.isTestEnvironment()) {
                throw new IllegalStateException("SSH session is null while apparently not in a test context");
            }
            return;
        }
        String sshSessionLogId = Integer.toString(System.identityHashCode(session));
        InetSocketAddress remoteAddressAndPort = (InetSocketAddress) session.getRemoteAddress();
        final LogEntry auditLogEntry;
        if (success) {
            auditLogEntry = AuditLog.newEntry(AuditLogIds.CONNECTION_INCOMING_ACCEPT);
        } else {
            auditLogEntry = AuditLog.newEntry(AuditLogIds.CONNECTION_INCOMING_REFUSE);
        }
        auditLogEntry
            .set(EVENT_LOG_KEY_CONNECTION_TYPE, EVENT_LOG_VALUE_CONNECTION_TYPE)
            .set("login_name", usernameParam)
            .set("auth_method", authMethod)
            .set("remote_ip", remoteAddressAndPort.getAddress().getHostAddress())
            .set("remote_port", remoteAddressAndPort.getPort())
            .set("server_port", configuration.getPort())
            .set("ssh_session_id", sshSessionLogId); // for association
        if (!success) {
            auditLogEntry.set(EVENT_LOG_KEY_REFUSAL_REASON, refusalReason);
        }
        AuditLog.append(auditLogEntry);
    }

}
