/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.ssh.jsch.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.ssh.jsch.DummyCommand;
import de.rcenvironment.core.utils.ssh.jsch.DummyPasswordAuthenticator;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.core.utils.ssh.jsch.Utils;

/**
 * Test case for {@link JSchCommandLineExecutor}.
 * 
 * @author Doreen Seider
 */
public class JSchCommandLineExecutorParallelTest {

    private static final int WAIT_FOR_THREADS_MSEC = 15000;

    private static final int TIMEOUT = 20000;

    private static final String TESTING_CONNECTION_FAILED = "Testing connection failed:\n";

    private static final String LOCALHOST = "localhost";

    private File localWorkdir;

    private File remoteWorkdir;

    /**
     * Set up test environment.
     * 
     * @throws IOException on error
     **/
    @Before
    public void setUp() throws IOException {
        TempFileServiceAccess.setupUnitTestEnvironment();
    }

    /**
     * Set up work dir.
     * 
     * @throws IOException on error
     **/
    @Before
    public void createWorkDir() throws IOException {
        remoteWorkdir = new File(DummyCommand.WORKDIR_REMOTE);
        remoteWorkdir.mkdir();
        localWorkdir = new File(DummyCommand.WORKDIR_LOCAL);
        localWorkdir.mkdir();
    }

    /**
     * Delete work dir.
     * 
     * @throws IOException on error
     **/
    @After
    public void deleteWorkDir() throws IOException {
        FileUtils.deleteQuietly(localWorkdir);
        FileUtils.deleteQuietly(remoteWorkdir);
    }

    /**
     * Tests parallel running of several SSH servers (as could happen i.e. on Jenkins)
     * 
     * @author Brigitte Boden
     * 
     * @throws IOException on error
     * @throws JSchException on error
     * @throws SshParameterException on error
     * @throws InterruptedException on error
     */
    @SuppressWarnings("serial")
    @Test(timeout = TIMEOUT)
    public void testParallelServers() throws JSchException, SshParameterException, IOException, InterruptedException {

        final int numServers = 10;

        final List<String> threadsCompleted = Collections.synchronizedList(new ArrayList<String>());

        for (int i = 0; i < numServers; i++) {
            final int threadid = i;

            SharedThreadPool.getInstance().execute(new Runnable() {

                @Override
                @TaskDescription(value = "Running SSH server")
                public void run() {
                    SshServer sshServer;
                    int port;
                    //Starting the SSH server will fail if the port is already in use, in that case it will be retried with another port.
                    boolean retry = true;
                    do {
                        port = Utils.getRandomPortNumber();
                        sshServer = SshServer.setUpDefaultServer();
                        sshServer.setPort(port);
                        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
                        sshServer.setUserAuthFactories(new ArrayList<NamedFactory<UserAuth>>() {

                            {
                                add(new UserAuthPassword.Factory());
                            }
                        });
                        sshServer.setPasswordAuthenticator(new DummyPasswordAuthenticator());
                        try {
                            sshServer.start();
                            retry = false;
                        } catch (IOException e) {
                            fail("Starting SSH server failed:\n" + e);
                            retry = true;
                        }
                    } while (retry);
                     
                    
                    // Establish a connections and est downloading work dir
                    final String fileContent = RandomStringUtils.randomAlphabetic(6);

                    try {
                        Session session = JschSessionFactory.setupSession(LOCALHOST, port, DummyPasswordAuthenticator.USERNAME,
                            null, DummyPasswordAuthenticator.PASSWORD, null);
                        JSchCommandLineExecutor executor = new JSchCommandLineExecutor(session, DummyCommand.WORKDIR_REMOTE);

                        Utils.createFileOnServerSidesWorkDir(sshServer, session, RandomStringUtils.randomAlphabetic(6),
                            fileContent,
                            executor);

                        sshServer.setCommandFactory(new ScpCommandFactory());

                        File dir = TempFileServiceAccess.getInstance().createManagedTempDir();
                        executor.downloadWorkdir(dir);
                        assertEquals(1, dir.listFiles().length);

                        TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(dir);
                    } catch (JSchException e) {
                        fail(TESTING_CONNECTION_FAILED + e);
                    } catch (SshParameterException e) {
                        fail(TESTING_CONNECTION_FAILED + e);
                    } catch (IOException e) {
                        fail(TESTING_CONNECTION_FAILED + e);
                    } catch (InterruptedException e) {
                        fail(TESTING_CONNECTION_FAILED + e);
                    }

                    try {
                        sshServer.stop();
                    } catch (InterruptedException e) {
                        fail("Stopping SSH server failed:\n" + e);
                    }
                    threadsCompleted.add("Server" + threadid);
                }
            });
        }
        Thread.sleep(WAIT_FOR_THREADS_MSEC);
        
        //Assert that all threads are completed
        assertEquals(numServers, threadsCompleted.size());
    }

}
