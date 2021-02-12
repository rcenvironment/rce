/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.io.IOException;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindrot.jbcrypt.BCrypt;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.embedded.ssh.api.SshAccount;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Default {@link SshAccountConfigurationService} implementation.
 * 
 * @author Robert Mischke
 */
public class SshAccountConfigurationServiceImpl implements SshAccountConfigurationService {

    private static final String CONFIGURATION_PATH_SEPARATOR = "/";

    private static final String MAIN_SSH_CONFIGURATION_PATH = "sshServer";

    private static final String SSH_ACCOUNTS_SUB_CONFIGURATION_PATH = "accounts";

    private static final String ACCOUNT_ROLES_RA_SUB_CONFIGURATION_PATH = "roles/remote access";

    private static final String FIELD_NAME_PASSWORD = "password";

    private static final String FIELD_NAME_ENABLED = "enabled";

    private static final String FIELD_NAME_PW_HASH = "passwordHash";

    private static final String FIELD_NAME_ROLE = "role";

    private static final String DEFAULT_RA_ROLE_COMMAND_PATTERN = "ra .+";

    private static final String EXPECTED_RA_ROLE_ALLOWED_COMMAND_PATTERN_REGEXP =
        "help|exit|(" + DEFAULT_RA_ROLE_COMMAND_PATTERN + ")";

    // TODO add length restriction?
    private static final String VALID_ACCOUNT_NAME_REGEXP = "^[a-zA-Z][a-zA-Z0-9_\\-]*$";

    // TODO imperfect wording: does not exclude non-ASCII characters
    private static final String VALID_ACCOUNT_NAME_VIOLATION_MESSAGE =
        "The login name must begin with a letter, followed by letters, digits, or the characters \"_\" and \"-\".";

    // somewhat arbitrary; maximum Linux account name length seems to be 32, but that may break the UI
    private static final int MAX_LOGIN_NAME_LENGTH = 20;

    private static final String LOGIN_NAME_TOO_LONG_MESSAGE = "The login name must not be longer than "
        + MAX_LOGIN_NAME_LENGTH + " characters";

    private ConfigurationService configurationService;

    private SshConfiguration sshConfiguration;

    private SshAuthenticationManager sshAuthenticationManager;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public String verifyExpectedStateForConfigurationEditing() {
        if (!configurationService.isUsingIntendedProfileDirectory()) {
            return "This configuration mode does not seem to run in the intended profile directory "
                + " - is the profile being used by another RCE instance?\n\nIntended profile location: "
                + configurationService.getOriginalProfileDirectory().getAbsolutePath();
        }

        if (configurationService.isUsingDefaultConfigurationValues()) {
            return "The current configuration file is either invalid, or there was an error creating it. "
                + "Please check the file for errors. If the file does not "
                + "exist, make sure you have write access to the containing directory.\n\n"
                + "Intended configuration file location: "
                + configurationService.getProfileConfigurationFile().getAbsolutePath();
        }

        final ConfigurationSegment sshConfigurationSegment = loadSshConfigurationSegment();
        try {
            readAsParsedConfigurationData(sshConfigurationSegment);
        } catch (ConfigurationException e) {
            return "Error reading SSH account data: " + e.getMessage();
        }

        // no SSH configuration yet -> ok
        if (!sshConfigurationSegment.isPresentInCurrentConfiguration()) {
            return null;
        }

//        SshAccountRole raRole = sshConfiguration.getRoleByName("remote access");
//        raAccountRoleIsPresent = (raRole != null);
//        // verify the role if it is already present
//        if (raRole != null) {
//            if (!EXPECTED_RA_ROLE_ALLOWED_COMMAND_PATTERN_REGEXP.equals(raRole.getAllowedCommandRegEx())) {
//                return "The SSH Account role \"remote access\" exists, but has an unexpected "
//                    + "configuration. Please fix this manually (for example, by deleting the "
//                    + "existing \"role\" entry) and restart.";
//            }
//        }

        return null; // all ok
    }

    @Override
    public SortedMap<String, SshAccount> getStaticAccountsByLoginName() throws ConfigurationException {
        return sshAuthenticationManager.getStaticAccountsByLoginName();
    }

    @Override
    public SshAccount getAccount(String account) {
        return sshAuthenticationManager.getAccountByLoginName(account, true); // true = allow disabled accounts
    }

    @Override
    public void createAccount(String loginName, String password) throws ConfigurationException {
        if (StringUtils.isNullorEmpty(loginName)) {
            throw new ConfigurationException("The login name must not be empty!");
        }
        if (getAccount(loginName) != null) {
            throw new ConfigurationException("An account with this login name already exists.");
        }
        validateLoginName(loginName);
        if (StringUtils.isNullorEmpty(password)) {
            throw new ConfigurationException("The password must not be empty!");
        }
        String passwordHash = generatePasswordHash(password);

        WritableConfigurationSegment accountsSegment = getWritableConfigurationSegmentForAccountsList();

        try {
            final WritableConfigurationSegment accountElement = accountsSegment.createElement(loginName);
            // write account data
            accountElement.setString(FIELD_NAME_PW_HASH, passwordHash);
            accountElement.setString(FIELD_NAME_ROLE, SshConstants.ROLE_NAME_REMOTE_ACCESS_USER);
            accountElement.setBoolean(FIELD_NAME_ENABLED, true);
        } catch (ConfigurationException e) {
            log.error("Failed to add SSH account", e);
            throw new ConfigurationException("Failed to add the new account;"
                + " most likely, there is already an account with that login name");
        }

        writeConfigurationChanges();
        log.debug(StringUtils.format("Created SSH account '%s' (using password hash authentication)", loginName));
    }

    @Override
    public void updatePasswordHash(String loginName, String plainTextPassword) throws ConfigurationException {
        WritableConfigurationSegment accountSegment = getWritableSegmentForAccount(loginName);

        accountSegment.setString(FIELD_NAME_PW_HASH, generatePasswordHash(plainTextPassword));
        if (accountSegment.getString(FIELD_NAME_PASSWORD) != null) {
            // erase existing plain-text password
            accountSegment.setString(FIELD_NAME_PASSWORD, null);
        }

        writeConfigurationChanges();
        log.debug(StringUtils.format("Updated password hash for SSH account '%s'", loginName));
    }

    @Override
    public void updateRole(String loginName, String role) throws ConfigurationException {
        WritableConfigurationSegment accountSegment = getWritableSegmentForAccount(loginName);
        accountSegment.setString(FIELD_NAME_ROLE, role);
        writeConfigurationChanges();
        log.debug(StringUtils.format("Set role for SSH account '%s' to '%s'", loginName, role));
    }

    @Override
    public void setAccountEnabled(String loginName, boolean enabled) throws ConfigurationException {
        WritableConfigurationSegment accountSegment = getWritableSegmentForAccount(loginName);
        accountSegment.setBoolean(FIELD_NAME_ENABLED, enabled);
        writeConfigurationChanges();
        log.debug(StringUtils.format("Set SSH account '%s' to enabled=%s", loginName, enabled));
    }

    @Override
    public void deleteAccount(String loginName) throws ConfigurationException {
        WritableConfigurationSegment accountsSegment = getWritableConfigurationSegmentForAccountsList();
        if (!accountsSegment.deleteElement(loginName)) {
            throw new ConfigurationException(
                "Internal consistency error: Requested account deletion, but no matching configuration node was found");
        }
        writeConfigurationChanges();
        log.debug(StringUtils.format("SSH account '%s' deleted", loginName));
    }

    @Override
    public String generatePasswordHash(final String password) {
        // TODO move to common utilities; check iteration count
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    // note: overridden in unit test
    protected ConfigurationSegment loadSshConfigurationSegment() {
        return configurationService.getConfigurationSegment(MAIN_SSH_CONFIGURATION_PATH);
    }

    private void readAsParsedConfigurationData(final ConfigurationSegment sshConfigurationSegment) throws ConfigurationException {
        try {
            sshConfiguration = new SshConfiguration(sshConfigurationSegment);
            // null = no session tracker needed; 0 = allowed number of login attempts, not used here at all
            sshAuthenticationManager = new SshAuthenticationManager(sshConfiguration, null, 0);
        } catch (ConfigurationException | IOException e) {
            throw new ConfigurationException("Error reading SSH configuration data: " + e.getMessage());
        }
    }

    private void reloadConfiguration() throws ConfigurationException {
        readAsParsedConfigurationData(loadSshConfigurationSegment());
    }

    private WritableConfigurationSegment getWritableConfigurationSegmentAndWrapErrors(String path) throws ConfigurationException {
        WritableConfigurationSegment segment;
        try {
            segment = configurationService.getOrCreateWritableConfigurationSegment(path);
        } catch (ConfigurationException e) {
            // internal error; the best that can be done here is to exit with this message
            throw new ConfigurationException("Failed to access configuration data: " + e.getMessage());
        }
        return segment;
    }

    private WritableConfigurationSegment getWritableConfigurationSegmentForAccountsList() throws ConfigurationException {
        WritableConfigurationSegment accountsSegment =
            getWritableConfigurationSegmentAndWrapErrors(MAIN_SSH_CONFIGURATION_PATH + CONFIGURATION_PATH_SEPARATOR
                + SSH_ACCOUNTS_SUB_CONFIGURATION_PATH);
        return accountsSegment;
    }

    private WritableConfigurationSegment getWritableSegmentForAccount(String loginName) throws ConfigurationException {
        WritableConfigurationSegment accountSegment =
            getWritableConfigurationSegmentAndWrapErrors(MAIN_SSH_CONFIGURATION_PATH + CONFIGURATION_PATH_SEPARATOR
                + SSH_ACCOUNTS_SUB_CONFIGURATION_PATH + CONFIGURATION_PATH_SEPARATOR + loginName);
        return accountSegment;
    }

    private void writeConfigurationChanges() throws ConfigurationException {
        try {
            try {
                configurationService.writeConfigurationChanges();
            } catch (IOException e) {
                throw new ConfigurationException("There was an error writing the configuration changes to the profile folder: "
                    + e.getMessage());
            }
        } finally {
            // always reload to ensure consistent in-memory data
            reloadConfiguration();
        }
    }

    private void validateLoginName(String loginName) throws ConfigurationException {
        // note: rarely used, so not precompiling the regexp
        if (!loginName.matches(VALID_ACCOUNT_NAME_REGEXP)) {
            throw new ConfigurationException(VALID_ACCOUNT_NAME_VIOLATION_MESSAGE);
        }
        if (loginName.length() > MAX_LOGIN_NAME_LENGTH) {
            throw new ConfigurationException(LOGIN_NAME_TOO_LONG_MESSAGE);
        }
    }
}
