/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import java.io.IOException;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.WritableConfigurationSegment;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.mail.SMTPServerConfigurationService;

/**
 * Default implementation of {@link SMTPServerConfigurationService}.
 *
 * @author Tobias Rodehutskors
 */
public class SMTPServerConfigurationServiceImpl implements SMTPServerConfigurationService {

    private ConfigurationService configurationService;

    protected void bindConfigurationService(ConfigurationService newConfigurationService) {
        this.configurationService = newConfigurationService;
    }

    @Override
    public SMTPServerConfiguration getSMTPServerConfiguration() {
        final ConfigurationSegment mailConfigurationSegment =
            configurationService.getConfigurationSegment(SMTPServerConfiguration.CONFIGURATION_PATH);

        if (mailConfigurationSegment != null && mailConfigurationSegment.isPresentInCurrentConfiguration()) {
            return new SMTPServerConfiguration(mailConfigurationSegment,
                SMTPServerConfiguration.getMailFilterInformation(configurationService));
        } else {
            return null;
        }
    }

    @Override
    public void configureSMTPServer(String host, int port, String encryption, String username, String password, String sender)
        throws ConfigurationException {

        WritableConfigurationSegment mailConfig =
            configurationService.getOrCreateWritableConfigurationSegment(SMTPServerConfiguration.CONFIGURATION_PATH);

        mailConfig.setString(SMTPServerConfiguration.CONFIG_KEY_HOST, host);
        mailConfig.setInteger(SMTPServerConfiguration.CONFIG_KEY_PORT, port);
        mailConfig.setString(SMTPServerConfiguration.CONFIG_KEY_ENCRYPTION, encryption);
        mailConfig.setString(SMTPServerConfiguration.CONFIG_KEY_USERNAME, username);
        mailConfig.setString(SMTPServerConfiguration.CONFIG_KEY_PASSWORD, PasswordObfuscationHelper.obfuscate(password));
        mailConfig.setString(SMTPServerConfiguration.CONFIG_KEY_SENDER, sender);

        if (new SMTPServerConfiguration(mailConfig,
            SMTPServerConfiguration.getMailFilterInformation(configurationService)).isValid()) { // reuse the validation code

            try {
                configurationService.writeConfigurationChanges();
            } catch (IOException e) {
                throw new ConfigurationException("There was an error writing the configuration changes to the profile folder: "
                    + e.getMessage());
            }

        } else {
            throw new ConfigurationException("TODO enter a useful error message here"); // TODO
        }
    }
}
