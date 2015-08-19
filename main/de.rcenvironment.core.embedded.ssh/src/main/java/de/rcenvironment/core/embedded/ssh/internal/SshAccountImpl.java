/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.List;

import org.apache.commons.logging.Log;

import de.rcenvironment.core.embedded.ssh.api.SshAccount;

/**
 * Default {@link SshAccount} implementation.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class SshAccountImpl implements SshAccount {

    private String username;

    private String password;

    private String publicKey;

    private String role = "";

    // allow to disable/suspend accounts without deleting them
    private boolean enabled = true;

    public SshAccountImpl() {}

    public SshAccountImpl(String username, String password, String publicKey, String role) {
        this.username = username;
        this.password = password;
        this.publicKey = publicKey;
        this.role = role;
    }

    /**
     * Method to validate the SshUser.
     * 
     * @param roles - List of roles
     * @param log the log instance to send validation failures to (as warnings)
     * @return true if valid, else false
     */
    public boolean validate(List<SshAccountRole> roles, Log log) {
        boolean isValid = true;
        boolean noMatchingRole = true;

        // every user has a name (that is not the empty String)
        if (username == null || username.isEmpty()) {
            log.warn("Found a user without username");
            isValid = false;
        }

        // every user has a password or a public key
        if ((password == null || password.isEmpty())
            && (publicKey == null || publicKey.isEmpty())) {
            log.warn("User " + username + " does not have a password or public key");
            isValid = false;
        }

        // ensure role is not null
        if (role == null) {
            log.warn("Changed role for user " + username + " from null to empty string");
            role = "";
        }

        // role of user exist
        for (SshAccountRole curRole : roles) {
            if (role.equals(curRole.getRoleName())) {
                noMatchingRole = false;
                break;
            }
        }
        if (noMatchingRole) {
            log.warn("Could not find role description for role " + role + " configured for user " + username);
            isValid = false;
        }
        return isValid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * {@inheritDoc}
     * 
     * @see de.rcenvironment.core.embedded.ssh.api.SshAccount#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
