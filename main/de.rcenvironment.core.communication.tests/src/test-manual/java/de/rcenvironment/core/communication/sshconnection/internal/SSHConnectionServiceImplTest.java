/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.communication.sshconnection.internal;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Test class for SSHConnectionServiceImpl.
 *
 * @author Brigitte Boden
 */
public class SSHConnectionServiceImplTest {
    
    private String ip;
    private int port;
    private String username;
    private String password;
    
    private TestParametersProvider testParameters;
    
    /**
     * Common setup.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Before
    public void setUp() throws Exception {
        testParameters = new ParameterizedTestUtils().readDefaultPropertiesFile(getClass());
        ip = testParameters.getNonEmptyString("ip");
        port = testParameters.getExistingInteger("port");
        username = testParameters.getNonEmptyString("username");
        password = testParameters.getNonEmptyString("passphrase");
    }
    
    /** Ignored, only used as "manual" test. 
     * @throws SshParameterException 
     * @throws JSchException */
    @Test
    @Ignore
    public void testCreateSshConnection() throws JSchException, SshParameterException {
        
        SshConnectionService sshService = new SshConnectionServiceImpl();
        String id = sshService.addSshConnectionWithAuthPhrase("displayName", ip, port, username, password, true, true);
    }

}
