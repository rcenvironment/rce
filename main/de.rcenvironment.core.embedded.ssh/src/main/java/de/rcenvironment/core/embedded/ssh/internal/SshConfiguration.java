/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;

/**
 * Configuration class for the ssh console.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public class SshConfiguration {

    protected static final int DEFAULT_PORT = 31005;

    protected static final String DEFAULT_HOST = "127.0.0.1"; // conservative default: bind to localhost, not 0.0.0.0

    private boolean enabled = false;

    private String host;

    private int port;

    private List<SshAccountImpl> accounts = new ArrayList<>();

    private List<SshAccountRole> roles = new ArrayList<>();

    public SshConfiguration() {
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
    }

    public SshConfiguration(ConfigurationSegment configurationSegment) throws IOException {

        enabled = configurationSegment.getBoolean("enabled", false);

        String oldHostSetting = configurationSegment.getString("host"); // deprecated alias
        String newHostSetting = configurationSegment.getString("ip");
        if (oldHostSetting != null) {
            LogFactory.getLog(getClass()).warn("Deprecated SSH server configuration parameter \"host\" used - use \"ip\" instead");
            host = oldHostSetting;
        }
        if (newHostSetting != null) {
            if (oldHostSetting != null) {
                LogFactory.getLog(getClass()).error(
                    "Both \"host\" and \"ip\" settings of SSH server used; ignoring deprecated \"host\" setting");
            }
            host = newHostSetting;
        }
        if (host == null) {
            host = DEFAULT_HOST;
        }
        port = configurationSegment.getInteger("port", DEFAULT_PORT);

        for (Entry<String, ConfigurationSegment> entry : configurationSegment.listElements("accounts").entrySet()) {
            SshAccountImpl account = entry.getValue().mapToObject(SshAccountImpl.class);
            account.setUsername(entry.getKey());
            accounts.add(account);
        }

        for (Entry<String, ConfigurationSegment> entry : configurationSegment.listElements("roles").entrySet()) {
            SshAccountRole role = entry.getValue().mapToObject(SshAccountRole.class);
            role.setRoleName(entry.getKey());
            roles.add(role);
        }
    }

    /**
     * Validates the Configuration-Object. To ensure the server is able to start and there are no security issues like users without a
     * password or without a role (if there is no default role).
     * 
     * @param logger - the logger to be used to log
     * 
     * @return true if valid else false
     */
    public boolean validateConfiguration(Log logger) {
        boolean isValid = true;
        List<SshAccountRole> valRoles = new ArrayList<SshAccountRole>();
        List<SshAccountImpl> valUsers = new ArrayList<SshAccountImpl>();

        if (host == null || host.isEmpty()) {
            logger.info("SSH server host can not be empty");
        }

        // sshContactPoint valid
        if (port < SshConstants.MIN_PORT_NUMBER || port > SshConstants.MAX_PORT_NUMBER) {
            logger.info("sshContactPoint must be between " + SshConstants.MIN_PORT_NUMBER + " and "
                + SshConstants.MAX_PORT_NUMBER);
            isValid = false;
        }

        // roles not null
        if (roles != null && !roles.isEmpty()) {
            for (SshAccountRole role : roles) {
                // validate single role
                isValid = role.validateRole(logger) && isValid;

                // distinct role names
                if (valRoles.contains(role)) {
                    logger.info("Role names must be distinct. found two roles with name: " + role.getRoleName());
                    isValid = false;
                } else {
                    valRoles.add(role);
                }
            }
        } else {
            if (roles == null) {
                roles = new ArrayList<SshAccountRole>();
            }
            List<String> defaultPrivileges = new ArrayList<String>();
            defaultPrivileges.add(SshConstants.DEFAULT_ROLE_PRIVILEGES);
            logger.info("Warning: Configuration did not include roles. Creating default role with all privileges");
            roles.add(new SshAccountRole("", defaultPrivileges, new ArrayList<String>()));
        }

        // users not null
        if (accounts != null && !accounts.isEmpty()) {
            for (SshAccountImpl user : accounts) {
                // validate single user
                isValid = user.validate(roles, logger) && isValid;

                // distinct user names (no empty string values)
                if (valUsers.contains(user)) {
                    logger.info("User names must be distinct. Found two users with name: " + user.getUsername());
                    isValid = false;
                } else {
                    valUsers.add(user);
                }
            }
        } else {
            logger.info("Configuration did not include user definitions. At least one user must be defined.");
            isValid = false;
        }
        return isValid;
    }

    // Special Getter and Setter - START

    /**
     * 
     * Get a user for a user name.
     * 
     * @param userName - the name of the user
     * @return - the user for the given name
     */
    public SshAccount getAccountByName(String userName) {
        SshAccount curUser = null;
        for (SshAccount user : accounts) {
            if (user.isEnabled() && user.getUsername().equals(userName)) {
                curUser = user;
                break;
            }
        }
        return curUser;
    }

    /**
     * 
     * Get a role for a role name.
     * 
     * @param roleName - the name of the role
     * @return - the role for the given name
     */
    public SshAccountRole getRoleByName(String roleName) {
        SshAccountRole curRole = null;
        for (SshAccountRole role : roles) {
            if (role.getRoleName().equals(roleName)) {
                curRole = role;
                break;
            }
        }
        return curRole;
    }

    // Special Getter - END
    // Getter and Setter - START

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean startSshConsole) {
        this.enabled = startSshConsole;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int sshContactPoint) {
        this.port = sshContactPoint;
    }

    public List<SshAccountImpl> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<SshAccountImpl> users) {
        this.accounts = users;
    }

    public List<SshAccountRole> getRoles() {
        return roles;
    }

    public void setRoles(List<SshAccountRole> roles) {
        this.roles = roles;
    }

    // Getter and Setter - END
}
