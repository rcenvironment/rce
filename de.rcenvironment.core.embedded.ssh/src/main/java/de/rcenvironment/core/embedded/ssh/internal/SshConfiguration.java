/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;

/**
 * Configuration class for the ssh console.
 * 
 * Synchronization policy: All account-related operations are synchronized (as they are mutable); all others operations are not.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 * @author Brigitte Boden
 */
public class SshConfiguration {

    protected static final int DEFAULT_PORT = 31005;

    protected static final String DEFAULT_HOST = "127.0.0.1"; // conservative default: bind to localhost, not 0.0.0.0

    // Was 10 Minutes until 10.2.0, then changed to 1 minute to prevent SSH sessions lingering for that time with some clients, even
    // after they have actively closed their connection. A special solution to keep interactive shells open for longer may be added in a
    // future release. -- misc_ro
    protected static final Integer DEFAULT_IDLE_TIMEOUT_SECONDS = 60;

    private static final String CONFIG_PROPERTY_IDLE_TIMEOUT_SECONDS = "idleTimeoutSeconds";

    // for user messages
    private static final String STATIC_ACCOUNT_DATA_SOURCE_INFO = "the main configuration file";

    private static final String DYNAMIC_ACCOUNTS_FILENAME = "accounts.json";

    private boolean enabled = false;

    private String host;

    private int port;

    private Integer idleTimeoutSeconds;

    private List<SshAccountImpl> staticAccounts = new ArrayList<>();

    private List<SshAccountImpl> dynamicAccounts = new ArrayList<>();

    private Map<String, SshAccountImpl> currentAccountMap = new HashMap<>();

    private final List<SshAccountRole> roles;

    private final Log log = LogFactory.getLog(getClass());

    private String accountDataOriginInfo;

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

        Map<String, ConfigurationSegment> staticAccountData = configurationSegment.listElements("accounts");
        try {
            staticAccounts = parseAccountData(staticAccountData, STATIC_ACCOUNT_DATA_SOURCE_INFO);
        } catch (ConfigurationException e) {
            log.warn("Failed to parse the list of SSH accounts; the SSH server will remain disabled");
            staticAccounts = new ArrayList<>();
        }

        // placeholder settings until updated with actual dynamic data
        dynamicAccounts = new ArrayList<>();
        accountDataOriginInfo = "static"; // includes case of no accounts at all

        updateEffectiveAccountLookup();

        // Check if deprecated "roles" configuration was used.
        if (configurationSegment.getSubSegment("roles").isPresentInCurrentConfiguration()) {
            log.warn("Deprecated \"roles\" configuration used. The roles will not be applied. Only "
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
    public synchronized boolean validateConfiguration(Log logger) {
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
        if (staticAccounts != null && !staticAccounts.isEmpty()) {
            for (SshAccountImpl user : staticAccounts) {
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
        }
        if (!isValid) {
            logger.error("Embedded SSH server will not be started due to an error in the configuration.");
        }
        return isValid;
    }

    public synchronized void applyDynamicSshAccountData(Map<String, ConfigurationSegment> sshAccounts) {

        try {
            dynamicAccounts = parseAccountData(sshAccounts, DYNAMIC_ACCOUNTS_FILENAME);
            updateEffectiveAccountLookup();
        } catch (ConfigurationException e1) {
            log.warn("Failed to read the dynamic list of SSH accounts from " + DYNAMIC_ACCOUNTS_FILENAME + "; no changes applied");
            return;
        }
    }

    /**
     * Test usage only!
     * 
     * Note: this account will not "survive" an updated of the effective account list.
     * 
     * @param account the account to add/inject
     */
    public synchronized void injectAccount(SshAccountImpl account) {
        currentAccountMap.put(account.getLoginName(), account);
    }

    /**
     * 
     * Get a user for a user name.
     * 
     * @param loginName - the name of the user
     * @param allowDisabled true if disabled accounts should be returned as well; if false, null is returned for disabled accounts
     * @return - the account object for the given name, if matched
     */
    public synchronized SshAccount getAccountByName(String loginName, boolean allowDisabled) {
        SshAccount account = currentAccountMap.get(loginName);
        if (account == null) {
            return null; // not found
        } else if (account.isEnabled()) {
            return account; // standard case: account enabled
        } else {
            if (allowDisabled) {
                log.debug("Returning disabled account '" + account.getLoginName() + "' on explicit query");
                return account;
            } else {
                log.debug("Default query for disabled account '" + account.getLoginName() + "'; responding as if it did not exist");
                return null;
            }
        }
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

    public void setIdleTimeoutSeconds(Integer idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public void setPort(int sshContactPoint) {
        this.port = sshContactPoint;
    }

    protected synchronized List<SshAccountImpl> listAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(currentAccountMap.values()));
    }

    /**
     * Override the active set of accounts; for testing only, as it will be replaced by dynamic accounts updates.
     * 
     * @param users the new list of accounts
     */
    protected synchronized void setStaticAccounts(List<SshAccountImpl> users) {
        this.staticAccounts = users;
        // backwards compatibility fix; the new parse method takes care of that in actual operation
        if (staticAccounts == null) {
            staticAccounts = new ArrayList<>();
        }
        updateEffectiveAccountLookup();
    }

    public List<SshAccountRole> getRoles() {
        return roles;
    }

    public synchronized int getCurrentNumberOfAccouts() {
        return this.currentAccountMap.size();
    }

    public synchronized String getAccountDataOriginInfo() {
        return this.accountDataOriginInfo;
    }

    public synchronized List<SshAccountImpl> getStaticAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(staticAccounts));
    }

    private List<SshAccountImpl> parseAccountData(Map<String, ConfigurationSegment> accountData, String dataSourceInfo)
        throws ConfigurationException {
        List<SshAccountImpl> tempAccountList = new ArrayList<>();
        if (accountData == null) {
            return tempAccountList; // convert to empty list
        }
        for (Entry<String, ConfigurationSegment> entry : accountData.entrySet()) {
            try {
                SshAccountImpl account = entry.getValue().mapToObject(SshAccountImpl.class);
                account.setLoginName(entry.getKey());
                tempAccountList.add(account);
            } catch (IOException e) {
                throw new ConfigurationException(
                    "Error parsing the SSH account entry \"" + entry.getKey() + "\" loaded from " + dataSourceInfo);
            }
        }
        return tempAccountList;
    }

    private void updateEffectiveAccountLookup() {

        Map<String, SshAccountImpl> tempMap = new HashMap<>();
        if (!staticAccounts.isEmpty()) {
            if (!dynamicAccounts.isEmpty()) {
                log.warn("Merging " + staticAccounts.size() + " initial SSH accounts with " + dynamicAccounts.size()
                    + " loaded from " + DYNAMIC_ACCOUNTS_FILENAME + "; accounts from the latter take precedence");
                accountDataOriginInfo = "merged";
            } else {
                // no dynamic accounts
                accountDataOriginInfo = "static"; // includes case of no accounts at all
            }
        } else {
            // no static accounts
            accountDataOriginInfo = "dynamic";
        }

        // add static accounts first so dynamic ones take precedence
        for (SshAccountImpl account : staticAccounts) {
            SshAccountImpl replaced = tempMap.put(account.getLoginName(), account);
            if (replaced != null) {
                log.warn("Found multiple static SSH accounts with login name '" + account.getLoginName() + "'");
                if (account.isEnabled() != replaced.isEnabled()) {
                    log.warn("The SSH accounts with the same login name '" + account.getLoginName()
                        + "' have different 'enabled' settings (current account: " + account.isEnabled()
                        + "). Unless this is intentional, it is highly recommended to check your configuration!");
                }
            }
        }
        for (SshAccountImpl account : dynamicAccounts) {
            SshAccountImpl replaced = tempMap.put(account.getLoginName(), account);
            if (replaced != null) {
                log.warn("SSH account '" + account.getLoginName() + "' loaded from " + DYNAMIC_ACCOUNTS_FILENAME
                    + " replaces a previous account with the same login name");
                if (account.isEnabled() != replaced.isEnabled()) {
                    log.warn("The SSH accounts with the same login name '" + account.getLoginName()
                        + "' have different 'enabled' settings (current account: " + account.isEnabled()
                        + "). Unless this is intentional, it is highly recommended to check your configuration!");
                }
            }
        }

        currentAccountMap = tempMap;
    }

}
