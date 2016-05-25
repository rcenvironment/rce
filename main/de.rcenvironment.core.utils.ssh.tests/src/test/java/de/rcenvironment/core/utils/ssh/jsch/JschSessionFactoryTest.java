/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.LogFactory;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;

/**
 * Test case for {@link JschSessionFactory}.
 * 
 * @author Doreen Seider
 */
public class JschSessionFactoryTest {

    private static final String LOCALHOST = "localhost";
    
    private static int port;
    
    private SshServer sshServer;
    
    /**
     * Set up test environment. 
     * @throws IOException on error
     **/
    @SuppressWarnings("serial")
    @Before
    public void setUp() throws IOException {
        port = SshTestUtils.getRandomPortNumber();
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<NamedFactory<UserAuth>>() {{ add(new UserAuthPassword.Factory()); }});
        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
        sshServer.start();
    }
    
    /**
     * Tear down test environment.
     * @throws InterruptedException 
     **/
    @After
    public void tearDown() throws InterruptedException {
        sshServer.stop();
    }
    
    /**
     * Test.
     **/
    @Test
    public void testCreateDelegateLogger() {
        JschSessionFactory.createDelegateLogger(LogFactory.getLog(JschSessionFactoryTest.class));
    }
    
    /**
     * Test.
     * @throws SshParameterException on error
     * @throws JSchException on error
     **/
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testSetupSession() throws JSchException, SshParameterException {
        // TODO test with key files
        
        Logger logger = JschSessionFactory.createDelegateLogger(LogFactory.getLog(JschSessionFactoryTest.class));
        
        JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME, null,
            DummyPasswordAuthenticator.PASSWORD, logger);
        
        try {
            JschSessionFactory.setupSession("horst", port, DummyPasswordAuthenticator.USERNAME, null,
                DummyPasswordAuthenticator.PASSWORD, logger);
            fail();
        } catch (JSchException e) {
            assertTrue(true);
        }
        
        try {
            JschSessionFactory.setupSession(LOCALHOST, port + 1, DummyPasswordAuthenticator.USERNAME, null,
                DummyPasswordAuthenticator.PASSWORD, logger);
            fail();
        } catch (JSchException e) {
            assertTrue(true);
        }
        
        try {
            JschSessionFactory.setupSession(LOCALHOST, port + 1, DummyPasswordAuthenticator.USERNAME, null,
                DummyPasswordAuthenticator.PASSWORD_INVALID, logger);
            fail();
        } catch (JSchException e) {
            assertTrue(true);
        }
        
        try {
            JschSessionFactory.setupSession(LOCALHOST, port + 1, DummyPasswordAuthenticator.USERNAME_UNKNOWN, null,
                DummyPasswordAuthenticator.PASSWORD, logger);
            fail();
        } catch (JSchException e) {
            assertTrue(true);
        }
        
        try {
            JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME, null, "", logger);
            fail();
        } catch (SshParameterException e) {
            assertTrue(true);
        }
        try {
            JschSessionFactory.setupSession(LOCALHOST, port, "", null, DummyPasswordAuthenticator.PASSWORD, logger);
            fail();
        } catch (SshParameterException e) {
            assertTrue(true);
        }
    }
}
