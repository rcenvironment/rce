/*
 * Copyright 2019 DLR, Germany
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
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
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
import de.rcenvironment.core.communication.uplink.network.internal.ServerSideUplinkLowLevelProtocolWrapper;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkEndpointServiceImpl;
import de.rcenvironment.core.communication.uplink.relay.internal.ServerSideUplinkSessionServiceImpl;
import de.rcenvironment.core.communication.uplink.session.api.UplinkSessionState;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;

/**
 * Integration tests for the call chain through the layers of the "uplink" mechanism.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractUplinkIntegrationTest {

    private static final String DUMMY_NAME = "dummyName";

    private static final String SESSION_QUALIFIER_CLIENT2 = "client2";

    private static final String SESSION_QUALIFIER_CLIENT1 = "client1";

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

    // TODO there are fairly consistent single-test failures with 200 msec; find out why
    private static final int SHORT_TIME = 300; // msec to wait for lightweight asynchronous operations to complete

    protected final Log log = LogFactory.getLog(getClass());

    private LocalUplinkSessionServiceImpl mockUplinkSessionService;

    private ServerSideUplinkSessionServiceImpl mockServerSideUplinkSessionService;

    private UplinkTestContext testContext;

    /**
     * Holds the context of a single test run, e.g. established connections, and provides utility methods.
     *
     * @author Robert Mischke
     */
    private final class UplinkTestContext {

        private final List<ClientSideUplinkSession> clientSessions = new ArrayList<>();

        public synchronized ClientSideUplinkSession setUpSession(MockClientStateHolder clientMock, String sessionQualifier) {
            UplinkConnection uplinkConnection = setUpClientConnection();

            final ClientSideUplinkSessionParameters sessionParameters =
                new ClientSideUplinkSessionParameters("Test session", sessionQualifier, clientMock.getCustomHandshakeParameters());
            final ClientSideUplinkSession session =
                mockUplinkSessionService.createSession(uplinkConnection, sessionParameters, clientMock);
            clientSessions.add(session);
            return session;
        }

        public void startSession(final ClientSideUplinkSession session) {
            ConcurrencyUtils.getAsyncTaskService().execute("Run mock client Uplink session", () -> {
                try {
                    session.runSession();
                } catch (IOException e) {
                    // TODO improve
                    log.error("Caught error from runSession()", e);
                }
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
                    session.close();
                } catch (IllegalStateException e) {
                    log.warn("Session " + session.getLogDescriptor() + " was already closed");
                }
            }
        }

    }

    /**
     * Creates mock service implementations.
     */
    @Before
    public void setUpMockServices() {
        mockUplinkSessionService = new LocalUplinkSessionServiceImpl();
        mockUplinkSessionService.bindConcurrencyUtilsFactory(ConcurrencyUtils.getFactory());

        final ServerSideUplinkEndpointServiceImpl mockServerSideUplinkEndpointService = new ServerSideUplinkEndpointServiceImpl();
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
        client1.waitForSessionActivation(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(client1.isSessionActive());
        assertTrue(clientSession1.isActive());
        assertNull(client1.getSessionErrorMessage());

        clientSession1.close();
        assertEquals(UplinkSessionState.PARTIALLY_CLOSED_BY_LOCAL, clientSession1.getState());
        waitFor(SHORT_TIME); // wait for asynchronous execution
        assertEquals(UplinkSessionState.FULLY_CLOSED, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
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
        waitFor(SHORT_TIME); // do not wait for session activation as it is not expected to happen
        assertEquals(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());
        assertTrue(client1.getSessionErrorMessage().endsWith(testMessage)); // allow message prefixing
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
            ClientSideUplinkSession clientSession = testContext.setUpAndStartSession(clients[i], "sharedQ");
        }
        waitFor(SHORT_TIME);

        int numActive = 0;
        int numRefused = 0;
        for (int i = 0; i < numClients; i++) {
            if (clients[i].isSessionActive()) {
                numActive++;
            }
            if (clients[i].isSessionInFinalState()) {
                numRefused++;
            }
        }

        assertEquals(1, numActive);
        assertEquals(numClients - 1, numRefused);

        testContext.closeAllClientSessions();
        waitFor(SHORT_TIME); // wait for asynchronous execution

        for (int i = 0; i < numClients; i++) {
            assertFalse(clients[i].isSessionActive());
            assertTrue(clients[i].isSessionInFinalState());
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
        final String testMessage = "hf message";
        customHandshakeParameters.put(UplinkProtocolConstants.HANDSHAKE_KEY_SIMULATE_HANDSHAKE_FAILURE, testMessage);
        client1.setCustomHandshakeParameters(customHandshakeParameters);
        ClientSideUplinkSession clientSession1 = testContext.setUpAndStartSession(client1, SESSION_QUALIFIER_CLIENT1);
        waitFor(SHORT_TIME); // do not wait for session activation as it is not expected to happen
        assertEquals(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());

        // only test presence to allow message prefixing/suffixing
        assertTrue(client1.getSessionErrorMessage(),
            client1.getSessionErrorMessage().contains(ServerSideUplinkLowLevelProtocolWrapper.ERROR_MESSAGE_CONNECTION_SETUP_FAILED));

        // wait for the handshake data timeout to verify that no redundant "handshake timeout" error is reported
        Thread.sleep(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC + SHORT_TIME);
        assertFalse(client1.getInconsistentStateFlag()); // set true on duplicate callbacks
    }

    /**
     * Error handling test that simulates the server side closing the connection due to a handshake error (e.g. protocol version mismatch).
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
        client1.waitForSessionActivation(
            UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);
        assertEquals(UplinkSessionState.SESSION_REFUSED_OR_HANDSHAKE_ERROR, clientSession1.getState());
        assertFalse(client1.isSessionActive());
        assertFalse(clientSession1.isActive());
        assertNotNull(client1.getSessionErrorMessage());
        assertTrue(client1.getSessionErrorMessage().contains("handshake response within"));
        // verify that no redundant "handshake timeout" errors were reported
        assertFalse(client1.getInconsistentStateFlag()); // set true on duplicate callbacks

        // give the server time to detect that the client did not confirm the handshake exchange
        waitFor(UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC + UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);

        // verify that another client can connect using the same qualifier (which fails if the first session was not properly cleaned up)
        MockClientStateHolder client1b = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession1b = testContext.setUpAndStartSession(client1b, SESSION_QUALIFIER_CLIENT1);
        client1b.waitForSessionActivation(
            UplinkProtocolConstants.HANDSHAKE_RESPONSE_TIMEOUT_MSEC);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1b.getState());
        assertTrue(clientSession1b.isActive());
        assertNull(client1b.getSessionErrorMessage());
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
        client1.waitForSessionActivation(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(clientSession1.isActive());

        MockClientStateHolder client2 = new MockClientStateHolder(null);
        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client2.waitForSessionActivation(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue(clientSession2.isActive());

        // verify initial state
        assertNotNull(client1.getKnownComponentsByDestinationId());
        assertEquals(0, client1.getKnownComponentsByDestinationId().size());
        assertNotNull(client2.getKnownComponentsByDestinationId());
        assertEquals(0, client2.getKnownComponentsByDestinationId().size());

        // client 1 sends an empty update
        final String client1DefaultDestinationId = client1.getAssignedDestinationIdPrefix();
        // final String client2DefaultDestinationId = client2.getAssignedDestinationIdRoot();
        // log.debug("Client 1's default destination id: " + client1DefaultDestinationId);
        // log.debug("Client 2's default destination id: " + client2DefaultDestinationId);

        // publish an EMPTY descriptor list from client 1
        clientSession1
            .publishToolDescriptorListUpdate(new ToolDescriptorListUpdate(client1DefaultDestinationId, DUMMY_NAME, new ArrayList<>()));

        waitFor(SHORT_TIME); // wait for asynchronous execution

        // only clients in other networks should see the update
        assertThat(client1.getKnownComponentsByDestinationId().size(), is(0));
        assertThat(client1.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), nullValue());
        assertThat(client2.getKnownComponentsByDestinationId().size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), notNullValue());
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(0));

        Set<String> groupSet = new HashSet<String>();
        groupSet.add(TEST_GROUP_A_ID);

        // publish an single-entry descriptor list from client 1
        ToolDescriptor mockDescriptor1 = new ToolDescriptor(TEST_TOOL_1_ID, TEST_TOOL_1_VERSION, groupSet, null, null);
        clientSession1
            .publishToolDescriptorListUpdate(
                new ToolDescriptorListUpdate(client1DefaultDestinationId, DUMMY_NAME, Arrays.asList(mockDescriptor1)));

        waitFor(SHORT_TIME); // wait for asynchronous execution

        // only clients in other networks should see the update
        assertThat(client1.getKnownComponentsByDestinationId().size(), is(0));
        assertThat(client1.getKnownComponentsByDestinationId().get(client1DefaultDestinationId), nullValue());
        assertThat(client2.getKnownComponentsByDestinationId().size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));

        // connect another client to check that it receives the last cached updates
        MockClientStateHolder client3 = new MockClientStateHolder(null);
        ClientSideUplinkSession clientSession3 = testContext.setUpAndStartSession(client3, "client3"); // as above for client2
        client3.waitForSessionActivation(SHORT_TIME);
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
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));
        assertThat(client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId), notNullValue());
        assertThat(client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId).size(), is(1));
        assertThat(client2.getKnownComponentsByDestinationId().get(client3DefaultDestinationId).get(0).getToolId(), is(TEST_TOOL_1_ID));

        // client 3 should see client 1's descriptor
        assertThat(client3.getKnownComponentsByDestinationId().size(), is(1));
        assertThat(client3.getKnownComponentsByDestinationId().get(client1DefaultDestinationId).size(), is(1));
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
        client1.waitForSessionActivation(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession1.getState());
        assertTrue(clientSession1.isActive());

        // register and connect client 2 with the mock tool execution handler
        MockClientStateHolder client2 = new MockClientStateHolder(actualToolExecutionMapper);
        // set a different "session qualifier"/"client id" to simulate a second client using the same login
        ClientSideUplinkSession clientSession2 = testContext.setUpAndStartSession(client2, SESSION_QUALIFIER_CLIENT2);
        client2.waitForSessionActivation(SHORT_TIME);
        assertEquals(UplinkSessionState.ACTIVE, clientSession2.getState());
        assertTrue(clientSession2.isActive());

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
                .destinationId(client2.getAssignedDestinationIdPrefix() + "arbitrary")
                .build();

        // from here, the event expectations on the *requesting/calling* side are defined
        ToolExecutionEventHandler eventHandlerMock = EasyMock.createMock(ToolExecutionEventHandler.class);

        eventHandlerMock.onInputUploadsStarting();
        EasyMock.expect(eventHandlerMock.getInputDirectoryProvider()).andReturn(inputUploadProvider).anyTimes();
        eventHandlerMock.onInputUploadsFinished();

        // no error during execution "expected"
        eventHandlerMock.onExecutionStarting();
        eventHandlerMock.processToolExecutionEvent("mockEventType", "mockEventData");
        Capture<ToolExecutionResult> toolExecutionResultCapture = new Capture<>();
        eventHandlerMock.onExecutionFinished(EasyMock.capture(toolExecutionResultCapture));

        eventHandlerMock.onOutputDownloadsStarting();
        // expect the single output file callback
        Capture<FileDataSource> outputFileCapture = new Capture<>();
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
        assertTrue(executionHandleResult.isPresent());
        waitFor(SHORT_TIME); // wait for asynchronous execution

        if (testCancellation) {
            executionHandleResult.get().requestCancel();
            waitFor(SHORT_TIME); // wait for asynchronous execution
        }

        // verify that client 2 received the execution request and created an execution provider
        assertNull(client1.getLastExecutionProvider());
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
        assertTrue(targetExecutionProvider.wasExecuteCalled());

        if (testCancellation) {
            assertTrue(targetExecutionProvider.wasCancelCalled());
        } else {
            assertFalse(targetExecutionProvider.wasCancelCalled());
        }

        // verify that the execution provider received the input files
        assertEquals(2, targetExecutionProvider.getReceivedInputFiles().size());
        // verify that the execution provider received the input directory sub-directories
        assertNotNull(targetExecutionProvider.getReceivedListOfInputSubDirectories());
        assertEquals(2, targetExecutionProvider.getReceivedListOfInputSubDirectories().size());
        assertTrue(targetExecutionProvider.getReceivedListOfInputSubDirectories().contains("subdir1"));
        assertTrue(targetExecutionProvider.getReceivedListOfInputSubDirectories().contains("subdir2"));
        // note: these file assertions rely on the file list currently having a stable ordering; if this changes,
        // adapt these lines as this is not an actual error
        assertEquals(TEST_INPUT_FILE_PATH + ":" + TEST_INPUT_FILE_CONTENT_BYTES.length + ":" + TEST_INPUT_FILE_CONTENT,
            targetExecutionProvider.getReceivedInputFiles().get(0).getSignature());
        assertEquals(TEST_EMPTY_INPUT_FILE_PATH + ":0:",
            targetExecutionProvider.getReceivedInputFiles().get(1).getSignature());

        // verify that the execution caller received the output file
        final FileDataSource receivedOutputFile = outputFileCapture.getValue();
        assertEquals(TEST_OUTPUT_FILE_PATH, receivedOutputFile.getRelativePath());
        assertEquals(TEST_OUTPUT_FILE_CONTENT_BYTES.length, receivedOutputFile.getSize());
        assertArrayEquals(TEST_OUTPUT_FILE_CONTENT_BYTES, IOUtils.toByteArray(receivedOutputFile.getStream()));

        // verify the content of the ToolExecutionResult object
        ToolExecutionResult toolExecutionResult = toolExecutionResultCapture.getValue();
        assertEquals(true, toolExecutionResult.successful); // mock result
        assertEquals(false, toolExecutionResult.cancelled); // mock result

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
                        ConcurrencyUtils.getAsyncTaskService().execute("Provide test docs data stream", () -> {
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
        client1.waitForSessionActivation(SHORT_TIME);
        client2.waitForSessionActivation(SHORT_TIME);

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
        assertTrue(DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK > UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH);

        // test the behavior
        result = clientSession1.fetchDocumentationData(destinationId, DOCUMENTATION_ID_LARGER_THAN_MESSAGE_BLOCK);
        assertTrue(result.isPresent());
        // note: as this method validates the actual streamed content, this also checks the check reconstruction from message blocks
        validateMockDocumentationStream(result.get(), DOCUMENTATION_SIZE_LARGER_THAN_MESSAGE_BLOCK);
    }

    protected abstract UplinkConnection setUpClientConnection();

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

}
