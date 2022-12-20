/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Unit tests for {@link CommunicationConfiguration}.
 * 
 * @author Robert Mischke
 */

public class CommunicationConfigurationTest {

    private static final int TEN_THOUSAND = 10000; // yay for Checkstyle

    /**
     * Test setup.
     */
    @Before
    public void setUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Tests behaviour on non-existing configuration.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    public void testEmptyConfiguration() throws IOException {
        ConfigurationSegment emptySegment = ConfigurationSegmentUtils.createEmptySegment();
        CommunicationConfiguration testInstance = new CommunicationConfiguration(emptySegment);

        assertEquals(CommunicationConfiguration.DEFAULT_REQUEST_TIMEOUT_MSEC, testInstance.getRequestTimeoutMsec());
        assertEquals(CommunicationConfiguration.DEFAULT_FORWARDING_TIMEOUT_MSEC, testInstance.getForwardingTimeoutMsec());
        assertEquals(0, testInstance.getRemoteContactPoints().size());
        assertEquals(0, testInstance.getProvidedContactPoints().size());
    }

    /**
     * Reads "/config-tests/example1.json" and verifies the effective settings.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    public void readTestConfigFile() throws IOException {
        ConfigurationSegment config = readExampleConfigurationSegment("example1.json");
        CommunicationConfiguration testInstance = new CommunicationConfiguration(config);

        assertEquals(8 * TEN_THOUSAND, testInstance.getRequestTimeoutMsec());
        assertEquals(9 * TEN_THOUSAND, testInstance.getForwardingTimeoutMsec());
        assertEquals(1, testInstance.getRemoteContactPoints().size());
        assertEquals(1, testInstance.getProvidedContactPoints().size());

        String connection1 = testInstance.getRemoteContactPoints().get(0);
        assertEquals("activemq-tcp:127.0.0.2:20002"
            + "(autoRetryInitialDelay=4,autoRetryMaximumDelay=600,autoRetryDelayMultiplier=2.5,connectOnStartup=true,autoRetry=true)", connection1);

        String serverPort1 = testInstance.getProvidedContactPoints().get(0);
        assertEquals("activemq-tcp:1.2.3.4:20009", serverPort1);
    }

    /**
     * Verifies that invalid data fields do not cause uncaught exceptions.
     * 
     * @throws IOException on uncaught errors
     * @throws ConfigurationException on uncaught errors
     */
    @Test
    public void testCorruptedConfiguration() throws IOException, ConfigurationException {
        final String baselineConfigurationFile = "example1.json";
        final String testConnectionEntryPath = "connections/1";
        ConfigurationSegment configData;

        configData = readExampleConfigurationSegment(baselineConfigurationFile);
        configData.getOrCreateWritableSubSegment(testConnectionEntryPath).deleteElement("host"); // missing
        assertConnectionGetsDiscardedWithoutException(configData);

        configData = readExampleConfigurationSegment(baselineConfigurationFile);
        configData.getOrCreateWritableSubSegment(testConnectionEntryPath).deleteElement("port"); // missing
        assertConnectionGetsDiscardedWithoutException(configData);

        configData = readExampleConfigurationSegment(baselineConfigurationFile);
        configData.getOrCreateWritableSubSegment(testConnectionEntryPath).setString("port", "x"); // non-integer value
        assertConnectionGetsDiscardedWithoutException(configData);
    }

    private void assertConnectionGetsDiscardedWithoutException(ConfigurationSegment configData) {
        // attempt to parse the result; it should neither throw an exception nor add the connection entry
        final CommunicationConfiguration parsedConfig = new CommunicationConfiguration(configData);
        assertEquals(0, parsedConfig.getRemoteContactPoints().size());
    }

    private ConfigurationSegment readExampleConfigurationSegment(String testFileName) throws IOException {
        ConfigurationSegment config =
            ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().getResourceAsStream("/config-tests/"
                + testFileName));
        return config;
    }
}
