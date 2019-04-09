/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch.internal;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

/**
 * Test case for {@link SshSessionConfigurationImpl}.
 *
 * @author Doreen Seider
 */
public class SshSessionConfigurationImplTest {

    private final String destinationHost = RandomStringUtils.random(5);
    
    private final int port = 6000;
    
    private final String sshAuthUser = RandomStringUtils.random(5);
    
    private final String sshAuthPassPhrase = RandomStringUtils.random(5);
    
    private final String sshKeyFileLocation = RandomStringUtils.random(5);
    
    /** Test. */
    @Test
    public void test() {
        SshSessionConfigurationImpl config = new SshSessionConfigurationImpl(destinationHost, port, sshAuthUser,
            sshAuthPassPhrase, sshKeyFileLocation);
        
        assertEquals(destinationHost, config.getDestinationHost());
        assertEquals(port, config.getPort());
        assertEquals(sshAuthUser, config.getSshAuthUser());
        assertEquals(sshAuthPassPhrase, config.getSshAuthPhrase());
        assertEquals(sshKeyFileLocation, config.getSshKeyFileLocation());
    }
}
