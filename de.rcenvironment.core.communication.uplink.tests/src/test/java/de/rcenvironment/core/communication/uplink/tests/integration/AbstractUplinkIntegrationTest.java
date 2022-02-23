/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.tests.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryDownloadReceiver;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadContext;
import de.rcenvironment.core.communication.uplink.client.execution.api.DirectoryUploadProvider;
import de.rcenvironment.core.communication.uplink.client.execution.api.FileDataSource;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionClientSideSetup;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionEventHandler;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionRequest;
import de.rcenvironment.core.communication.uplink.client.execution.api.ToolExecutionResult;
import de.rcenvironment.core.communication.uplink.client.session.api.ClientSideUplinkSession;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptor;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolDescriptorListUpdate;
import de.rcenvironment.core.communication.uplink.client.session.api.ToolExecutionHandle;
import de.rcenvironment.core.communication.uplink.client.session.api.UplinkConnection;
import de.rcenvironment.core.communication.uplink.client.session.internal.ClientSideUplinkSessionParameters;
import de.rcenvironment.core.communication.uplink.client.session.internal.LocalUplinkSessionServiceImpl;
import de.rcenvironment.core.communication.uplink.common.internal.UplinkProtocolMessageConverter;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.communication.uplink.network.internal.ServerSideUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConfiguration;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConfiguration.Builder;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkEndpointServiceImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionServiceImpl;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.component.management.utils.JsonDataWithOptionalEncryption;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.testutils.SimpleThroughputLimiter;
import de.rcenvironment.core.utils.common.testutils.ThroughputLimiter;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Integration tests for the call chain through the layers of the "uplink" mechanism.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractUplinkIntegrationTest {

    private static final String DUMMY_NAME = "dummyName";

    private static final String SESSION_QUALIFIER_CLIENT1 = "client1";

    private static final String SESSION_QUALIFIER_CLIENT2 = "client2";

    private static final String SESSION_QUALIFIER_CLIENT3 = "client3";

    private static final byte SOME_PRIME_NUMBER = 31;

    private static final String TEST_GROUP_A_ID = "groupA";

    private static final String TEST_TOOL_1_VERSION = "1.0";

    private static final String TEST_TOOL_1_ID = "tool1";

    private static final String TEST_INPUT_FILE_PATH = "hello.txt";

    private static final String TEST_EMPTY_INPUT_FILE_PATH = "empty.test";

    private static final String TEST_INPUT_FILE_CONTENT = "helloContent";

    private static final byte[] TEST_INPUT_FILE_CONTENT_BYTES = TEST_INPUT_FILE_CONTENT.getBytes(Charset.defaultCharset());

    private static final String TEST_OUTPUT_FILE_PATH = "results/output.txt";

    private static final byte[] TEST_OUTPUT_FILE_CONTENT_BYTES = TEST_INPUT_FILE_CONTENT.getBytes(Charset.defaultCharset());

    private static final String DOCUMENTATION_ID_EXISTING = "existingDoc";

    private static final String DOCUMENTATION_ID_LARGER_THAN_MESSAGE_BLOCK = "largeDoc";

    private static final int DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK = 1000000;

    // TODO investigate why for some tests, shorter times (e.g. 200 msec) are not enough for reliably completing
    // a handshake exchange and session activation; not a real problem, just curious
    private static final int SHORT_TIME = 500; // msec to wait for lightweight asynchronous operations to complete

    private static final int STATE_WAITING_CHECK_INTERVAL = 50;

    private static final int STATE_WAITING_TIMEOUT = 2000; // fairly high for slow test environments; only used when necessary

    private static final int TEST_SIZE_OF_DEFAULT_MESSAGE_QUEUE = 50;

    protected final Log log = LogFactory.getLog(getClass());

    private final AsyncTaskService asyncTaskService = ConcurrencyUtils.getAsyncTaskService();

    private LocalUplinkSessionServiceImpl mockUplinkSessionService;

    private ServerSideUplinkEndpointServiceImpl mockServerSideUplinkEndpointService;

    private ServerSideUplinkSessionServiceImpl mockServerSideUplinkSessionService;

    private volatile UplinkTestContext testContext;

    private UplinkProtocolConfiguration configuration;

    /**
     * Holds the context of a single test run, e.g. established connections, and provides utility methods.
     *
     * @author Robert Mischke
     */
    private final class UplinkTestContext {

        private final List<ClientSideUplinkSession> clientSessions = new ArrayList<>();

        public synchronized ClientSideUplinkSession setUpSession(MockClientStateHolder clientMock, String sessionQualifier) {

            UplinkConnection uplinkConnection =
                setUpClientConnection(clientMock.getOutgoingThroughputLimiter(), clientMock.getIncomingThroughputLimiter());
            clientMock.setUplinkConnection(uplinkConnection); // store for test access

            try {
                uplinkConnection.open(errorMessage -> {
                    log.warn("Unhandled connection error message: " + errorMessage);
                });
            } catch (IOException e) {
                throw new AssertionError("Failed to setup mock connection", e); // should never happen
            }

            final ClientSideUplinkSessionParameters sessionParameters =
                new ClientSideUplinkSessionParameters("Test session", sessionQualifier, null, clientMock.getCustomHandshakeParameters());
            final ClientSideUplinkSession session =
                mockUplinkSessionService.createSession(uplinkConnection, sessionParameters, clientMock);

            clientSessions.add(session);
            return session;
        }

        public void startSession(final ClientSideUplinkSession session) {
            asyncTaskService.execute("Run mock client Uplink session", () -> {
                session.runSession();
            });
        }

        public ClientSideUplinkSession setUpAndStartSession(MockClientStateHolder clientMock, String sessionQualifier)
            throws OperationFailureException {
            final ClientSideUplinkSession session = setUpSession(clientMock, sessionQualifier);
            startSession(session);
            return session;
        }

        public synchronized void closeAllClientSessions() {
            for (ClientSideUplinkSession session : clientSessions) {
                try {
                    session.initiateCleanShutdownIfRunning();
                } catch (IllegalStateException e) {
                    log.warn("Session " + session.getLogDescriptor() + " was already closed");
                }
            }
        }

    }

    /**
     * Customizes Uplink configuration parameters to support testing, e.g. timeouts.
     */
    @BeforeClass
    public static void setUpConfiguration() {
        Builder builder = UplinkProtocolConfiguration.newBuilder();
        // define custom test timeouts
        builder.setHandshakeResponseTimeout(500);
        builder.setMaxBufferedMessagesForPriority(MessageBlockPriority.DEFAULT, TEST_SIZE_OF_DEFAULT_MESSAGE_QUEUE);
        // apply them
        UplinkProtocolConfiguration.override(builder); // affects all Uplink objects created afterwards
    }

    /**
     * Creates mock service implementations.
     */
    @Before
    public void setUpMockServices() {
        mockUplinkSessionService = new LocalUplinkSessionServiceImpl();
        mockUplinkSessionService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());

        mockServerSideUplinkEndpointService = new ServerSideUplinkEndpointServiceImpl();
        mockServerSideUplinkEndpointService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());

        mockServerSideUplinkSessionService = new ServerSideUplinkSessionServiceImpl();
        mockServerSideUplinkSessionService.bindServerSideUplinkEndpointService(mockServerSideUplinkEndpointService);
        mockServerSideUplinkSessionService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());
    }

    /**
     * Creates the {@link UplinkTestContext}.
     */
    @Before
    public void setUpContext() {
        // fetch and store the configuration object for convenient test access to values
        configuration = UplinkProtocolConfiguration.getCurrent();
        assertNotNull(configuration);

        testContext = new UplinkTestContext();
    }

    /**
     * Common cleanup.
     */
    @After
    public void cleanUp() {
        testContext.closeAllClientSessions();
    }

    /**
     * Performs a connect-disconnect cycle and verifies the session state.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void basicSessionLifeCycle() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpSession(client1, SESSION_QUALIFIER_CLIENT1);
        assertEquals(UplinkSessionState.INITIAL, clientSession1.getState());
        testContext.startSession(clientSession1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(client1.isSessionActive());
        assertTrue(clientSession1.isActive());
        assertNull(client1.getSessionErrorMessage());

        assertFalse(clientSession1.isShuttingDownOrShutDown());
        shutDownClientAndVerifyCleanShutdown(client1, clientSession1);
        assertTrue(clientSession1.isShuttingDownOrShutDown()); // should still be true
        assertNull(client1.getSessionErrorMessage()); // regular shutdown, no error
    }

    /**
     * Error handling test that simulates the server side refusing the connection attempt on the handshake level.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void errorHandlingOnServerRefusingSession() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        final Map<String, String> customHandshakeParameters = new HashMap<>();
        final String testMessage = "cr message";
        customHandshakeParameters.put(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_REFUSED_CONNECTION, testMessage);
        client1.setCustomHandshakeParameters(customHandshakeParameters);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        awaitStateOrFail(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1);
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());
        assertTrue(client1.getSessionErrorMessage().endsWith(testMessage)); // allow message prefixing
    }

    /**
     * Connects normally, then sends an invalid message, verifies that the local session has terminated, and then validates that a second
     * client can connect using the same namespace (login + clientId).
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void errorHandlingOnClientSendingInvalidMessageAfterHandshake() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpSession(client1, SESSION_QUALIFIER_CLIENT1);
        testContext.startSession(clientSession1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());

        client1.getUplinkConnection().getOutputStream()
            // arbitrary error content: regular header size of 13 bytes, but 0x09090909 (bytes 8-11) is an invalid message size
            .write(new byte[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 });
        client1.getUplinkConnection().getOutputStream().flush();

        waitFor(SHORT_TIME); // wait for asynchronous execution
        // assertEquals(UplinkSessionState.UNCLEAN_SHUTDOWN, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertTrue(clientSession1.isShuttingDownOrShutDown()); // should still be true
        assertNotNull(client1.getSessionErrorMessage());
        assertTrue(client1.getSessionErrorMessage(), client1.getSessionErrorMessage().contains("E#"));
        // TODO (p2) >10.2: consider validating that the error should indicate a protocol violation; currently not the case

        // validate that a second client can connect using the previously held namespace (login+clientId)
        MockClientStateHolder client2 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession2 = testContext.setUpSession(client2, SESSION_QUALIFIER_CLIENT1);
        testContext.startSession(clientSession2);
        client2.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
    }

    /**
     * Verifies that the server does not allow concurrent logins with the same account and session qualifier/"client id" combination, which
     * would result in a collisions from both clients using the same destination id namespace (which is deterministically derived from these
     * parameters).
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void serverSideHandlingOfNamespaceCollisions() throws Exception {
        int numClients = 5; // let this many clients connect concurrently to find potential race conditions
        runIterationOfServerSideHandlingOfNamespaceCollisions(numClients);
        // run this again to check if the namespace was released properly and could be used again by the next session
        runIterationOfServerSideHandlingOfNamespaceCollisions(numClients);
    }

    private void runIterationOfServerSideHandlingOfNamespaceCollisions(int numClients)
        throws OperationFailureException, InterruptedException, TimeoutException {
        MockClientStateHolder[] clients = new MockClientStateHolder[numClients];
        for (int i = 0; i < numClients; i++) {
            // handler = null because there is no need to handle tool execution requests here
            clients[i] = new MockClientStateHolder(null);
            // note: using the local virtual connection which always uses the same login name
            testContext.setUpAndStartSession(clients[i], "sharedQ");
        }
        waitFor(SHORT_TIME);

        int numActive = 0;
        int numRefused = 0;
        for (int i = 0; i < numClients; i++) {
            if (clients[i].isSessionActive()) {
                numActive++;
            }
            if (clients[i].isSessionInTerminalState()) {
                numRefused++;
            }
        }

        assertEquals(1, numActive);
        assertEquals(numClients - 1, numRefused);

        testContext.closeAllClientSessions();
        waitFor(SHORT_TIME); // wait for asynchronous execution

        for (int i = 0; i < numClients; i++) {
            assertFalse(clients[i].isSessionActive());
            assertTrue(clients[i].isSessionInTerminalState());
        }
    }

    /**
     * Error handling test that simulates the server side closing the connection due to a handshake error (e.g. protocol version mismatch).
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void errorHandlingOnServerHandshakeFailure() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        final Map<String, String> customHandshakeParameters = new HashMap<>();
        final String testMessage = "failure test message";
        customHandshakeParameters.put(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE, testMessage);
        client1.setCustomHandshakeParameters(customHandshakeParameters);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        awaitStateOrFail(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1);
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());

        // only test presence to allow message prefixing/suffixing
        assertTrue(client1.getSessionErrorMessage(),
            client1.getSessionErrorMessage().contains(ServerSideUplinkLowLevelProtocolWrapper.ERROR_MESSAGE_CONNECTION_SETUP_FAILED));

        // wait for the handshake data timeout to verify that no redundant "handshake timeout" error is reported
        Thread.sleep(configuration.getHandshakeResponseTimeout() + SHORT_TIME);
        assertFalse(client1.getInconsistentStateFlag()); // set true on duplicate callbacks
    }

    /**
     * Error handling test that simulates the server side failing to send a handshake response in time while keeping the connection itself
     * open. The client should react to this by closing the connection due to timeout, and closing the stream. This, in turn, should cause
     * the server-side session to be closed if it wasn't already. As the final result, the server-side session should not hold a lock on the
     * namespace id (login name + client id), either because it was never assigned in the first place, or because it has been released.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void errorHandlingOnServerHandshakeResponseTimeout() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        final Map<String, String> customHandshakeParameters = new HashMap<>();
        customHandshakeParameters.put(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_RESPONSE_DELAY_ABOVE_TIMEOUT, null);
        client1.setCustomHandshakeParameters(customHandshakeParameters);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        // the client-side code should return after the time given by the constant, so wait twice that time to avoid test timeouts
        client1.waitForSessionInitCompletion(
            configuration.getHandshakeResponseTimeout() + configuration.getHandshakeResponseTimeout());
        assertEquals(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());
        assertTrue(client1.getSessionErrorMessage(), client1.getSessionErrorMessage().contains("timeout"));
        // verify that no redundant "handshake timeout" errors were reported
        assertFalse(client1.getInconsistentStateFlag()); // set true on duplicate callbacks

        // give the server time to detect that the client did not confirm the handshake exchange
        waitFor(configuration.getHandshakeResponseTimeout() + configuration.getHandshakeResponseTimeout());

        // verify that another client can connect using the same qualifier (which fails if the first session was not properly cleaned up)
        MockClientStateHolder client1b = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1b = testContext.setUpAndStartSession(client1b, SESSION_QUALIFIER_CLIENT1);
        client1b.waitForSessionInitCompletion(
            configuration.getHandshakeResponseTimeout());
        assertEquals(UplinkSessionState.ACTIVE, clientSession1b.getState());
        assertTrue(clientSession1b.isActive());
        assertNull(client1b.getSessionErrorMessage());

        // wait a little, and verify that the failing session has not asynchronously changed state again (regression check)
        waitFor(SHORT_TIME);
        assertEquals(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1.getState());
        assertFalse(clientSession1.isActive());
    }

    /**
     * Error handling test that simulates the client side unexpectedly closing the data stream. Note that when testing with a simulated
     * connection, this can only test a part of the actual live error behavior. For example, certain exceptions being thrown by the
     * low-level network layer may cause different errors.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void errorHandlingOnClientEOFDuringActiveSession() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        final Map<String, String> customHandshakeParameters = new HashMap<>();
        client1.setCustomHandshakeParameters(customHandshakeParameters);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        awaitStateOrFail(UplinkSessionState.ACTIVE, clientSession1);
        assertTrue(client1.isSessionActive());
        assertTrue(clientSession1.isActive());

        final String expectedNamespaceId = "test----client1-";
        assertTrue(mockServerSideUplinkEndpointService.isNamespaceAssigned(expectedNamespaceId));

        simulateClientSideEOF(client1.getUplinkConnection());

        waitFor(SHORT_TIME);

        assertFalse(mockServerSideUplinkEndpointService.isNamespaceAssigned(expectedNamespaceId));
    }

    /**
     * Tests a scenario where a very slow client connects, and other clients perform normal operations (in this case, sending tool
     * descriptor updates and connecting/disconnecting) concurrently. Serves as a regression test for issue #17649, where the combination of
     * the slow client's backpressure and concurrent operations caused a deadlock involving the outgoing message queue reaching its limit
     * and session activation/deactivation.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void behaviorOnStalledOrExtremelySlowClient() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(clientSession1.isActive());

        // create client 2 with throughput limiting (i.e. a "slow" client)
        MockClientStateHolder client2 = new MockClientStateHolder(null);
        // install a limiter that waits for 100 msec every 50 bytes; reduces client 2's wire speed to approximately 0,5 kb/s
        SimpleThroughputLimiter client2OutgoingThroughputLimiter = new SimpleThroughputLimiter(50, 100);
        client2OutgoingThroughputLimiter.enableVerboseLogging("[Client 2 -> Server] ");
        SimpleThroughputLimiter client2IncomingThroughputLimiter = new SimpleThroughputLimiter(50, 100);
        client2IncomingThroughputLimiter.enableVerboseLogging("[Server -> Client 2] ");
        client2.setThroughputLimiters(client2OutgoingThroughputLimiter, client2IncomingThroughputLimiter);

        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client2.waitForSessionInitCompletion(SHORT_TIME + SHORT_TIME); // give more leeway to slow client
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue(clientSession2.isActive());

        // After client1 (normal) and client2 (slow) are connected, start a loop of client1 constantly sending tool updates and another
        // client connecting/disconnecting concurrently. This replicates the scenario in which issue #0017649 occurred.

        AtomicBoolean shutdownFlag = new AtomicBoolean(false);
        AtomicInteger errorCounter = new AtomicInteger(0);
        final CountDownLatch workerThreadsTerminatedLatch = new CountDownLatch(2);

        // create a mock update list containing a single tool; as these a simply forwarded for now, reusing it does not matter
        ArrayList<ToolDescriptor> mockToolList = new ArrayList<ToolDescriptor>();
        mockToolList.add(new ToolDescriptor("abc", "1", new HashSet<String>(), null,
            new JsonDataWithOptionalEncryption(StringUtils.repeat("xyz", 1000), new HashMap<>()))); // ~3kb payload

        asyncTaskService.execute("Test worker: Send mock tool updates", () -> {
            int dummySourceIndex = 0; // spread the tool descriptors across a few "virtual" nodes; not strictly related, but can't hurt
            while (!shutdownFlag.get()) {
                dummySourceIndex = (dummySourceIndex + 1) % 10;
                try {
                    clientSession1.publishToolDescriptorListUpdate(new ToolDescriptorListUpdate(
                        clientSession1.getDestinationIdPrefix() + "MockUpdateSource" + dummySourceIndex,
                        "MockUpdateSource" + dummySourceIndex,
                        mockToolList));
                    Thread.sleep(1);
                } catch (IOException | InterruptedException e) {
                    errorCounter.incrementAndGet();
                    log.error("Unexpected error sending mock tool update from client 1", e);
                }
            }
            shutDownClientAndVerifyCleanShutdown(client1, clientSession1);
            workerThreadsTerminatedLatch.countDown();
        });
        asyncTaskService.execute("Test worker: Perform repeated connect/disconnect", () -> {
            while (!shutdownFlag.get()) {
                try {
                    MockClientStateHolder client3 = new MockClientStateHolder(null);
                    ClientSideUplinkSession clientSession3 = testContext.setUpAndStartSession(client3, SESSION_QUALIFIER_CLIENT3);
                    client3.waitForSessionInitCompletion(SHORT_TIME + SHORT_TIME); // high load situation -> give more leeway
                    assertEquals(UplinkSessionState.ACTIVE, clientSession3.getState());
                    assertTrue(clientSession3.isActive());

                    shutDownClientAndVerifyCleanShutdown(client3, clientSession3);
                } catch (OperationFailureException | InterruptedException | TimeoutException | AssertionError e) {
                    errorCounter.incrementAndGet();
                    log.error("Error during connect/disconnect loop", e);
                }
            }
            workerThreadsTerminatedLatch.countDown();
        });

        waitFor(10000); // some base running time to allow errors to occur; adjust if needed for different test platforms (e.g. CI)

        shutdownFlag.set(true);

        // print statistics to allow verification of client 2's throughput limiting
        log.debug("Client 2 -> Server throughput statistics: " + client2OutgoingThroughputLimiter.getStatisticsLine());
        log.debug("Server -> Client 2 throughput statistics: " + client2IncomingThroughputLimiter.getStatisticsLine());
        workerThreadsTerminatedLatch.await(10, TimeUnit.SECONDS);
        assertEquals("Encountered " + errorCounter.get() + " error(s) in worker threads; check log output for details", 0,
            errorCounter.get());

        shutDownClientAndVerifyCleanShutdown(client1, clientSession1);

        // If there were no errors, verify that client 2 is still active, i.e. not terminated due to excessive backpressure from its
        // artificial slowness. If it was, consider increasing the test queue size and/or the throughput limiting.
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue(clientSession2.isActive());

        // best-effort attempt to shut down normally, mostly to observe the log output; make no testable assumptions
        clientSession2.initiateCleanShutdownIfRunning();
        waitFor(SHORT_TIME);
    }

    /**
     * Tests the call chain for publishing and receiving component list updates.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void toolListPublishing() throws Exception {
        // handler = null because there is no need to handle tool execution requests here
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(clientSession1.isActive());

        MockClientStateHolder client2 = new MockClientStateHolder(null);
        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client2.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue(clientSession2.isActive());

        // verify initial state
        assertNotNull(client1.getKnownComponentsByDestinationId());
        assertEquals(0, client1.getKnownComponentsByDestinationId().size());
        assertNotNull(client2.getKnownComponentsByDestinationId());
        assertEquals(0, client2.getKnownComponentsByDestinationId().size());

        // client 1 sends an empty update
        final String client1DefaultDestinationId = client1.getAssignedDestinationIdPrefix();

        // Briefly wait, as client2 receiving its session information does not mean the server has already fully activated the session yet.
        // For non-empty updates, this would not happen (and is not the client's responsibly to wait for!), but here we are actively
        // testing the forwarding of empty updates. If client2's session activates after this empty update, it will not receive the update,
        // as empty tool list are not cached (by design), and therefore not sent to newly activating sessions; see #17410. -- misc_ro
        waitFor(SHORT_TIME);
        // TODO (p3) alternatively, add a mechanism to check/wait for server-side session activation to avoid waiting

        // publish an EMPTY descriptor list from client 1
        clientSession1
            .publishToolDescriptorListUpdate(new ToolDescriptorListUpdate(client1DefaultDestinationId, DUMMY_NAME, new ArrayList<>()));

        waitFor(SHORT_TIME); // wait for asynchronous execution

        // only clients in other networks should see the update
        assertThat("c1 visible tool lists = 0", client1.getKnownComponentsByDestinationId().size(), is(0));
        assertThat("c1->c1 tool list = null", client1.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), nullValue());
        assertThat("c2 visible tool lists = 1", client2.getKnownComponentsByDestinationId().size(), is(1));
        assertThat("c2->c1 tool list != null", client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId),
            notNullValue());
        assertThat("c2->c1 tool list empty", client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(0));

        Set<String> groupSet = new HashSet<String>();
        groupSet.add(TEST_GROUP_A_ID);

        // publish an single-entry descriptor list from client 1
        ToolDescriptor mockDescriptor1 = new ToolDescriptor(TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, groupSet, null, null);
        clientSession1
            .publishToolDescriptorListUpdate(
                new ToolDescriptorListUpdate(client1DefaultDestinationId, DUMMY_NAME, Arrays.asList(mockDescriptor1)));

        waitFor(SHORT_TIME); // wait for asynchronous execution

        // only clients in other networks should see the update
        assertThat("c1 visible tool lists = 0", client1.getKnownComponentsByDestinationId().size(), is(0));
        assertThat("c1->c1 tool list = null", client1.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), nullValue());
        assertThat("c2 visible tool lists = 1", client2.getKnownComponentsByDestinationId().size(), is(1));
        assertThat("c2->c1 tool list: 1 entry", client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
        assertThat("c2->c1 tool list: correct tool id",
            client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));

        // connect another client to check that it receives the last cached updates
        MockClientStateHolder client3 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession3 = testContext.setUpAndStartSession(client3, "client3"); // as above for client2
        client3.waitForSessionInitCompletion(SHORT_TIME);
        final String client3DefaultDestinationId = client3.getAssignedDestinationIdPrefix();

        // publish an single-entry descriptor list from client 3
        ToolDescriptor mockDescriptor3 = new ToolDescriptor(TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, groupSet, null, null);
        clientSession3
            .publishToolDescriptorListUpdate(
                new ToolDescriptorListUpdate(client3DefaultDestinationId, DUMMY_NAME, Arrays.asList(mockDescriptor3)));

        waitFor(SHORT_TIME); // wait for asynchronous execution

        // client 1 should see client 3's descriptor
        assertThat(client1.getKnownComponentsByDestinationId().size(), is(1));
        assertThat(client1.getKnownComponentsByDestinationId().get(client3DefaultDestinationId), notNullValue());
        assertThat(client1.getKnownComponentsByDestinationId().get(client3DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));

        // client 2 should see client 1's and client 3's descriptors
        assertThat(client2.getKnownComponentsByDestinationId().size(), is(2));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), notNullValue());
        assertThat("c2->c1 tool list: 1 entry", client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));
        assertThat(client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId), notNullValue());
        assertThat("c2->c3 tool list: 1 entry", client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId).size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));

        // client 3 should see client 1's descriptor
        assertThat(client3.getKnownComponentsByDestinationId().size(), is(1));
        assertThat("c3->c1 tool list: 1 entry", client3.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
        assertThat(client3.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));
    }

    /**
     * Tests the call chain for initiating and processing tool execution requests.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void toolExecution() throws Exception {
        testToolExecutionWithOptionalCancelation(false);
    }

    /**
     * Tests the call chain for initiating and canceling a tool execution request.
     * 
     * TODO consider testing different cancel timings, e.g. specified by a multi-value parameter.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void toolCancellation() throws Exception {
        testToolExecutionWithOptionalCancelation(true);
    }

    /**
     * A parameterized test case to avoid excessive code duplication between "standard execution" and "execution with cancellation".
     */
    private void testToolExecutionWithOptionalCancelation(boolean testCancellation) throws Exception {

        // this configures the part *providing* the test tool's execution; this can be slightly confusing because in this test,
        // both sides are simulated "clients" connecting to a simulated uplink server
        final Function<ToolExecutionRequest, MockToolExecutionProvider> actualToolExecutionMapper = request -> {
            // a new execution provider is created at the time of each execution request, so it is configured via this lambda
            final MockToolExecutionProvider mockExecutionProvider;
            mockExecutionProvider = new MockToolExecutionProvider(request);
            mockExecutionProvider
                .addMockOutputFile(
                    new MockFile(TEST_OUTPUT_FILE_PATH, TEST_OUTPUT_FILE_CONTENT_BYTES.length, TEST_OUTPUT_FILE_CONTENT_BYTES));
            return mockExecutionProvider;
        };

        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals("client 1 ACTIVE?", UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue("clientSession1.isActive?", clientSession1.isActive());

        // register and connect client 2 with the mock tool execution handler
        MockClientStateHolder client2 = new MockClientStateHolder(actualToolExecutionMapper);
        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client2.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals("client2 ACTIVE?", UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue("clientSession2.isActive?", clientSession2.isActive());

        // the following code configures the part *requesting/calling* the test tool's execution from client 1
        DirectoryUploadProvider inputUploadProvider = new DirectoryUploadProvider() {

            @Override
            public List<String> provideDirectoryListing() throws IOException {
                return Arrays.asList("subdir1", "subdir2");
            }

            @Override
            public void provideFiles(DirectoryUploadContext uploadContext) throws IOException {
                log.debug("Simulating upload of mock input file " + TEST_INPUT_FILE_PATH);
                uploadContext.provideFile(new FileDataSource(TEST_INPUT_FILE_PATH, TEST_INPUT_FILE_CONTENT_BYTES.length,
                    new ByteArrayInputStream(TEST_INPUT_FILE_CONTENT_BYTES)));
                uploadContext.provideFile(new FileDataSource(TEST_EMPTY_INPUT_FILE_PATH, 0,
                    new ByteArrayInputStream(new byte[0])));
            }
        };

        ToolExecutionClientSideSetup setup =
            // note: if you have an existing ToolMetadata object, use builder.copyFrom() in actual code
            ToolExecutionClientSideSetup.newBuilder()
                .toolId("myTool")
                .toolVersion("1")
                .authGroupId("MyGroup:22348asdbc")
                // set a destination id from client 2's id namespace; note that beyond the assigned namespace prefix,
                // the destination id has no meaning here; it would be interpreted by the actual ToolExecutionProvider
                .destinationId(client2.getAssignedDestinationIdPrefix() + "toolLocationPart")
                .build();

        // from here, the event expectations on the *requesting/calling* side are defined
        ToolExecutionEventHandler eventHandlerMock = EasyMock.createMock(ToolExecutionEventHandler.class);

        eventHandlerMock.onInputUploadsStarting();
        EasyMock.expect(eventHandlerMock.getInputDirectoryProvider()).andReturn(inputUploadProvider).anyTimes();
        eventHandlerMock.onInputUploadsFinished();

        // no error during execution "expected"
        eventHandlerMock.onExecutionStarting();
        eventHandlerMock.processToolExecutionEvent("mockEventType", "mockEventData");
        Capture<ToolExecutionResult> toolExecutionResultCapture = Capture.newInstance();
        eventHandlerMock.onExecutionFinished(EasyMock.capture(toolExecutionResultCapture));

        eventHandlerMock.onOutputDownloadsStarting();
        // expect the single output file callback
        Capture<FileDataSource> outputFileCapture = Capture.newInstance();
        DirectoryDownloadReceiver mockOutputFileReceiver = EasyMock.createMock(DirectoryDownloadReceiver.class);
        mockOutputFileReceiver.receiveDirectoryListing(null);
        mockOutputFileReceiver.receiveFile(EasyMock.capture(outputFileCapture));
        EasyMock.replay(mockOutputFileReceiver);
        EasyMock.expect(eventHandlerMock.getOutputDirectoryReceiver()).andReturn(mockOutputFileReceiver).anyTimes();
        eventHandlerMock.onOutputDownloadsFinished();

        eventHandlerMock.onContextClosing();

        EasyMock.replay(eventHandlerMock);

        // trigger the actual execution chain from client 1
        final Optional<ToolExecutionHandle> executionHandleResult = clientSession1.initiateToolExecution(setup, eventHandlerMock);
        assertTrue("executionHandleResult.isPresent?", executionHandleResult.isPresent());
        waitFor(SHORT_TIME); // wait for asynchronous execution

        if (testCancellation) {
            executionHandleResult.get().requestCancel();
            waitFor(SHORT_TIME); // wait for asynchronous execution
        }

        // verify that client 2 received the execution request and created an execution provider
        assertNull("client1.getLastExecutionProvider == null?", client1.getLastExecutionProvider());
        final MockToolExecutionProvider targetExecutionProvider = client2.getLastExecutionProvider();
        assertNotNull(targetExecutionProvider);

        // wait for the simulated execution to finish
        while (!toolExecutionResultCapture.hasCaptured()) {
            waitFor(SHORT_TIME); // the test timeout will break out of the loop if necessary
        }
        waitFor(SHORT_TIME); // wait until events have processed

        // verify that the expected life cycle events were fired
        EasyMock.verify(eventHandlerMock);

        // verify that execute() was called
        assertTrue("targetExecutionProvider.wasExecuteCalled?", targetExecutionProvider.wasExecuteCalled());

        if (testCancellation) {
            assertTrue("targetExecutionProvider.wasCancelCalled?", targetExecutionProvider.wasCancelCalled());
        } else {
            assertFalse("targetExecutionProvider.wasCancelCalled == false?", targetExecutionProvider.wasCancelCalled());
        }

        // verify that the execution provider received the input files
        assertEquals("received expected input files", 2, targetExecutionProvider.getReceivedInputFiles().size());
        // verify that the execution provider received the input directory sub-directories
        assertNotNull("received list if input subdirs", targetExecutionProvider.getReceivedListOfInputSubDirectories());
        assertEquals("received expected number of input subdirs", 2, targetExecutionProvider.getReceivedListOfInputSubDirectories().size());
        assertTrue("subdir1 present", targetExecutionProvider.getReceivedListOfInputSubDirectories().contains("subdir1"));
        assertTrue("subdir2 present", targetExecutionProvider.getReceivedListOfInputSubDirectories().contains("subdir2"));
        // note: these file assertions rely on the file list currently having a stable ordering; if this changes,
        // adapt these lines as this is not an actual error
        assertEquals("input file 1 signature",
            TEST_INPUT_FILE_PATH + ":" + TEST_INPUT_FILE_CONTENT_BYTES.length + ":" + TEST_INPUT_FILE_CONTENT,
            targetExecutionProvider.getReceivedInputFiles().get(0).getSignature());
        assertEquals("input file 2 (empty) signature", TEST_EMPTY_INPUT_FILE_PATH + ":0:",
            targetExecutionProvider.getReceivedInputFiles().get(1).getSignature());

        // verify that the execution caller received the output file
        final FileDataSource receivedOutputFile = outputFileCapture.getValue();
        assertEquals("output file path", TEST_OUTPUT_FILE_PATH, receivedOutputFile.getRelativePath());
        assertEquals("output file size", TEST_OUTPUT_FILE_CONTENT_BYTES.length, receivedOutputFile.getSize());
        assertArrayEquals("output file content", TEST_OUTPUT_FILE_CONTENT_BYTES, IOUtils.toByteArray(receivedOutputFile.getStream()));

        // verify the content of the ToolExecutionResult object
        ToolExecutionResult toolExecutionResult = toolExecutionResultCapture.getValue();
        assertEquals("result.successful == true?", true, toolExecutionResult.successful); // mock result
        // FIXME: shouldn't this be "true" in the cancellation test case? at least mocked? -- misc_ro, 2020-09-01
        assertEquals("result.cancelled == false?", false, toolExecutionResult.cancelled); // mock result

        // TODO (p1) 11.0: enable this check once ChannelEndpoint.dispose() is actually being called
        // assertTrue(targetExecutionProvider.wasContextClosingCalled());
    }

    /**
     * Integration test for the tool documentation fetching round trip.
     * 
     * @throws Exception on unexpected failure
     */
    @Test
    public void documentationFetching() throws Exception {
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);

        final int dummyDataSize = 99;

        MockClientStateHolder client2 = new MockClientStateHolder(null) {

            @Override
            public Optional<SizeValidatedDataSource> provideToolDocumentationData(String sourceId, String docReferenceId) {
                try {
                    log.debug("Received docs request for sourceId " + sourceId + ", docs ref: " + docReferenceId);
                    if (docReferenceId.equals(DOCUMENTATION_ID_EXISTING)) {
                        // test with a non-streamed data block
                        final ByteArrayOutputStream baas = new ByteArrayOutputStream(dummyDataSize);
                        generateMockDocumentationStream(baas, dummyDataSize);
                        return Optional.of(new SizeValidatedDataSource(baas.toByteArray()));
                    } else if (docReferenceId.equals(DOCUMENTATION_ID_LARGER_THAN_MESSAGE_BLOCK)) {
                        // create a local stream pair to pipe the documentation data through
                        final PipedOutputStream providerOutputStream = new PipedOutputStream();
                        final PipedInputStream providerInputStream = new PipedInputStream(providerOutputStream);
                        // spawn a thread to asynchronously create the documentation data
                        asyncTaskService.execute("Provide test docs data stream", () -> {
                            try {
                                // wait for a bit so that the stream data is not already available when the return object is processed
                                Thread.sleep(SHORT_TIME);
                                generateMockDocumentationStream(providerOutputStream, DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK);
                            } catch (InterruptedException | IOException e) {
                                log.error(e);
                            }
                        });
                        return Optional.of(new SizeValidatedDataSource(DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK,
                            providerInputStream));
                    } else {
                        return Optional.empty();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        client2.waitForSessionInitCompletion(SHORT_TIME);

        // create an arbitrary destination id within the virtual network connected via client 2
        String destinationId = client2.getAssignedDestinationIdPrefix() + "netInternalId";

        // try to send the request from client 2 to itself; this is not allowed and should fail
        Optional<SizeValidatedDataSource> result;
        result = clientSession2.fetchDocumentationData(destinationId, DOCUMENTATION_ID_EXISTING);

        assertFalse(result.isPresent());

        // now send it from client 1 to client 2; this should succeed
        result = clientSession1.fetchDocumentationData(destinationId, DOCUMENTATION_ID_EXISTING);
        assertTrue(result.isPresent());
        validateMockDocumentationStream(result.get(), dummyDataSize);

        // now test the transfer of "large" documentation, i.e. data that cannot be transfered in a single MessageBlock

        // first, check that the test size is actually sufficient
        assertTrue(DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK > UplinkProtocolConstants.MAX_FILE_TRANSFER_CHUNK_SIZE);

        // test the behavior
        result = clientSession1.fetchDocumentationData(destinationId, DOCUMENTATION_ID_LARGER_THAN_MESSAGE_BLOCK);
        assertTrue(result.isPresent());
        // note: as this method validates the actual streamed content, this also checks the check reconstruction from message blocks
        validateMockDocumentationStream(result.get(), DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK);
    }

    @Test(timeout = 120000)
    // regression test for issue #17448
    public void regressionTestDeadlockOnToolUpdates() throws InterruptedException, TimeoutException, OperationFailureException {
        MockClientStateHolder client1 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        client1.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals("client 1 ACTIVE?", UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue("clientSession1.isActive?", clientSession1.isActive());

        final AtomicBoolean shouldTerminate = new AtomicBoolean();
        final AtomicBoolean encounteredError = new AtomicBoolean();
        final UplinkProtocolMessageConverter messageConverter = new UplinkProtocolMessageConverter("Test util");

        asyncTaskService.execute("Continuously trigger tool list updates", () -> {
            // give the clients a little head start
            waitFor(SHORT_TIME * 5);
            log.debug("Starting to send continuous tool list updates from client " + SESSION_QUALIFIER_CLIENT1);
            int count = 0;
            while (!shouldTerminate.get()) {
                try {
                    ToolDescriptorListUpdate update = new ToolDescriptorListUpdate("dummy", "dummy", new ArrayList<>());
                    // true = in this particular setup, block if the send queue is full
                    clientSession1
                        .enqueueMessageBlockForSending(UplinkProtocolConstants.DEFAULT_CHANNEL_ID,
                            messageConverter.encodeToolDescriptorListUpdate(update), MessageBlockPriority.TOOL_DESCRIPTOR_UPDATES, true);
                    Thread.sleep(1); // add a minimal wait time to avoid driving the other clients into timeout by sheer message volume
                    count++;
                } catch (IOException | InterruptedException e) {
                    encounteredError.set(true);
                    shouldTerminate.set(true);
                    log.error("Failed to send tool update: " + e.toString());
                }
            }
            log.debug("Total tool list updates sent from client " + SESSION_QUALIFIER_CLIENT1 + ": " + count);
        });

        final int numConnectCycleClients = 5;
        final int connectCyclesPerClient = 20;
        final int totalTimeoutSeconds = 60;

        CountDownLatch cdl = new CountDownLatch(numConnectCycleClients);
        for (int clientIndex = 1; clientIndex <= numConnectCycleClients; clientIndex++) {
            final String clientId = "cycler" + Integer.toString(clientIndex);
            asyncTaskService.execute("Simulate single client", () -> {
                try {
                    for (int i = 1; i <= connectCyclesPerClient; i++) {
                        try {
                            performAndVerifyConnectDisconnectCycle(clientId);
                        } catch (OperationFailureException | InterruptedException | TimeoutException | AssertionError e) {
                            // important: catch AssertionErrors here, too!
                            encounteredError.set(true);
                            log.error("Error during connect/disconnect cycle " + i + " of client " + clientId + ": " + e.toString());
                            break; // exit loop
                        }
                    }
                } finally {
                    cdl.countDown();
                }
            });
        }

        try {
            cdl.await(totalTimeoutSeconds, TimeUnit.SECONDS);
            log.debug("All cycling threads terminated within the time limit (regardless of error status)");
            assertFalse("A (potentially) async error has occurred; check the log output", encounteredError.get());
        } finally {
            shouldTerminate.set(true);
        }
    }

    protected abstract UplinkConnection setUpClientConnection(ThroughputLimiter outgoingThroughputLimiter,
        ThroughputLimiter incomingThroughputLimiter);

    protected abstract void simulateClientSideEOF(UplinkConnection connection);

    protected ServerSideUplinkSessionServiceImpl getMockServerSideUplinkSessionService() {
        return mockServerSideUplinkSessionService;
    }

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private void awaitStateOrFail(UplinkSessionState targetState, ClientSideUplinkSession clientSession) {
        int totalTimeWaited = 0;
        while (clientSession.getState() != targetState) {
            totalTimeWaited += STATE_WAITING_CHECK_INTERVAL;
            if (totalTimeWaited < STATE_WAITING_TIMEOUT) {
                try {
                    Thread.sleep(STATE_WAITING_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    throw new AssertionError("Interrupted while waiting for state change of " + clientSession.getLogDescriptor() + " to "
                        + targetState + ": " + e.toString());
                }
            } else {
                fail("Client session " + clientSession.getLogDescriptor() + " did not reach its target state " + targetState + " within "
                    + STATE_WAITING_TIMEOUT + " msec; final state: " + clientSession.getState());
            }
        }
    }

    private void performAndVerifyConnectDisconnectCycle(String clientId)
        throws OperationFailureException, InterruptedException, TimeoutException, AssertionError {
        MockClientStateHolder cylceTestClient = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSessionCycleTest = testContext.setUpAndStartSession(cylceTestClient, clientId);
        cylceTestClient.waitForSessionInitCompletion(SHORT_TIME);
        assertEquals("cycle test client ACTIVE?", UplinkSessionState.ACTIVE, clientSessionCycleTest.getState());
        assertTrue("cycle test client session isActive?", clientSessionCycleTest.isActive());
        clientSessionCycleTest.initiateCleanShutdownIfRunning();
        awaitStateOrFail(UplinkSessionState.CLEAN_SHUTDOWN, clientSessionCycleTest); // throws AssertionError
    }

    private void generateMockDocumentationStream(final OutputStream providerOutputStream, int dataLength) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(providerOutputStream);
        for (int i = 0; i < dataLength; i++) {
            bos.write((byte) i * SOME_PRIME_NUMBER);
        }
        bos.close();
        log.debug("Completed writing a mock documentation stream of size " + dataLength);
    }

    private void validateMockDocumentationStream(final SizeValidatedDataSource largeDocsDataSource, int expectedLength)
        throws IOException {
        assertEquals(expectedLength, largeDocsDataSource.getSize());

        BufferedInputStream bis = new BufferedInputStream(largeDocsDataSource.getStream());
        assertFalse(largeDocsDataSource.receivedCompletely());
        for (int i = 0; i < largeDocsDataSource.getSize(); i++) {
            final byte expectedByteVal = (byte) (i * SOME_PRIME_NUMBER);
            assertEquals(expectedByteVal, (byte) bis.read());
        }
        assertTrue(largeDocsDataSource.receivedCompletely()); // TODO could be improved by checking this flag at n-1, n, n+1 --misc_ro
    }

    private void shutDownClientAndVerifyCleanShutdown(MockClientStateHolder client, ClientSideUplinkSession clientSession) {
        clientSession.initiateCleanShutdownIfRunning();
        assertTrue(clientSession.isShuttingDownOrShutDown());
        awaitStateOrFail(UplinkSessionState.CLEAN_SHUTDOWN, clientSession);
        assertFalse(client.isSessionActive());
        assertFalse(clientSession.isActive());
    }
}
