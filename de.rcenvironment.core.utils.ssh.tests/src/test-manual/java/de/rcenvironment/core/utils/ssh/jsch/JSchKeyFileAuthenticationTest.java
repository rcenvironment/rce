/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */


package de.rcenvironment.core.utils.ssh.jsch;


import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Manual test case.
 * 
 * @author Brigitte Boden
 */
public class JSchKeyFileAuthenticationTest {

    private String ip;
    
    private int port;
    
    private String username;
    
    //Optional passphrase
    private String passphrase;
    
    private String keyFileLocation;
    
    private TestParametersProvider testParameters;
    
    private Log log = LogFactory.getLog(getClass());

    /**
     * Set up test environment.
     * 
     * @throws IOException on error
     **/
    @Before
    public void setUp() throws IOException {
        testParameters = new ParameterizedTestUtils().readDefaultPropertiesFile(getClass());
        ip = testParameters.getNonEmptyString("ip");
        port = testParameters.getExistingInteger("port");
        username = testParameters.getNonEmptyString("username");
        passphrase = testParameters.getOptionalString("passphrase");
        keyFileLocation = testParameters.getNonEmptyString("keyfilelocation");
    }

    /**
     * Test.
     * 
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws IOException on error
     * @throws InterruptedException on error
     */
    @Test
    public void testAuthentication() throws JSchException, SshParameterException, IOException, InterruptedException {

        Session session = JschSessionFactory.setupSession(ip, port, username,
            keyFileLocation, passphrase, null);
        
        
    }

}
