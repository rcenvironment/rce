/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.ssh.jsch.executor.context;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.common.validation.ValidationFailureException;
import de.rcenvironment.core.utils.executor.CommandLineExecutor;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfiguration;
import de.rcenvironment.core.utils.ssh.jsch.SshSessionConfigurationFactory;
import de.rcenvironment.core.utils.ssh.jsch.DummyCommand;
import de.rcenvironment.core.utils.ssh.jsch.DummyPasswordAuthenticator;
import de.rcenvironment.core.utils.ssh.jsch.SshTestUtils;

/**
 * Test case for {@link JSchExecutorContext}.
 * 
 * @author Doreen Seider
 */
public class JschExecutorContextTest {

    private static final String LOCALHOST = "localhost";

    private static final int INVALID_PORT = -22;

    private static SshServer sshServer;
    
    private static int port;

    private volatile boolean failed = false;
    
    private final SshSessionConfiguration sshConfiguration = SshSessionConfigurationFactory
        .createSshSessionConfigurationWithAuthPhrase(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
            DummyPasswordAuthenticator.PASSWORD);

    /**
     * Set up test environment. 
     * @throws IOException on error
     **/
    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws IOException {
        port = SshTestUtils.getRandomPortNumber();
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<NamedFactory<UserAuth>>() {{ add(new UserAuthPassword.Factory()); }});
        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
        sshServer.setCommandFactory(SshTestUtils.createDummyCommandFactory());
        sshServer.start();
    }
    
    /**
     * Set up work dir.
     * 
     * @throws IOException on error
     **/
    @Before
    public void createWorkDir() throws IOException {
        failed = false;
    }
    
    /**
     * Tear down test environment.
     * @throws InterruptedException 
     **/
    @AfterClass
    public static void tearDown() throws InterruptedException {
        sshServer.stop();
    }
    
    /** 
     * Test. 
     * @throws IOException on error
     * @throws ValidationFailureException on error
     **/
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testLifecycle() throws IOException, ValidationFailureException {
        JSchExecutorContext context = new JSchExecutorContext(sshConfiguration);
        
        context.setUpSession();
        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (!command.contains("mkdir -p ")) {
                    failed = true;
                }
                return new DummyCommand();
            }
        });
        
        CommandLineExecutor executor = context.setUpSandboxedExecutor();
        if (failed) {
            fail();
        }
        
        sshServer.setCommandFactory(new CommandFactory() {
            
            @Override
            public Command createCommand(String command) {
                if (!command.contains("rm ") && !command.contains("rmdir")) {
                    failed = true;
                }
                return new DummyCommand();
            }
        });

        context.tearDownSandbox(executor);
        
        context.tearDownSession();
        
    }
    
    /** 
     * Test. 
     * @throws IOException on error
     * @throws ValidationFailureException on error
     **/
    @Test(timeout = SshTestUtils.TIMEOUT)
    public void testForLifecycleFailure() throws IOException, ValidationFailureException {
        
        SshSessionConfiguration invalidSshConfiguration = SshSessionConfigurationFactory
            .createSshSessionConfigurationWithAuthPhrase(LOCALHOST, INVALID_PORT, DummyPasswordAuthenticator.USERNAME,
                DummyPasswordAuthenticator.PASSWORD);
        
        JSchExecutorContext context = new JSchExecutorContext(invalidSshConfiguration);
        
        try {
            context.setUpSession();
            fail();
        } catch (ValidationFailureException e) {
            assertTrue(true);
        }
        
        context = new JSchExecutorContext(sshConfiguration);

        try {
            context.setUpSandboxedExecutor();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }

        try {
            context.tearDownSession();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
        
        JSchExecutorContext context2 = new JSchExecutorContext(sshConfiguration);
        context2.setUpSession();
        CommandLineExecutor executor = context2.setUpSandboxedExecutor();
        
        try {
            context.tearDownSandbox(executor);
            fail();
        } catch (IllegalStateException e) {
            assertTrue(true);
        }

    }
    
    /** 
     * Test. 
     * @throws IOException on error
     * @throws ValidationFailureException on error
     **/
    @Test
    public void testCreateUniqueTempDir() throws IOException, ValidationFailureException {
        JSchExecutorContext context = new JSchExecutorContext(sshConfiguration);

        String contextHint = RandomStringUtils.randomAlphanumeric(5);
        assertTrue(context.createUniqueTempDir(contextHint).contains(contextHint));
    }
}
