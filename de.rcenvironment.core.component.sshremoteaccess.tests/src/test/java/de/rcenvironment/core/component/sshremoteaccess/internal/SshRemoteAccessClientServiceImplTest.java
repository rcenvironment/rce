/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess.internal;

import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.communication.api.LogicalNodeManagementService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.sshconnection.SshConnectionService;
import de.rcenvironment.core.communication.testutils.PlatformServiceDefaultStub;
import de.rcenvironment.core.component.management.api.LocalComponentRegistrationService;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.ssh.jsch.JschSessionFactory;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;

/**
 * Tests for SSH Remote Access Service.
 * 
 * @author Brigitte Boden
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class SshRemoteAccessClientServiceImplTest {

    protected static final Object GROUP_NAME_TOOLS = "groupForTools";

    protected static final Object GROUP_NAME_WFS = "groupForWfs";

    private SshServer sshServer;

    private SshConnectionService connectionService;

    private SshRemoteAccessClientServiceImpl remoteAccessService;

    private final InstanceNodeSessionId dummyNodeId = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("dummy");

    private LocalComponentRegistrationService mockRegistry;

    /**
     * Set up a dummy ssh server to connect to and setup mock connection service.
     * 
     * @throws IOException on unexpected error
     * @throws SshParameterException on SSH error
     * @throws JSchException on SSH error
     **/
    @SuppressWarnings("serial")
    @Before
    public void setup() throws IOException, JSchException, SshParameterException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(SshRemoteAccessClientTestConstants.PORT);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setUserAuthFactories(new ArrayList<UserAuthFactory>() {

            {
                add(new UserAuthPasswordFactory());
            }
        });
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {

            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return (username.equals(SshRemoteAccessClientTestConstants.USER) && password
                    .equals(SshRemoteAccessClientTestConstants.PASSWORD));
            }
        });
        // Command factory that returns the correct version for the command "ra protocol-version"
        sshServer.setCommandFactory(new CommandFactory() {

            @Override
            public Command createCommand(ChannelSession channelSession, String commandString) {
                if (commandString.equals("ra list-tools") || commandString.equals("ra list-wfs")) {

                    final String stdout;
                    if (commandString.equals("ra list-tools")) {
                        CSVFormat csvFormat = CSVFormat.newFormat(' ').withQuote('"').withQuoteMode(QuoteMode.ALL);
                        stdout = csvFormat.format(SshRemoteAccessClientTestConstants.TOOL_NAME,
                            SshRemoteAccessClientTestConstants.TOOL_VERSION, SshRemoteAccessClientTestConstants.HOST_ID,
                            SshRemoteAccessClientTestConstants.HOST_NAME, "", "", GROUP_NAME_TOOLS, "");
                    } else {
                        stdout =
                            "1\n4\n" + SshRemoteAccessClientTestConstants.WF_NAME + "\n" + SshRemoteAccessClientTestConstants.WF_VERSION
                                + "\n" + SshRemoteAccessClientTestConstants.HOST_NAME + "\n" + SshRemoteAccessClientTestConstants.HOST_ID;
                    }
                    return new Command() {

                        /** Test constant. */
                        public static final String EMPTY_STRING = "";

                        protected ExitCallback exitCallback;

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
                        public void start(ChannelSession channelSession, Environment env) throws IOException {

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
                            exitCallback.onExit(exitValue);
                        }

                        @Override
                        public void destroy(ChannelSession channelSession) {}

                    };
                } else {
                    throw new IllegalArgumentException("Unknown command: " + commandString);
                }
            }
        });
        sshServer.start();

        Session session =
            JschSessionFactory.setupSession(SshRemoteAccessClientTestConstants.LOCALHOST, SshRemoteAccessClientTestConstants.PORT,
                SshRemoteAccessClientTestConstants.USER, null, SshRemoteAccessClientTestConstants.PASSWORD, null);
        connectionService = new MockSshConnectionService(session);
        remoteAccessService = new SshRemoteAccessClientServiceImpl();

        remoteAccessService.bindSSHConnectionService(connectionService);
        remoteAccessService.bindPlatformService(new PlatformServiceDefaultStub() {

            @Override
            public InstanceNodeSessionId getLocalInstanceNodeSessionId() {
                return dummyNodeId;
            }
        });

        mockRegistry = EasyMock.createNiceMock(LocalComponentRegistrationService.class);
        remoteAccessService.bindComponentRegistry(mockRegistry);

        LogicalNodeManagementService logicalNodeService = EasyMock.createNiceMock(LogicalNodeManagementService.class);
        remoteAccessService.bindLogicalNodeManagementService(logicalNodeService);
    }

    /**
     * Test updating ssh remote access components.
     * 
     * @throws OperationFailureException if registering the component failed for some reason
     * 
     */
    @Test(timeout = SshRemoteAccessClientTestConstants.TIMEOUT)
    public void testUpdatingRemoteAccessComponents() throws OperationFailureException {
        EasyMock.reset(mockRegistry);
        mockRegistry.registerOrUpdateSingleVersionLocalComponentInstallation(notNull(ComponentInstallation.class),
            notNull(AuthorizationPermissionSet.class));
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {

            @Override
            public Void answer() throws Throwable {
                ComponentInstallation ci = (ComponentInstallation) EasyMock.getCurrentArguments()[0];
                assertNotNull(ci);
                assertEquals(SshRemoteAccessClientTestConstants.TOOL_VERSION, ci.getComponentInterface()
                    .getVersion());
                assertEquals(GROUP_NAME_TOOLS, ci.getComponentInterface()
                    .getGroupName());
                return null;
            }
        });
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {

            @Override
            public Void answer() throws Throwable {
                ComponentInstallation ci = (ComponentInstallation) EasyMock.getCurrentArguments()[0];
                assertNotNull(ci);
                assertEquals(SshRemoteAccessClientTestConstants.WF_VERSION, ci.getComponentInterface()
                    .getVersion());
                assertEquals(GROUP_NAME_WFS, ci.getComponentInterface()
                    .getGroupName());
                return null;
            }
        });
        EasyMock.replay(mockRegistry);
        remoteAccessService.updateSshRemoteAccessComponents();
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
