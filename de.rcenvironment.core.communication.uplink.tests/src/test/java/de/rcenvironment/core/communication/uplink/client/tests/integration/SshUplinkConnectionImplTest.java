/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.tests.integration;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;

import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.communication.uplink.client.session.impl.SshUplinkConnectionImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkEndpointServiceImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionServiceImpl;
import de.rcenvironment.core.embedded.ssh.internal.EmbeddedSshServerImpl;
import de.rcenvironment.core.embedded.ssh.internal.SshAccountImpl;
import de.rcenvironment.core.embedded.ssh.internal.SshConfiguration;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;

/**
 * An integration test for {@link SshUplinkConnectionImpl} and its corresponding server-side code in {@link EmbeddedSshServerImpl}.
 * <p>
 * FIXME: temporarily exported de.rcenvironment.core.embedded.ssh.internal to give these tests access; this should be
 * refactored/encapsulated.
 *
 * @author Robert Mischke
 */
public class SshUplinkConnectionImplTest extends AbstractUplinkConnectionTest {

    private static final int TEST_SSH_IDLE_TIMEOUT = 10;

    private static final int TEST_SSH_PORT = 40500;

    private static final String TEST_USER_ACCOUNT = "test";

    private static final String TEST_USER_PW = "testpw";

    private static final String TEST_USER_ROLE = "remote_access_user"; // TODO consider creating a separate Uplink role

    private EmbeddedSshServerImpl testServer;

    private Session sshSession;

    private Logger delegateLogger;

    /**
     * Common test setup.
     * 
     * @throws Exception on failure
     */
    @Before
    public void setUp() throws Exception {
        TempFileServiceAccess.setupUnitTestEnvironment();
        testServer = new EmbeddedSshServerImpl();
        SshConfiguration configuration = new SshConfiguration();
        configuration.setEnabled(true);
        configuration.setPort(TEST_SSH_PORT); // TODO make dynamic
        configuration.setIdleTimeoutSeconds(TEST_SSH_IDLE_TIMEOUT);
        configuration.getAccounts().add(new SshAccountImpl(TEST_USER_ACCOUNT, TEST_USER_PW, null, null, TEST_USER_ROLE));

        // TODO refactor; move into base class
        ServerSideUplinkSessionServiceImpl mockServerSideUplinkSessionService = new ServerSideUplinkSessionServiceImpl();
        mockServerSideUplinkSessionService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());

        final ServerSideUplinkEndpointServiceImpl mockServerSideUplinkEndpointService = new ServerSideUplinkEndpointServiceImpl();
        mockServerSideUplinkEndpointService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());
        mockServerSideUplinkSessionService.bindServerSideUplinkEndpointService(mockServerSideUplinkEndpointService);

        testServer.mockActivateAndStart(configuration, TempFileServiceAccess.getInstance().createManagedTempDir(),
            mockServerSideUplinkSessionService);
        assertTrue("The SSH test server could not be started; most likely, there is another instance using the test port",
            testServer.isRunning());
        delegateLogger = null; // JschSessionFactory.createDelegateLogger(log);

        // connect to SSH server
        sshSession = JschSessionFactory.setupSession("localhost", TEST_SSH_PORT, TEST_USER_ACCOUNT, null, TEST_USER_PW, delegateLogger);

        // wrap into UplinkConnection
        uplinkConnection = new SshUplinkConnectionImpl(sshSession);
    }

    /**
     * Common test tear-down.
     * 
     * @throws Exception on failure
     */
    @After
    public void tearDown() {
        if (sshSession != null) {
            sshSession.disconnect();
        }
        testServer.deactivate();

    }

}
