/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import de.rcenvironment.core.configuration.ConfigurationException;

/**
 * Provides methods for querying and editing the SMTP server configuration. Currently used by the text mode configuration UI.
 *
 * Note that these operations NOT yet safe for concurrent operation (e.g. for calling from the standard UI)!
 *
 * @author Tobias Rodehutskors
 */
public interface SMTPServerConfigurationService {
    
    /**
     * Stores the given parameters as the SMTP server configuration in the configuration file. Existing values will be overwritten.
     * 
     * @param host The host name of the SMTP server.
     * @param port The port of the SMTP server.
     * @param encryption The encryption used to connect to the server. Has to be either 'implicit' or 'explicit'.
     * @param username The user name for authentication.
     * @param password The password for authentication.
     * @param sender The sender of all mails.
     * @throws ConfigurationException Thrown if the configuration resulting from the given values is invalid.
     */
    void configureSMTPServer(String host, int port, String encryption, String username, String password, String sender)
        throws ConfigurationException;

    /**
     * @return Returns a MailConfiguration object representing the currently stored SMTP mail server configuration (even if it is invalid).
     *         Returns null if the SMTP server configuration is not present.
     */
    SMTPServerConfiguration getSMTPServerConfiguration();
}
