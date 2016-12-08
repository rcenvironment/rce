/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.mail.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.mail.SMTPServerConfiguration;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Test case for the class {@link SMTPServerConfiguration}.
 * 
 * @author Tobias Rodehutskors
 */
public class SMTPServerConfigurationTest {

    private static final int DEFAULT_PORT = 25;

    /**
     * ExpectedException.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Tests loading a test configuration file.
     * 
     * @throws IOException on uncaught exceptions
     * @throws ConfigurationException on configuration exceptions.
     */
    @Test
    public void testLoading() throws IOException, ConfigurationException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream("/validConfig.json"));

        SMTPServerConfiguration configuration = new SMTPServerConfiguration(configurationSegment, null);

        assertEquals(DEFAULT_PORT, configuration.getPort());
        assertEquals("localhost", configuration.getHost());
        assertEquals("testpw", configuration.getPassword());
        assertEquals("testuser", configuration.getUsername());
        assertEquals("implicit", configuration.getEncryption());
        assertEquals("valid@mail.ad", configuration.getSenderAsString());

        assertTrue(configuration.isValid());
    }

    /**
     * Tests default values when providing an empty {@link ConfigurationSegment}.
     * 
     * @throws ConfigurationException expected
     */
    @Test
    public void testEmptyConfiguration() throws ConfigurationException {
        ConfigurationSegment configurationSegment = ConfigurationSegmentUtils.createEmptySegment();
        SMTPServerConfiguration configuration = new SMTPServerConfiguration(configurationSegment, null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(containsString("You need to specify the host."));

        assertFalse(configuration.isValid());
    }

    /**
     * Tests if a user account is properly rejected according to the defined filter.
     * 
     * @throws IOException unexpected
     * @throws ConfigurationException expected
     */
    @Test
    public void testFilterForbiddenUsernameIsProperlyRejected() throws IOException, ConfigurationException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils
                .readTestConfigurationFromStream(getClass().getResourceAsStream("/filterForbiddenUsernameConfig.json"));
        SMTPServerConfiguration configuration = new SMTPServerConfiguration(configurationSegment,
            new MailFilterInformation(".*dlr.de", "f_.*", "Only functional DLR accounts are allowed."));

        expectedException.expect(ConfigurationException.class);

        assertFalse(configuration.isValid());
    }

    /**
     * Tests if a user account is not rejected if it does not match the defined filter.
     * 
     * @throws IOException unexpected
     * @throws ConfigurationException unexpected
     */
    @Test
    public void testFilterAllowedUsernameIsNotRejected() throws IOException, ConfigurationException {
        TempFileServiceAccess.setupUnitTestEnvironment();
        ConfigurationSegment configurationSegment =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream("/filterAllowedUsernameConfig.json"));
        SMTPServerConfiguration configuration = new SMTPServerConfiguration(configurationSegment,
            new MailFilterInformation(".*dlr.de", "f_.*", "Only functional DLR accounts are allowed."));

        assertTrue(configuration.isValid());
    }
}
