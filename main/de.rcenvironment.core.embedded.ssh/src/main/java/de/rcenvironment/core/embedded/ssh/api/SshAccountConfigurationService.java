/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.api;

import java.util.SortedMap;

import de.rcenvironment.core.configuration.ConfigurationException;

/**
 * Provides methods for querying and editing SSH/RemoteAccess accounts. Currently used by the text mode configuration UI.
 * 
 * Note that these operations NOT yet safe for concurrent operation (e.g. for calling from the standard UI)!
 * 
 * @author Robert Mischke
 */
public interface SshAccountConfigurationService {

    /**
     * Checks preconditions for safe/consistent configuration editing; for example, the "Remote Access" role must either not exist at all,
     * or be configured as expected.
     * 
     * @return null on success, or a human-readable error message on failure
     */
    String verifyExpectedStateForConfigurationEditing();

    /**
     * @return the current map of all SSH accounts, with account ids as map keys
     * @throws ConfigurationException on internal configuration errors
     */
    SortedMap<String, SshAccount> getAllAccountsByLoginName() throws ConfigurationException;

    /**
     * @param accountId the requeusted account id
     * @return the given account's representation, or null if no such account exists
     */
    SshAccount getAccount(String accountId);

    /**
     * Adds a new SSH account to the active RCE configuration.
     * 
     * @param loginName the new login name
     * @param plainTextPassword the new password (will be hashed before saving)
     * @param setRemoteAccessRole TODO
     * @throws ConfigurationException if the account creation failed
     */
    void createAccount(String loginName, String plainTextPassword, boolean setRemoteAccessRole) throws ConfigurationException;

    /**
     * Updates the password for an existing account.
     * 
     * @param loginName the new login name
     * @param plainTextPassword the new password (will be hashed before saving)
     * @throws ConfigurationException if the operation failed
     */
    void updatePasswordHash(String loginName, String plainTextPassword) throws ConfigurationException;

    /**
     * Updates the role for an existing account.
     * 
     * @param loginName the new login name
     * @param role the new role to set
     * @throws ConfigurationException if the operation failed
     */
    void updateRole(String loginName, String role) throws ConfigurationException;

    /**
     * Enabled or disables the given account.
     * 
     * @param loginName the account's login name
     * @param enabled true to enable, false to disable
     * @throws ConfigurationException if the operation failed
     * */
    void setAccountEnabled(String loginName, boolean enabled) throws ConfigurationException;

    /**
     * Deletes the given account.
     * 
     * @param loginName the account's login name
     * @throws ConfigurationException if the operation failed
     */
    void deleteAccount(String loginName) throws ConfigurationException;

    /**
     * Returns the hash of a password string.
     * 
     * @param password the password
     * @return the hash
     */
    String generatePasswordHash(String password);

}
