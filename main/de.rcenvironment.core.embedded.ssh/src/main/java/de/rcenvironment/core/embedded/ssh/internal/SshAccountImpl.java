/*
 * Copyright (C) 2006-2015 DLR, Germany
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

    private String loginName;

    private String password;

    private String passwordHash;

    private String publicKey;

    private String role = "";

    // allow to disable/suspend accounts without deleting them
    private boolean enabled = true;

    public SshAccountImpl() {} // for JSON serialization

    public SshAccountImpl(String username, String password, String passwordHash, String publicKey, String role) {
        this.loginName = username;
        this.password = password;
        this.passwordHash = passwordHash;
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
        if (loginName == null || loginName.isEmpty()) {
            log.warn("Found a user without username");
            isValid = false;
        }

        // warn on deprecated clear-text passwords, but do not fail the validation
        if (isValid && password != null) {
            log.warn("SSH user \"" + loginName + "\" has an insecure clear-text password. "
                + "Refer to the RCE User Guide on how to change it to a secure format.");
        }

        // every user has a password or a public key
        if ((password == null || password.isEmpty())
            && (passwordHash == null || passwordHash.isEmpty())
            && (publicKey == null || publicKey.isEmpty())) {
            log.warn("User " + loginName + " does not have a password, password hash, or public key");
            isValid = false;
        }

        if (password != null && passwordHash != null) {
            log.warn("User " + loginName + " has both a clear-text and a hashed password at the same time");
            isValid = false;
        }

        // ensure role is not null
        if (role == null) {
            log.warn("Changed role for user " + loginName + " from null to empty string");
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
            log.warn("Could not find role description for role " + role + " configured for user " + loginName);
            isValid = false;
        }
        return isValid;
    }

    @Override
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getPasswordHash() {
        return passwordHash;
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
