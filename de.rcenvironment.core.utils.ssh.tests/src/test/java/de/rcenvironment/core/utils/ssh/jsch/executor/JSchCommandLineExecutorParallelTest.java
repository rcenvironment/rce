/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.ssh.jsch.executor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.DummyPasswordAuthenticator;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.SshTestUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Test case for {@link JSchCommandLineExecutor}.
 * 
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class JSchCommandLineExecutorParallelTest {

    private static final int TIMEOUT = 20000;

    private static final String LOCALHOST = "localhost";

    private TempFileService tempFileService = TempFileServiceAccess.getInstance();
    
    private File localWorkdir;

    private File remoteWorkdir;

    /**
     * Initial set up of test environment.
     * 
     * @throws IOException on unexpected error
     */
    @BeforeClass
    public static void initialSetUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Set up test environment.
     * 
     * @throws IOException on unexpected error
     **/
    @Before
    public void setUp() throws IOException {
        remoteWorkdir = tempFileService.createManagedTempDir();
        localWorkdir = tempFileService.createManagedTempDir();
    }

    /**
     * Tear down test environment.
     * 
     * @throws InterruptedException on error when stopping the server
     * @throws IOException on unexpected error
     **/
    @After
    public void tearDown() throws InterruptedException, IOException {
        tempFileService.disposeManagedTempDirOrFile(remoteWorkdir);
        tempFileService.disposeManagedTempDirOrFile(localWorkdir);
    }
    
    /**
     * Tests parallel running of several SSH servers (as could happen i.e. on Jenkins)
     * 
     * @author Brigitte Boden
     * @throws Exception on unexpected errors
     */
    @SuppressWarnings("serial")
    @Test(timeout = TIMEOUT)
    public void testParallelServers() throws Exception {

        final int numServers = 10;
        final CountDownLatch threadsCompletedLatch = new CountDownLatch(numServers);

        RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();
        
        for (int i = 0; i < numServers; i++) {

            runnablesGroup.add(new Runnable() {

                @Override
                @TaskDescription(value = "Running SSH server")
                public void run() {
                    SshServer sshServer;
                    int port;
                    // Starting the SSH server will fail if the port is already in use, in that case it will be retried with another port.
                    int retry = 0;
                    do {
                        port = SshTestUtils.getRandomPortNumber();
                        sshServer = SshServer.setUpDefaultServer();
                        sshServer.setPort(port);
                        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
                        sshServer.setUserAuthFactories(new ArrayList<UserAuthFactory>() {
                            {
                                add(new UserAuthPasswordFactory());
                            }
                        });
                        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
                        try {
                            sshServer.start();
                            break;
                        } catch (IOException e) {
                            if (retry++ > 3) {
                                throw new RuntimeException("Failed to start SSH server at port: "
                                    + port, e);                                
                            }
                        }
                    } while (true);
                     
                    
                    // Establish a connections and est downloading work dir
                    final String fileContent = RandomStringUtils.randomAlphabetic(6);

                    try {
                        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
                            null, DummyPasswordAuthenticator.PASSWORD, null);
                        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, remoteWorkdir.getAbsolutePath());

                        SshTestUtils.createFileOnServerSidesWorkDir(sshServer, session, remoteWorkdir,
                            RandomStringUtils.randomAlphabetic(6), fileContent, executor);

                        sshServer.setCommandFactory(new ScpCommandFactory());

                        File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
                        executor.downloadWorkdir(dir);
                        assertEquals(1, dir.listFiles().length);

                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
                    } catch (JSchException | SshParameterException | IOException | InterruptedException e) {
                        throw new RuntimeException("Testing connection failed", e);
                    }

                    try {
                        sshServer.stop();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to stop server", e);
                    }
                    threadsCompletedLatch.countDown();
                }
            });
            
        }
        
        for (Exception e : runnablesGroup.executeParallel()) {
            if (e != null) {
                throw e;
            }
        }
        threadsCompletedLatch.await();
    }

}
