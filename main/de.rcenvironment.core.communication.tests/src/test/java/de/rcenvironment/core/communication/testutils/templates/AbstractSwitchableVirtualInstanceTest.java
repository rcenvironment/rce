/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.configuration.NodeConfigurationService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetup;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupService;
import de.rcenvironment.core.communication.connection.api.ConnectionSetupState;
import de.rcenvironment.core.communication.connection.api.DisconnectReason;
import de.rcenvironment.core.communication.connection.impl.ConnectionSetupServiceImpl;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.nodeproperties.NodePropertyConstants;
import de.rcenvironment.core.communication.protocol.ProtocolConstants;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.ConnectionSetupStateTracker;
import de.rcenvironment.core.communication.testutils.TestNetworkRequestHandler;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;
import de.rcenvironment.core.communication.transport.virtual.testutils.VirtualTopology;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Base class providing tests that can operate using duplex as well as non-duplex transports. A common use case is setting up a topology
 * where are topology links are logially bidirectional. In this case, the test code must adapt as this bidirectional linkage is
 * automatically achieved when using a duplex transport, but must be explicitly wired when using a non-duplex transport.
 * 
 * @author Robert Mischke
 * @author Phillip Kroll
 */
public abstract class AbstractSwitchableVirtualInstanceTest extends AbstractVirtualInstanceTest {

    private static final String DEFAULT_SERVER_NODE_ID = "server";

    /**
     * A short wait time for connect/disconnect actions to complete; fairly long to support real transports in stress-test setups.
     */
    private static final int CONNECTION_OPERATION_WAIT_MSEC = 5000;

    // simulated number of clients for concurrent connection setups testing; 5 for standard, 20 for extended testing
    private static final int CONCURRENT_CS_TEST_NUM_CLIENTS = CommonTestOptions.selectStandardOrExtendedValue(5, 20);

    private static final int CONCURRENT_CS_TEST_TIMEOUT_MSEC = 10000;

    private static final String MESSAGE_INSTANCES_DID_NOT_CONVERGE_AT_ITERATION = "Instances did not converge at iteration index ";

    // test size 5 for fast/standard testing, 10 for extended testing
    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(5, 10);

    protected final Random randomGenerator = new Random();

    /**
     * @throws Exception on uncaught exceptions
     */
    @BeforeClass
    // TODO transitional; rework this
    public static void setTestParameters() throws Exception {
        testSize = TEST_SIZE;
    }

    @Override
    protected void setupInstances(int numNodes, boolean useDuplexTransport, boolean startInstances) throws InterruptedException {
        super.setupInstances(numNodes, useDuplexTransport, startInstances);
        // this causes all connect() calls to explicitly create a connection for each direction by default
        testTopology.setConnectBothDirectionsByDefaultFlag(!usingDuplexTransport);
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testClientServerBidirectionalMessaging() throws Exception {

        setupInstances(2, usingDuplexTransport, true);

        VirtualInstance client = testTopology.getInstance(0);
        VirtualInstance server = testTopology.getInstance(1);
        prepareWaitForNextMessage();
        testTopology.connect(0, 1);
        waitForNextMessage();
        waitForNetworkSilence();

        NetworkResponse serverResponse =
            client.performRoutedRequest("c2s", ProtocolConstants.VALUE_MESSAGE_TYPE_TEST, server.getInstanceNodeSessionId());
        assertNotNull("C2S communication failed", serverResponse);
        assertTrue("C2S communication failed: " + NetworkFormatter.networkResponseToString(serverResponse), serverResponse.isSuccess());
        assertEquals(TestNetworkRequestHandler.getTestResponse("c2s", server.getInstanceNodeSessionId()),
            serverResponse.getDeserializedContent());
        NetworkResponse clientResponse =
            server.performRoutedRequest("s2c", ProtocolConstants.VALUE_MESSAGE_TYPE_TEST, client.getInstanceNodeSessionId());
        assertNotNull("S2C communication failed", clientResponse);
        assertTrue("S2C communication failed", clientResponse.isSuccess());
        assertEquals(TestNetworkRequestHandler.getTestResponse("s2c", client.getInstanceNodeSessionId()),
            clientResponse.getDeserializedContent());

        // Systemx.out.println(NetworkFormatter.summary(client.getTopologyMap()));
        // Systemx.out.println(NetworkFormatter.summary(server.getTopologyMap()));

        prepareWaitForNextMessage();
        testTopology.getAsGroup().shutDown();
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testIterativelyGrowingLinearNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;

            prepareWaitForNextMessage();
            testTopology.connect(i, newInstanceIndex);
            waitForNextMessage();
            waitForNetworkSilence();

            // note: the third parameter in Arrays.copyOfRange() is exclusive and must be "last + 1"
            assertTrue(MESSAGE_INSTANCES_DID_NOT_CONVERGE_AT_ITERATION + i,
                instanceUtils.allInstancesHaveSameRawNetworkGraph(Arrays.copyOfRange(allInstances, 0, newInstanceIndex + 1)));
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testConcurrentlyGrowingLinearNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        prepareWaitForNextMessage();
        for (int i = 0; i < allInstances.length - 1; i++) {
            testTopology.connect(i, i + 1);
        }
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(testTopology.allInstancesConverged());
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testIterativelyGrowingRandomAcyclicNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            int connectedInstanceIndex = random.nextInt(newInstanceIndex); // 0..newInstanceIndex-1
            prepareWaitForNextMessage();
            testTopology.connect(connectedInstanceIndex, newInstanceIndex);
            waitForNextMessage();
            waitForNetworkSilence();

            // note: the third parameter in Arrays.copyOfRange() is exclusive and must be "last + 1"
            assertTrue(MESSAGE_INSTANCES_DID_NOT_CONVERGE_AT_ITERATION + i,
                instanceUtils.allInstancesHaveSameRawNetworkGraph(Arrays.copyOfRange(allInstances, 0, newInstanceIndex + 1)));
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testIterativelyGrowingRandomCyclicNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            int connectedInstanceIndex1 = random.nextInt(newInstanceIndex); // 0..newInstanceIndex-1
            prepareWaitForNextMessage();
            testTopology.connect(connectedInstanceIndex1, newInstanceIndex);
            // connect to two existing nodes per iteration to create cycles
            if (i >= 3) {
                // do not start at i=2, otherwise the starting graph will always be the same
                int connectedInstanceIndex2;
                do {
                    connectedInstanceIndex2 = random.nextInt(newInstanceIndex); // 0..i
                } while (connectedInstanceIndex2 == connectedInstanceIndex1);
                testTopology.connect(connectedInstanceIndex2, newInstanceIndex);
            }
            waitForNextMessage();
            waitForNetworkSilence();

            // note: the third parameter in Arrays.copyOfRange() is exclusive and must be "last + 1"
            assertTrue(MESSAGE_INSTANCES_DID_NOT_CONVERGE_AT_ITERATION + i,
                instanceUtils.allInstancesHaveSameRawNetworkGraph(Arrays.copyOfRange(allInstances, 0, newInstanceIndex + 1)));
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testConcurrentlyGrowingRandomAcyclicNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        prepareWaitForNextMessage();
        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            int connectedInstanceIndex = random.nextInt(newInstanceIndex); // 0..i
            testTopology.connect(connectedInstanceIndex, newInstanceIndex);
        }
        waitForNextMessage();
        waitForNetworkSilence();
        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testConcurrentlyGrowingRandomCyclicNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        prepareWaitForNextMessage();
        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            int connectedInstanceIndex1 = random.nextInt(newInstanceIndex); // 0..i
            testTopology.connect(connectedInstanceIndex1, newInstanceIndex);
            // connect to two existing nodes per iteration to create cycles
            if (i >= 3) {
                // do not start at i=2, otherwise the starting graph will always be the same
                int connectedInstanceIndex2;
                do {
                    connectedInstanceIndex2 = random.nextInt(newInstanceIndex); // 0..i
                } while (connectedInstanceIndex2 == connectedInstanceIndex1);
                testTopology.connect(connectedInstanceIndex2, newInstanceIndex);
            }
        }
        waitForNextMessage();
        waitForNetworkSilence();
        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testNodePropertiesInConcurrentlyGrowingRandomNetwork() throws Exception {

        setupInstances(TEST_SIZE, usingDuplexTransport, true);

        prepareWaitForNextMessage();
        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            int connectedInstanceIndex = random.nextInt(newInstanceIndex); // 0..i
            testTopology.connect(connectedInstanceIndex, newInstanceIndex);
        }
        waitForNextMessage();
        waitForNetworkSilence();
        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));

        // check for consistently converged properties on all nodes
        for (VirtualInstance vi : allInstances) {
            Map<InstanceNodeSessionId, Map<String, String>> allNodeProperties = vi.getNodePropertiesService().getAllNodeProperties();
            assertEquals(allInstances.length, allNodeProperties.size());
            // each node should see proper "nodeId" properties fields for each node
            for (VirtualInstance vi2 : allInstances) {
                InstanceNodeSessionId nodeId = vi2.getInstanceNodeSessionId();
                Map<String, String> nodeProperties = allNodeProperties.get(nodeId);
                assertNotNull("No metadata for node " + nodeId + " at " + vi.getInstanceNodeSessionId(), nodeProperties);
                assertEquals(nodeId.getInstanceNodeSessionIdString(), nodeProperties.get(NodePropertyConstants.KEY_NODE_ID));
            }
        }

        // inject a metadata value at each node
        String insertionTestMetadataKey = "insertionTest";
        String insertionTestDataSuffix = ".inserted";
        prepareWaitForNextMessage();
        for (VirtualInstance vi : allInstances) {
            vi.getNodePropertiesService().addOrUpdateLocalNodeProperty(insertionTestMetadataKey,
                vi.getInstanceNodeSessionId().getInstanceNodeSessionIdString() + insertionTestDataSuffix);
        }
        waitForNextMessage();
        waitForNetworkSilence();

        // check for consistent *injected* properties on all nodes
        for (VirtualInstance vi : allInstances) {
            Map<InstanceNodeSessionId, Map<String, String>> allNodeProperties = vi.getNodePropertiesService().getAllNodeProperties();
            assertEquals(allInstances.length, allNodeProperties.size());
            // each node should see proper "nodeId" property fields for each node
            for (VirtualInstance vi2 : allInstances) {
                InstanceNodeSessionId nodeId = vi2.getInstanceNodeSessionId();
                assertEquals(nodeId.getInstanceNodeSessionIdString() + insertionTestDataSuffix,
                    allNodeProperties.get(nodeId).get(insertionTestMetadataKey));
            }
        }

        // print debug output
        // Map<NodeIdentifier, Map<String, String>> allNodeProperties =
        // allInstances[0].getNodePropertiesService().getAllNodeProperties();
        // Thread.sleep(500);
        // for (VirtualInstance vi : allInstances) {
        // NodeIdentifier nodeId = vi.getNodeId();
        // log.info("Metadata knowledge about " + nodeId + ": " + allNodeProperties.get(nodeId));
        // }
        // Thread.sleep(500);
    }

    /**
     * Tests for proper {@link ConnectionSetupService} behaviour.
     * 
     * @throws Exception on uncaught Exceptions
     */
    @Test(timeout = CONCURRENT_CS_TEST_TIMEOUT_MSEC)
    public void testConcurrentConnectionSetupBehaviour() throws Exception {
        final VirtualInstance server = new VirtualInstance(DEFAULT_SERVER_NODE_ID, false); // no relay
        server.registerNetworkTransportProvider(transportProvider);
        server.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        server.start();

        ConnectionSetupService serverConnectionSetupService = server.getConnectionSetupService();
        assertEquals(0, serverConnectionSetupService.getAllConnectionSetups().size());

        final List<RuntimeException> exceptions = Collections.synchronizedList(new ArrayList<RuntimeException>());

        final CountDownLatch phase1Cdl = new CountDownLatch(CONCURRENT_CS_TEST_NUM_CLIENTS);
        final CountDownLatch serverShutdownCdl = new CountDownLatch(1);
        final CountDownLatch phase2Cdl = new CountDownLatch(CONCURRENT_CS_TEST_NUM_CLIENTS);

        AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();
        for (int i = 1; i <= CONCURRENT_CS_TEST_NUM_CLIENTS; i++) {
            final int i2 = i;
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    log.debug("Starting thread " + i2);
                    try {
                        VirtualInstance client = new VirtualInstance("client-" + i2);
                        client.registerNetworkTransportProvider(transportProvider);
                        client.start();
                        log.debug("Created client " + client.getInstanceNodeSessionId());
                        ConnectionSetupServiceImpl clientConnectionSetupService =
                            (ConnectionSetupServiceImpl) client.getConnectionSetupService();

                        assertEquals(0, clientConnectionSetupService.getAllConnectionSetups().size());
                        ConnectionSetup setupC2S =
                            clientConnectionSetupService.createConnectionSetup(server.getDefaultContactPoint(), "test-cs-" + i2, true);
                        assertEquals(1, clientConnectionSetupService.getAllConnectionSetups().size());

                        ConnectionSetupStateTracker tracker = new ConnectionSetupStateTracker(setupC2S);
                        clientConnectionSetupService.addConnectionSetupListener(tracker);

                        testConnectionSetupBehaviourBeforePotentialServerShutdown(client, server, clientConnectionSetupService,
                            setupC2S, tracker);
                        phase1Cdl.countDown();

                        // if this is not a duplex test, verify direct connection shutdown
                        if (usingDuplexTransport) {
                            log.debug("Waiting for server shutdown...");
                            serverShutdownCdl.await();
                        } else {
                            setupC2S.signalStopIntent();
                            tracker.awaitAndExpect(ConnectionSetupState.DISCONNECTING, CONNECTION_OPERATION_WAIT_MSEC);
                        }

                        testConnectionSetupBehaviourAfterPotentialServerShutdown(client, server, clientConnectionSetupService,
                            setupC2S, tracker);
                        client.shutDown();
                        phase2Cdl.countDown();
                    } catch (InterruptedException | AssertionError | TimeoutException e) {
                        onException(e);
                    }
                }

                private void onException(Throwable e) {
                    exceptions.add(new RuntimeException(e));
                    // let it terminate quickly; it will fail anyway
                    phase1Cdl.countDown();
                    phase2Cdl.countDown();
                }
            });
        }

        phase1Cdl.await();

        assertEquals(0, serverConnectionSetupService.getAllConnectionSetups().size());

        if (usingDuplexTransport) {
            // if this is a duplex test, verify indirect/remote connection shutdown
            server.shutDown();
            serverShutdownCdl.countDown();
        }
        phase2Cdl.await();
        if (!usingDuplexTransport) {
            server.shutDown();
        }

        assertEquals(0, serverConnectionSetupService.getAllConnectionSetups().size());

        if (!exceptions.isEmpty()) {
            for (Throwable e : exceptions) {
                log.error("Async test exception", e);
            }
            fail("Async exceptions have occurred (see log)");
        }
    }

    private void testConnectionSetupBehaviourBeforePotentialServerShutdown(VirtualInstance client, VirtualInstance server,
        ConnectionSetupServiceImpl clientConnectionSetupService, ConnectionSetup setupC2S, ConnectionSetupStateTracker tracker)
        throws InterruptedException,
        AssertionError, TimeoutException {

        // check initial, unconnected state
        assertEquals(1, clientConnectionSetupService.getAllConnectionSetups().size());
        assertEquals(setupC2S, clientConnectionSetupService.getAllConnectionSetups().iterator().next());
        assertEquals(ConnectionSetupState.DISCONNECTED, setupC2S.getState());
        assertNull(setupC2S.getCurrentChannelId());
        assertNull(setupC2S.getLastChannelId());

        setupC2S.signalStartIntent();

        tracker.awaitAndExpect(ConnectionSetupState.CONNECTING, CONNECTION_OPERATION_WAIT_MSEC);
        tracker.awaitAndExpect(ConnectionSetupState.CONNECTED, CONNECTION_OPERATION_WAIT_MSEC);

        assertEquals(1, clientConnectionSetupService.getAllConnectionSetups().size());
        assertEquals(ConnectionSetupState.CONNECTED, setupC2S.getState());
        assertNotNull(setupC2S.getCurrentChannelId());
        assertNotNull(setupC2S.getLastChannelId());

        String firstChannelId = setupC2S.getCurrentChannelId();

        setupC2S.signalStopIntent();

        tracker.awaitAndExpect(ConnectionSetupState.DISCONNECTING, CONNECTION_OPERATION_WAIT_MSEC);
        tracker.awaitAndExpect(ConnectionSetupState.DISCONNECTED, CONNECTION_OPERATION_WAIT_MSEC);

        assertEquals(ConnectionSetupState.DISCONNECTED, setupC2S.getState());
        assertEquals(DisconnectReason.ACTIVE_SHUTDOWN, setupC2S.getDisconnectReason());
        assertNull(setupC2S.getCurrentChannelId());
        // last channel id should still be present, e.g. for debugging
        assertNotNull(setupC2S.getLastChannelId());
        assertTrue(setupC2S.getLastChannelId().equals(firstChannelId));

        // reconnect
        setupC2S.signalStartIntent();

        tracker.awaitAndExpect(ConnectionSetupState.CONNECTING, CONNECTION_OPERATION_WAIT_MSEC);
        tracker.awaitAndExpect(ConnectionSetupState.CONNECTED, CONNECTION_OPERATION_WAIT_MSEC);

        assertEquals(ConnectionSetupState.CONNECTED, setupC2S.getState());
        assertNotNull(setupC2S.getCurrentChannelId());
        assertNotNull(setupC2S.getLastChannelId());
        // ensure the connection setup received a new channel id after reconnect
        assertFalse(setupC2S.getLastChannelId().equals(firstChannelId));
    }

    private void testConnectionSetupBehaviourAfterPotentialServerShutdown(VirtualInstance client,
        VirtualInstance server, ConnectionSetupServiceImpl clientConnectionSetupService, ConnectionSetup setupC2S,
        ConnectionSetupStateTracker tracker) throws InterruptedException, AssertionError, TimeoutException {

        tracker.awaitAndExpect(ConnectionSetupState.DISCONNECTED, CONNECTION_OPERATION_WAIT_MSEC);
        assertEquals(ConnectionSetupState.DISCONNECTED, setupC2S.getState());
        assertNull(setupC2S.getCurrentChannelId());
        assertNotNull(setupC2S.getLastChannelId());

        if (usingDuplexTransport) {
            assertEquals(DisconnectReason.REMOTE_SHUTDOWN, setupC2S.getDisconnectReason());
        } else {
            assertEquals(DisconnectReason.ACTIVE_SHUTDOWN, setupC2S.getDisconnectReason());
        }
    }

    /**
     * Tests the effect of the {@link NodeConfigurationService#isRelay()} setting (which is read from
     * {@link ConfigurationService#getIsRelay()} in live instances).
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testRelayBehaviour() throws Exception {
        // create instances manually to control the "is relay" settings
        VirtualInstance c1 = new VirtualInstance("c1", false); // actually irrelevant
        VirtualInstance c2 = new VirtualInstance("c2", false);
        VirtualInstance c3 = new VirtualInstance("c3", false); // actually irrelevant
        VirtualInstance s1 = new VirtualInstance("s1", true);
        VirtualInstance s2 = new VirtualInstance("s2", true);

        VirtualInstanceGroup allNodes = new VirtualInstanceGroup(c1, c2, c3, s1, s2);
        allInstances = allNodes.toArray();
        for (VirtualInstance instance : allInstances) {
            instance.addServerConfigurationEntry(contactPointGenerator.createContactPoint());
        }
        allNodes.registerNetworkTransportProvider(transportProvider);
        allNodes.addNetworkTrafficListener(getGlobalTrafficListener());
        testTopology = new VirtualTopology(allInstances);
        testTopology.setConnectBothDirectionsByDefaultFlag(!usingDuplexTransport);
        allNodes.start();

        // connect to an "M" shape
        prepareWaitForNextMessage();
        testTopology.connect(0, 3); // c1-s1
        testTopology.connect(1, 3); // c2-s1
        testTopology.connect(1, 4); // c2-s2
        testTopology.connect(2, 4); // c3-s2
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance instance : allInstances) {
            log.debug(NetworkFormatter.networkGraphToGraphviz(instance.getRawNetworkGraph(), true));
        }

        VirtualInstance[][] expectedNonTrivialVisibilities = new VirtualInstance[][] {
            { c1, s1 },
            { c1, c2 },
            { c2, c1 },
            { c2, s1 },
            { c2, s2 },
            { c2, c3 },
            { c3, c2 },
            { c3, s2 },
            { s1, c1 },
            { s1, c2 },
            { s2, c2 },
            { s2, c3 }
        };

        // create visibility matrix
        Map<VirtualInstance, Map<VirtualInstance, Boolean>> visibilityMatrix =
            new HashMap<VirtualInstance, Map<VirtualInstance, Boolean>>();
        for (VirtualInstance i1 : allInstances) {
            Map<VirtualInstance, Boolean> singleInstanceVisMap = new HashMap<VirtualInstance, Boolean>();
            visibilityMatrix.put(i1, singleInstanceVisMap);
            for (VirtualInstance i2 : allInstances) {
                // initialize with trivial visibility
                singleInstanceVisMap.put(i2, (i1 == i2));
            }
        }
        // add non-trivial visibilities
        for (VirtualInstance[] entry : expectedNonTrivialVisibilities) {
            visibilityMatrix.get(entry[0]).put(entry[1], Boolean.TRUE);
        }

        // test network graph visibility
        for (VirtualInstance i1 : allInstances) {
            for (VirtualInstance i2 : allInstances) {
                boolean expected = visibilityMatrix.get(i1).get(i2);
                assertEquals("Raw network graph visibility: " + i1.getInstanceNodeSessionId() + " ->" + i2.getInstanceNodeSessionId(),
                    expected, i1.getRawNetworkGraph().getNodeIds().contains(i2.getInstanceNodeSessionId()));
            }
        }

        // verify expected visible node counts (as a cross-check)
        Set<InstanceNodeSessionId> c1VisibleNodes = c1.getRawNetworkGraph().getNodeIds();
        Set<InstanceNodeSessionId> c2VisibleNodes = c2.getRawNetworkGraph().getNodeIds();
        Set<InstanceNodeSessionId> c3VisibleNodes = c3.getRawNetworkGraph().getNodeIds();
        Set<InstanceNodeSessionId> s1VisibleNodes = s1.getRawNetworkGraph().getNodeIds();
        Set<InstanceNodeSessionId> s2VisibleNodes = s2.getRawNetworkGraph().getNodeIds();
        assertEquals(3, c1VisibleNodes.size());
        assertEquals(3, s1VisibleNodes.size());
        assertEquals(5, c2VisibleNodes.size());
        assertEquals(3, c3VisibleNodes.size());
        assertEquals(3, s2VisibleNodes.size());

        // test messaging
        for (VirtualInstance i1 : allInstances) {
            for (VirtualInstance i2 : allInstances) {
                // local-to-local routing is not allowed, so do not test it
                if (i1 == i2) {
                    continue;
                }
                boolean expected = visibilityMatrix.get(i1).get(i2);
                // perform a standard routed request; while this is a good test of the "positive" allowed routing paths,
                // the "negative" (forbidden) paths are not really tested as the network graph should simply not contain
                // the target node, which was already tested above - misc_ro
                // TODO add test that non-relays actually refuse to forward requests
                NetworkResponse response =
                    i1.performRoutedRequest("test", ProtocolConstants.VALUE_MESSAGE_TYPE_TEST, i2.getInstanceNodeSessionId());
                boolean success = response.isSuccess();
                assertEquals("Messaging result: " + i1.getInstanceNodeSessionId() + " ->" + i2.getInstanceNodeSessionId(), expected,
                    success);
            }
        }

        // test node property visibilty;
        for (VirtualInstance i1 : allInstances) {
            Map<InstanceNodeSessionId, Map<String, String>> allNodeProperties = i1.getNodePropertiesService().getAllNodeProperties();
            assertEquals("Node " + i1 + " knows a different number of nodes with published properties "
                + "than its total number known network nodes", i1.getRawNetworkGraph().getNodeCount(), allNodeProperties.size());
            // check that all node id properties (simplest differing one to test) are actually visible
            for (Entry<InstanceNodeSessionId, Map<String, String>> e : allNodeProperties.entrySet()) {
                InstanceNodeSessionId targetNodeId = e.getKey();
                Map<String, String> targetProperties = e.getValue();
                assertEquals(targetNodeId.getInstanceNodeSessionIdString(), targetProperties.get(NodePropertyConstants.KEY_NODE_ID));
            }
        }

        prepareWaitForNextMessage();
        allNodes.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();
    }

    private void logIteration(int i) {
        log.debug("Starting iteration " + i);
    }

}
