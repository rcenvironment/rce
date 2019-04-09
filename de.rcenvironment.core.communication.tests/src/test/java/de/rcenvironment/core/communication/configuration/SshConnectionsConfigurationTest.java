/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.sshconnection.InitialSshConnectionConfig;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.testutils.ConfigurationSegmentUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Unit test for {@link SshConnectionsConfiguration}.
 *
 * @author Brigitte Boden
 */
public class SshConnectionsConfigurationTest {

    private static final int PORT = 31000;

    /**
     * Test setup.
     */
    @Before
    public void setUp() {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Reads "/config-tests/exampleSsh.json" and verifies the effective settings.
     * 
     * @throws IOException on uncaught errors
     */
    @Test
    public void readTestConfigFile() throws IOException {
        ConfigurationSegment config = ConfigurationSegmentUtils.readTestConfigurationFromStream(getClass().
            getResourceAsStream("/config-tests/exampleSsh.json"));
        SshConnectionsConfiguration sshConfig = new SshConnectionsConfiguration(config);
        assertEquals(1, sshConfig.getProvidedConnectionConfigs().size());
        InitialSshConnectionConfig connection = sshConfig.getProvidedConnectionConfigs().get(0);
        assertEquals("exampleSSHConnection", connection.getId());
        assertEquals("example", connection.getDisplayName());
        assertEquals("127.0.0.1", connection.getHost());
        assertEquals(PORT, connection.getPort());
        assertEquals("sample-user", connection.getUser());
        assertEquals("filepath", connection.getKeyFileLocation());
        assertFalse(connection.getUsePassphrase());
    }
}
