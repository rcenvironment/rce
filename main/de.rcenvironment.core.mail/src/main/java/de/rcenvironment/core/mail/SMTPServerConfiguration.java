/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail;

import java.io.File;
import java.io.IOException;

import jodd.mail.EmailAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.mail.internal.MailFilterInformation;
import de.rcenvironment.core.mail.internal.PasswordObfuscationHelper;
import de.rcenvironment.core.utils.common.JsonUtils;

/**
 * Class providing the configuration of the mail bundle.
 * 
 * @author Tobias Rodehutskors
 */
public class SMTPServerConfiguration {

    /**
     * The path in the configuration file to the SMTP server configuration.
     */
    public static final String CONFIGURATION_PATH = "smtpServer";

    /**
     * If implicit encryption mode is specified, an SSL/TLS connection should be opened to the SMTP server.
     */
    public static final String IMPLICIT_ENCRYPTION = "implicit";

    /**
     * If explicit encryption mode is specified, a plain text connection should be opened to the SMTP server and upgraded to a secured
     * connection using the STARTTLS command.
     */
    public static final String EXPLICIT_ENCRYPTION = "explicit";

    /**
     * Configuration key for the host element.
     */
    public static final String CONFIG_KEY_HOST = "host";

    /**
     * Configuration key for the port element.
     */
    public static final String CONFIG_KEY_PORT = "port";

    /**
     * Configuration key for the username element.
     */
    public static final String CONFIG_KEY_USERNAME = "username";

    /**
     * Configuration key for the password element.
     */
    public static final String CONFIG_KEY_PASSWORD = "password";

    /**
     * Configuration key for the sender element.
     */
    public static final String CONFIG_KEY_SENDER = "sender";

    /**
     * Configuration key for the encryption element.
     */
    public static final String CONFIG_KEY_ENCRYPTION = "encryption";

    private static final int LARGEST_ALLOWED_PORT_NUMBER = 65535;

    private static final String EXTRAS_MAIL_FILTER = "extras" + File.separatorChar + "mail" + File.separatorChar + "filter.json";

    private static Log log = LogFactory.getLog(SMTPServerConfiguration.class);

    private String host;

    private int port;

    private String encryption;

    private String username;

    private String password;

    private EmailAddress sender;

    private MailFilterInformation filter;

    /**
     * @param configurationSegment
     * @param filter The filter to validated the given username against. If this field is null, the given username is not checked against a
     *        filter.
     */
    public SMTPServerConfiguration(ConfigurationSegment configurationSegment, MailFilterInformation filter) {

        if (configurationSegment != null) {

            host = configurationSegment.getString(CONFIG_KEY_HOST);
            port = configurationSegment.getInteger(CONFIG_KEY_PORT, 0);
            username = configurationSegment.getString(CONFIG_KEY_USERNAME);
            password = configurationSegment.getString(CONFIG_KEY_PASSWORD);
            password = PasswordObfuscationHelper.deobfuscate(password);
            encryption = configurationSegment.getString(CONFIG_KEY_ENCRYPTION);

            String senderAsString = configurationSegment.getString(CONFIG_KEY_SENDER);
            if (senderAsString != null) {
                sender = new EmailAddress(senderAsString);
            }
        }

        this.filter = filter;
    }

    /**
     * Checks if the current configuration is valid.
     * 
     * @return True, if the configuration is valid.
     * @throws ConfigurationException thrown if the configuration is invalid.
     */
    public boolean isValid() throws ConfigurationException {

        if (host == null || host.isEmpty()) {
            throw new ConfigurationException("You need to specify the host.");
        }

        if (port < 1 || port > LARGEST_ALLOWED_PORT_NUMBER) {
            throw new ConfigurationException("Invalid port number.");
        }

        if (username == null || username.isEmpty()) {
            throw new ConfigurationException("You need to specify the user you want to use for authenticating.");
        }

        // throw a ConfigurationException if the host name matches a given regex and the username does not match another given regex
        if (filter != null && host.matches(filter.getHostRegex()) && !username.matches(filter.getUsernameRegex())) {
            throw new ConfigurationException(filter.getErrorMessage());
        }

        if (password == null || password.isEmpty()) {
            throw new ConfigurationException("You need to specify the password you want to use for authenticating.");
        }

        if (sender == null || !sender.isValid()) {
            throw new ConfigurationException("You need to specify a valid email address as a sender address.");
        }

        // encryption either has to be explicit or implicit
        if (encryption == null || !(EXPLICIT_ENCRYPTION.equals(encryption) || IMPLICIT_ENCRYPTION.equals(encryption))) {
            throw new ConfigurationException("You need to specify if you want to use explicit or implicit encryption.");
        }

        return true;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getEncryption() {
        return encryption;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public EmailAddress getSender() {
        return sender;
    }

    public String getSenderAsString() {
        return sender.getInternetAddress().toString();
    }

    /**
     * Helper function to load MailFilterInformation from a file.
     * 
     * @param configurationService The ConfigurationService of the RCE instance.
     * @return A {@link MailFilterInformation} object if the defining filter.json file was found or otherwise null.
     */
    public static MailFilterInformation getMailFilterInformation(ConfigurationService configurationService) {

        File installationDataRoot =
            configurationService.getConfigurablePath(ConfigurationService.ConfigurablePathId.INSTALLATION_DATA_ROOT);
        File filterFile = new File(installationDataRoot, EXTRAS_MAIL_FILTER);

        if (filterFile != null && filterFile.exists()) {
            ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();

            try {
                return mapper.readValue(filterFile, MailFilterInformation.class);
            } catch (IOException e) {
                log.error("IOException while reading the MailFilterInformation from the file.", e);
                return null;
            }
        } else {
            log.debug("Cannot find a file to read the MailFilterInformation from.");
            return null;
        }

    }
}
