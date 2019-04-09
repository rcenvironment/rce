/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

/**
 * Test case for {@link SshSessionConfigurationFactory}.
 * 
 * @author Doreen Seider
 */
public class SshSessionConfigurationFactoryTest {
    
    private final String destinationHost = RandomStringUtils.randomAlphanumeric(6);

    private final int port = 5000;

    private final String sshAuthUser = RandomStringUtils.randomAlphanumeric(5);

    private final String sshAuthPassPhrase = RandomStringUtils.randomAlphanumeric(8);
    
    private final String sshKeyFileLocation = RandomStringUtils.randomAlphanumeric(8);

    /** Test. */
    @Test
    public void testCreateSshSessionConfigurationWithAuthPhrase() {
        SshSessionConfiguration config = SshSessionConfigurationFactory.createSshSessionConfigurationWithAuthPhrase(destinationHost, port,
            sshAuthUser, sshAuthPassPhrase);
        assertEquals(destinationHost, config.getDestinationHost());
        assertEquals(port, config.getPort());
        assertEquals(sshAuthUser, config.getSshAuthUser());
        assertEquals(sshAuthPassPhrase, config.getSshAuthPhrase());
    }
    
    /** Test. */
    @Test
    public void testCreateSshSessionConfigurationWithKeyFileLocation() {
        SshSessionConfiguration config = SshSessionConfigurationFactory.createSshSessionConfigurationWithKeyFileLocation(destinationHost,
            port, sshAuthUser, sshKeyFileLocation);
        assertEquals(destinationHost, config.getDestinationHost());
        assertEquals(port, config.getPort());
        assertEquals(sshAuthUser, config.getSshAuthUser());
        assertEquals(sshKeyFileLocation, config.getSshKeyFileLocation());
    }
}
