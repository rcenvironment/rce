/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.configuration.testutils.TestConfigurationProvider;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test case for the class {@link SMTPServerConfigurationServiceImpl}.
 *
 * @author Tobias Rodehutskors
 */
public class SMTPServerConfigurationServiceImplTest {

    private static final int EXPECTED_PORT_25 = 25;
    private SMTPServerConfigurationServiceImpl smtpServerConfigurationImpl;

    private void setupSMTPServerConfigurationService(String configurationLocation) throws IOException {

        smtpServerConfigurationImpl = new SMTPServerConfigurationServiceImpl();

        // load a configuration segment from the resources ...
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream(configurationLocation));

        // ... and make it available to the service
        TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
        configurationProvider.setConfigurationSegment(SMTPServerConfiguration.CONFIGURATION_PATH, configurationSegment);
        smtpServerConfigurationImpl.bindConfigurationService(configurationProvider);
    }

    /**
     * Tests, if the SMTP configuration content can be retrieved even if the content is invalid.
     * 
     * @throws IOException unexpected
     * @throws ConfigurationException unexpected
     */
    @Test
    public void testInvalidConfig() throws IOException, ConfigurationException {
        setupSMTPServerConfigurationService("/invalidConfig.json");
        SMTPServerConfiguration smtpServerConfiguration = smtpServerConfigurationImpl.getSMTPServerConfiguration();
        assertEquals(smtpServerConfiguration.getHost(), "localhost");
        assertEquals(smtpServerConfiguration.getPort(), EXPECTED_PORT_25);
    }

    /**
     * Tests the behavior if no SMTP mail server configuration is available.
     * 
     * @throws IOException unexpected
     * @throws ConfigurationException unexpected
     */
    @Test
    public void testNoConfig() throws IOException, ConfigurationException {
        smtpServerConfigurationImpl = new SMTPServerConfigurationServiceImpl();
        TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
        smtpServerConfigurationImpl.bindConfigurationService(configurationProvider);

        SMTPServerConfiguration smtpServerConfiguration = smtpServerConfigurationImpl.getSMTPServerConfiguration();
        assertNull(smtpServerConfiguration);
    }
}
