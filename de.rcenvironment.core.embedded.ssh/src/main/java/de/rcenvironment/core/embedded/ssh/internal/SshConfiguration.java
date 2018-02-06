/*
 * Copyright (C) 2006-2016 DLR, Germany
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

import de.rcenvironment.core.configuration.ConfigurationException;
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

    protected static final Integer DEFAULT_IDLE_TIMEOUT_SECONDS = 10 * 60; // 10 minutes

    private static final String CONFIG_PROPERTY_IDLE_TIMEOUT_SECONDS = "idleTimeoutSeconds";

    private boolean enabled = false;

    private String host;

    private int port;

    private Integer idleTimeoutSeconds;

    private List<SshAccountImpl> accounts = new ArrayList<>();

    private final List<SshAccountRole> roles;

    public SshConfiguration() {
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
        // Create predefined roles
        roles = createPredefinedRoles();

    }

    public SshConfiguration(ConfigurationSegment configurationSegment) throws ConfigurationException, IOException {

        // Create predefined roles
        roles = createPredefinedRoles();

        enabled = configurationSegment.getBoolean("enabled", false);

        idleTimeoutSeconds = configurationSegment.getInteger(CONFIG_PROPERTY_IDLE_TIMEOUT_SECONDS, DEFAULT_IDLE_TIMEOUT_SECONDS);
        if (idleTimeoutSeconds <= 0) {
            LogFactory.getLog(getClass()).warn("Invalid value for SSH server setting '" + CONFIG_PROPERTY_IDLE_TIMEOUT_SECONDS
                + "' - using default of " + DEFAULT_IDLE_TIMEOUT_SECONDS);
            idleTimeoutSeconds = DEFAULT_IDLE_TIMEOUT_SECONDS;
        }

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
            try {
                SshAccountImpl account = entry.getValue().mapToObject(SshAccountImpl.class);
                account.setLoginName(entry.getKey());
                accounts.add(account);
            } catch (IOException e) {
                throw new ConfigurationException("Error parsing the configuration for account \"" + entry.getKey()
                    + "\". The embedded SSH server will not be started.");
            }
        }

        // Check if deprecated "roles" configuration was used.
        if (configurationSegment.getSubSegment("roles").isPresentInCurrentConfiguration()) {
            LogFactory.getLog(getClass()).warn("Deprecated \"roles\" configuration used. The roles will not be applied. Only "
                + "predefined roles can be used. A list of available roles can be found in the configuration reference or the user guide.");
        }
    }

    private List<SshAccountRole> createPredefinedRoles() {
        List<SshAccountRole> predefRoles = new ArrayList<SshAccountRole>();
        for (String roleName : SshConstants.PREDEFINED_ROLE_NAMES) {
            predefRoles.add(new SshAccountRole(roleName));
        }
        return predefRoles;
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
        // List<SshAccountRole> valRoles = new ArrayList<SshAccountRole>();
        List<SshAccountImpl> valUsers = new ArrayList<SshAccountImpl>();

        if (host == null || host.isEmpty()) {
            logger.warn("SSH server host can not be empty");
        }

        // sshContactPoint valid
        if (port < SshConstants.MIN_PORT_NUMBER || port > SshConstants.MAX_PORT_NUMBER) {
            logger.warn("sshContactPoint must be between " + SshConstants.MIN_PORT_NUMBER + " and "
                + SshConstants.MAX_PORT_NUMBER);
            isValid = false;
        }

        /*
         * Validation of roles currently not needed, only predefined roles are used. // roles not null if (roles != null &&
         * !roles.isEmpty()) { for (SshAccountRole role : roles) { // validate single role isValid = role.validateRole(logger) && isValid;
         * 
         * // distinct role names if (valRoles.contains(role)) { logger.warn("Role names must be distinct. found two roles with name: " +
         * role.getRoleName()); isValid = false; } else { valRoles.add(role); } } } else { if (roles == null) { roles = new
         * ArrayList<SshAccountRole>(); } List<String> defaultPrivileges = new ArrayList<String>();
         * defaultPrivileges.add(SshConstants.DEFAULT_ROLE_PRIVILEGES);
         * logger.warn("Warning: Configuration did not include roles. Creating default role with all privileges"); roles.add(new
         * SshAccountRole("", new ArrayList<String>())); }
         */

        // users not null
        if (accounts != null && !accounts.isEmpty()) {
            for (SshAccountImpl user : accounts) {
                // validate single user
                isValid = user.validate(roles, logger) && isValid;

                // distinct user names (no empty string values)
                if (valUsers.contains(user)) {
                    logger.warn("User names must be distinct. Found two users with name: " + user.getLoginName());
                    isValid = false;
                } else {
                    valUsers.add(user);
                }
            }
        } else {
            logger.warn("Configuration did not include user definitions. At least one user must be defined.");
            isValid = false;
        }
        if (!isValid) {
            logger.error("Embedded SSH server will not be started due to an error in the configuration.");
        }
        return isValid;
    }

    // Special Getter and Setter - START

    /**
     * 
     * Get a user for a user name.
     * 
     * @param loginName - the name of the user
     * @param allowDisabled true if disabled accounts should be returned as well; if false, null is returned for disabled accounts
     * @return - the account object for the given name, if matched
     */
    public SshAccount getAccountByName(String loginName, boolean allowDisabled) {
        SshAccount curUser = null;
        for (SshAccount user : accounts) {
            if (user.getLoginName().equals(loginName) && (allowDisabled || user.isEnabled())) {
                if (curUser != null) {
                    LogFactory.getLog(getClass()).error(
                        "Invalid state: more than one SSH account matched for login name '" + loginName + "'! Returning 'null' for safety");
                    return null;
                }
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
        if (curRole == null) {
            curRole = getRoleByName(SshConstants.ROLE_NAME_DEFAULT);
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

    public Integer getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
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

    // Getter and Setter - END
}
