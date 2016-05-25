/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.sshconnection.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.sshconnection.SshConnectionConstants;
import de.rcenvironment.core.communication.sshconnection.SshConnectionContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link SshConnectionServiceImpl}.
 *
 * @author Brigitte Boden
 */
public class SshConnectionServiceImplTests {

    private static final int PORT = 31007;

    private static final String LOCALHOST = "localhost";

    private static final String DISPLAYNAME = "example connection";

    private static final String DISPLAYNAME2 = "example connection2";

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    private static final int TIMEOUT = 30000;

    private SshServer sshServer;

    private SshConnectionServiceImpl sshConnectionService;

    /**
     * Set up a dummy ssh server to connect to.
     * 
     * @throws IOException on unexpected error
     **/
    @SuppressWarnings("serial")
    @Before
    public void setUp() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(PORT);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<NamedFactory<UserAuth>>() {

            {
                add(new UserAuthPassword.Factory());
            }
        });
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {

            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return (username.equals(USER) && password.equals(PASSWORD));
            }
        });
        // Command factory that returns the correct version for the command "ra protocol-version"
        sshServer.setCommandFactory(new CommandFactory() {

            @Override
            public Command createCommand(String commandString) {
                if (commandString.equals("ra protocol-version")) {
                    return new Command() {

                        /** Test constant. */
                        public static final String EMPTY_STRING = "";

                        protected ExitCallback exitCallback;

                        private String stdout = SshConnectionConstants.REQUIRED_PROTOCOL_VERSION;

                        private String stderr;

                        private int exitValue;

                        private OutputStream stdoutStream;

                        private OutputStream stderrStream;

                        @Override
                        public void setInputStream(InputStream in) {}

                        @Override
                        public void setOutputStream(OutputStream out) {
                            this.stdoutStream = out;
                        }

                        @Override
                        public void setErrorStream(OutputStream err) {
                            this.stderrStream = err;
                        }

                        @Override
                        public void setExitCallback(ExitCallback callback) {
                            this.exitCallback = callback;
                        }

                        @Override
                        public void start(Environment env) throws IOException {

                            if (stdout != null) {
                                stdoutStream.write(stdout.getBytes());
                            } else {
                                stdoutStream.write(EMPTY_STRING.getBytes());
                            }
                            if (stderr != null) {
                                stderrStream.write(stderr.getBytes());
                            } else {
                                stderrStream.write(EMPTY_STRING.getBytes());
                            }
                            stdoutStream.flush();
                            stderrStream.flush();
                            IOUtils.closeQuietly(stdoutStream);
                            IOUtils.closeQuietly(stderrStream);
                            exitCallback.onExit(exitValue);
                        }

                        @Override
                        public void destroy() {}

                    };
                } else {
                    throw new IllegalArgumentException("Unknown command: " + commandString);
                }
            }
        });
        sshServer.start();
    }

    /**
     * Initialize the connection service.
     * 
     */
    @Before
    public void initSshConnectionService() {
        sshConnectionService = new SshConnectionServiceImpl();
    }

    /**
     * Test adding, editing, connecting and disconnecting an SSH connection. Does not test storing a password because we can't access the
     * secure store here.
     * 
     */
    @Test(timeout = TIMEOUT)
    public void testHandlingSshConnection() {
        // Add a connection
        String connectionId =
            sshConnectionService.addSshConnection(DISPLAYNAME, LOCALHOST, PORT, USER, null, true, false);
        assertEquals(0, sshConnectionService.getAllActiveSshConnectionSetups().size());
        assertEquals(1, sshConnectionService.getAllSshConnectionSetups().size());
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getId(), connectionId);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getDisplayName(), DISPLAYNAME);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getUsername(), USER);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getHost(), LOCALHOST);
        assertNull(sshConnectionService.getAvtiveSshSession(connectionId));

        // Edit the connection
        sshConnectionService.editSshConnection(new SshConnectionContext(connectionId, DISPLAYNAME2, LOCALHOST, PORT, USER, null, true,
            false));
        assertEquals(0, sshConnectionService.getAllActiveSshConnectionSetups().size());
        assertEquals(1, sshConnectionService.getAllSshConnectionSetups().size());
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getId(), connectionId);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getDisplayName(), DISPLAYNAME2);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getUsername(), USER);
        assertEquals(sshConnectionService.getConnectionSetup(connectionId).getHost(), LOCALHOST);
        assertNull(sshConnectionService.getAvtiveSshSession(connectionId));

        // Connect
        sshConnectionService.connectSession(connectionId, PASSWORD);
        assertNotNull(sshConnectionService.getAvtiveSshSession(connectionId));
        assertTrue(sshConnectionService.isConnected(connectionId));

        // Disconnect
        sshConnectionService.disconnectSession(connectionId);
        assertNull(sshConnectionService.getAvtiveSshSession(connectionId));
        assertFalse(sshConnectionService.isConnected(connectionId));
    }

    /**
     * Stop ssh server.
     * 
     * @throws InterruptedException on error when stopping the server
     * @throws IOException on unexpected error
     **/
    @After
    public void tearDown() throws InterruptedException, IOException {
        sshServer.stop();
    }
}
