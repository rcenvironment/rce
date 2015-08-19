/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

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
        ConfigurationSegment config = ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().
            getResourceAsStream("/config-tests/example1.json"));
        CommunicationConfiguration testInstance = new CommunicationConfiguration(config);

        assertEquals(8 * TEN_THOUSAND, testInstance.getRequestTimeoutMsec());
        assertEquals(9 * TEN_THOUSAND, testInstance.getForwardingTimeoutMsec());
        assertEquals(1, testInstance.getRemoteContactPoints().size());
        assertEquals(1, testInstance.getProvidedContactPoints().size());

        String connection1 = testInstance.getRemoteContactPoints().get(0);
        assertEquals("activemq-tcp:127.0.0.2:20002"
            + "(autoRetryInitialDelay=4,autoRetryMaximumDelay=600,autoRetryDelayMultiplier=2.5,connectOnStartup=true)", connection1);

        String serverPort1 = testInstance.getProvidedContactPoints().get(0);
        assertEquals("activemq-tcp:1.2.3.4:20009", serverPort1);
    }
}
