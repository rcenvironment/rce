/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.channel.MessageChannelLifecycleListener;
import de.rcenvironment.core.communication.channel.MessageChannelService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.model.NetworkContactPoint;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.NetworkContactPointGenerator;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceGroup;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.communication.transport.spi.NetworkTransportProvider;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;

/**
 * Unit tests for {@link LinkStateRoutingProtocolManager}.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
// TODO rework to general topology tests? - misc_ro
public class RoutingProtocolTest extends AbstractVirtualInstanceTest {

    private static final int DEFAULT_REQUEST_TIMEOUT = 10000;

    // test size 5 for fast/standard testing, 10 for extended testing
    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(4, 10);

    private static final String CONNECTION_ID_1 = "#1";

    private static final String FAILED_TO_CONNECT = "Failed to connect.";

    // private static final String NETWORK_NOT_FULLY_CONVERGED = "The network graph of every instance is supposed to be the same.";

    /**
     * Create a chain of instances that are not connected in a ring.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testChainOfInstances() throws Exception {
        setupInstances(TEST_SIZE, false, true);

        prepareWaitForNextMessage();
        instanceUtils.connectToChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        // TODO actually verify topology state
    }

    /**
     * Create instances that are connected in a ring.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testRingOfInstances() throws Exception {
        setupInstances(TEST_SIZE, false, true);

        for (VirtualInstance vi : allInstances) {
            assertFalse("The network graph of any instance is not supposed to be the same.",
                instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
        }

        prepareWaitForNextMessage();
        instanceUtils.connectToRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        try {
            assertAllInstancesKnowSameTopology();
        } catch (AssertionError e) {
            throw new RuntimeException(e);
        }

        // arbitrary number of iterations; using test size
        for (int i = 0; i < TEST_SIZE; i++) {
            VirtualInstance sender = instanceUtils.getRandomInstance(allInstances);
            VirtualInstance receiver = instanceUtils.getRandomInstance(allInstances, sender);

            // check a random route
            assertTrue(StringUtils.format(ERROR_MSG_A_NO_ROUTE_TO_B, sender, receiver), sender.getRouteTo(receiver) != null);
        }
    }

    /**
     * Test if stopping and restarting all virtual instances works smoothly.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testStartAndStop() throws Exception {

        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        setupInstances(TEST_SIZE, false, true);
        VirtualInstanceGroup group = new VirtualInstanceGroup(allInstances);

        prepareWaitForNextMessage();
        instanceUtils.connectToRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertAllInstancesKnowSameTopology();

        prepareWaitForNextMessage();
        group.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();

        prepareWaitForNextMessage();
        group.start();
        waitForNextMessage();
        waitForNetworkSilence();

        assertAllInstancesKnowSameTopology();
    }

    /**
     * Add initial network peer afterwards and check if everything went ok.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testClosingChainToRing() throws Exception {
        setupInstances(TEST_SIZE, false, true);

        VirtualInstance firstInstance = allInstances[0];
        VirtualInstance lastInstance = allInstances[allInstances.length - 1];

        prepareWaitForNextMessage();
        instanceUtils.connectToChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        // connect last and first instance to create a ring
        prepareWaitForNextMessage();
        firstInstance.connectAsync(lastInstance.getConfigurationService().getServerContactPoints().get(0));
        waitForNextMessage();
        waitForNetworkSilence();

        // TODO This should not fail.
        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));

        List<? extends NetworkGraphLink> route = firstInstance.getRouteTo(lastInstance);
        assertTrue(StringUtils.format(ERROR_MSG_A_NO_ROUTE_TO_B, firstInstance, lastInstance), route != null);
        assertEquals(1, route.size());
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testCrashingNodeInSingleDirectionRing() throws Exception {

        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        setupInstances(TEST_SIZE, false, true);

        prepareWaitForNextMessage();
        testTopology.connectToRing(false); // single-direction ring
        waitForNextMessage();
        waitForNetworkSilence();

        VirtualInstance firstNode = allInstances[0];
        VirtualInstance failingNodePredecessor = allInstances[allInstances.length / 2 - 1];
        VirtualInstance failingNode = allInstances[allInstances.length / 2];
        VirtualInstance lastNode = allInstances[allInstances.length - 1];

        // verify initial message sending
        String dummyContent = instanceUtils.generateUniqueMessageToken();
        assertTrue(firstNode.performRoutedRequest(dummyContent, lastNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT).isSuccess());

        // failure
        failingNode.simulateCrash();

        // there should still be a link in the topology map of the predecessor
        assertTrue(failingNodePredecessor.knownTopologyContainsLinkTo(failingNode));

        // sender should falsely believe that there is a route.
        assertTrue(StringUtils.format(ERROR_MSG_A_NO_ROUTE_TO_B, firstNode, lastNode), firstNode.getRouteTo(lastNode) != null);

        dummyContent = instanceUtils.generateUniqueMessageToken();

        prepareWaitForNextMessage();
        // sending to the predecessor should still work normally
        assertTrue(firstNode.performRoutedRequest(dummyContent, failingNodePredecessor.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT)
            .isSuccess());
        // sending across the whole chain should cause the predecessor to notice the crashed node
        // TODO verify location of routing failure
        assertFalse(firstNode.performRoutedRequest(dummyContent, lastNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT).isSuccess());
        // sending a message to the failed node should fail as well
        assertFalse(
            firstNode.performRoutedRequest(dummyContent, failingNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT).isSuccess());
        // sending to the predecessor should still work normally
        assertTrue(firstNode.performRoutedRequest(dummyContent, failingNodePredecessor.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT)
            .isSuccess());
        waitForNextMessage();
        waitForNetworkSilence();

        // the link from the predecessor to the crashed node should now be gone
        // TODO necessary to ensure thread visibility here?
        assertFalse("Topology link should be gone after failed delivery",
            failingNodePredecessor.knownTopologyContainsLinkTo(failingNode));

        prepareWaitForNextMessage();
        // restart the crashed node
        failingNode.start();
        // TODO reconnect actively until event-driven reconnect is available
        failingNodePredecessor.connectAsync(failingNode.getConfigurationService().getServerContactPoints().get(0));
        waitForNextMessage();
        waitForNetworkSilence();

        assertAllInstancesKnowSameTopology();

        // the channel should now be there again
        assertTrue(failingNodePredecessor.knownTopologyContainsLinkTo(failingNode));

        assertTrue(StringUtils.format(ERROR_MSG_A_NO_ROUTE_TO_B, firstNode, lastNode), firstNode.getRouteTo(lastNode) != null);

        dummyContent = instanceUtils.generateUniqueMessageToken();

        prepareWaitForNextMessage();
        // this should not fail anymore
        assertTrue(firstNode.performRoutedRequest(dummyContent, failingNodePredecessor.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT)
            .isSuccess());
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * Send a simple routed message.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testRoutedMessage() throws Exception {
        setupInstances(TEST_SIZE, false, true);

        String uniqueMessageToken = instanceUtils.generateUniqueMessageToken();
        VirtualInstance sender = allInstances[0];
        VirtualInstance receiver = allInstances[allInstances.length - 1];

        prepareWaitForNextMessage();
        instanceUtils.connectToRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        prepareWaitForNextMessage();
        try {
            // log.debug(allInstances[0].getFormattedNetworkGraph());
            NetworkResponse response =
                sender.performRoutedRequest(uniqueMessageToken, receiver.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertTrue("Request failed: " + response.getResultCode(), response.isSuccess());
        } catch (CommunicationException e) {
            fail(e.getMessage());
        }
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * Initiate LSAs randomly and wait for the network to converge.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    @Deprecated
    // TODO old test; review and probably remove - misc_ro, Nov 2013
    public void testDiscoveryByRandomLSAs() throws Exception {
        final int maxIterations = TEST_SIZE * 10;
        setupInstances(TEST_SIZE, false, true);

        String uniqueMessageToken = instanceUtils.generateUniqueMessageToken();

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleStarTopology(Arrays.copyOfRange(allInstances, 0, TEST_SIZE / 2 - 1));
        instanceUtils.connectToDoubleStarTopology(Arrays.copyOfRange(allInstances, TEST_SIZE / 2, TEST_SIZE - 1));
        instanceUtils.randomlyConcatenateTopologies(
            Arrays.copyOfRange(allInstances, 0, TEST_SIZE / 2 - 1),
            Arrays.copyOfRange(allInstances, TEST_SIZE / 2, TEST_SIZE - 1));

        waitForNextMessage();
        waitForNetworkSilence();

        int j = 0;
        int max = maxIterations;
        boolean fullConvercenceForEveryone = false;

        // send LSAs randomly until every instance reports that the network knowledge is the same
        // everywhere
        while (!fullConvercenceForEveryone && j++ < max) {

            VirtualInstance randomInstance = instanceUtils.getRandomInstance(allInstances);

            prepareWaitForNextMessage();
            // NOTE: commented out as the target method has been removed
            // randomInstance.getRoutingService().getProtocolManager().broadcastLsa();
            // FIXME there should always be traffic; check
            waitForNextMessage();
            waitForNetworkSilence();

            fullConvercenceForEveryone = true;
            for (VirtualInstance vi : allInstances) {
                fullConvercenceForEveryone &= vi.hasSameTopologyHashesForAllNodes();
            }
            // log.info("Iteration " + j + ": " + randomInstance.getFormattedNetworkGraph());
        }

        assertTrue("Networks did not converge after the maximum of " + max + " iterations", j < max);

        // TODO perform several iterations
        VirtualInstance sender = instanceUtils.getRandomInstance(allInstances);
        VirtualInstance receiver = instanceUtils.getRandomInstance(allInstances, sender);

        prepareWaitForNextMessage();
        // log.info(sender.getFormattedNetworkGraph());
        assertTrue(
            sender.performRoutedRequest(uniqueMessageToken, receiver.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT).isSuccess());
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * Assert that instance ids, instance session ids, and logical node ids are not registered as separate nodes.
     * 
     * @throws InterruptedException not expected
     */
    @Test
    public void testSingleClientRoutingTopology() throws InterruptedException {
        VirtualInstance client1 = new VirtualInstance("Client1");
        final int shortWaitTime = 100;
        Thread.sleep(shortWaitTime); // give async registration tasks time to complete
        assertEquals(1, client1.getRawNetworkGraph().getNodeIds().size());
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testTwoClientsTalkingToEachOther() throws Exception {

        VirtualTransportTestConfiguration testConfiguration = new VirtualTransportTestConfiguration(false);
        NetworkTransportProvider transportProvider = testConfiguration.getTransportProvider();
        NetworkContactPointGenerator contactPointGenerator = testConfiguration.getContactPointGenerator();

        // create two virtual instances.
        VirtualInstance client1 = new VirtualInstance("Client1");
        VirtualInstance client2 = new VirtualInstance("Client2");

        addGlobalTrafficListener(new VirtualInstanceGroup(client1));
        addGlobalTrafficListener(new VirtualInstanceGroup(client2));

        // add the "virtual" transport method to the connection service
        client1.registerNetworkTransportProvider(transportProvider);
        client2.registerNetworkTransportProvider(transportProvider);

        // create a network contact point and make client 1 available under this contact
        NetworkContactPoint client1ContactPoint = contactPointGenerator.createContactPoint();
        client1.addServerConfigurationEntry(client1ContactPoint);

        // create a network contact point and make client 2 available under this contact
        NetworkContactPoint client2ContactPoint = contactPointGenerator.createContactPoint();
        client2.addServerConfigurationEntry(client2ContactPoint);

        MessageChannelLifecycleListener connectionListener = EasyMock.createMock(MessageChannelLifecycleListener.class);
        // FIXME why does this not work?
        // connectionListener.onOutgoingConnectionEstablished(EasyMock.createMock(MessageChannel.class));
        // connectionListener.onOutgoingConnectionEstablished(EasyMock.createMock(MessageChannel.class));
        EasyMock.replay(connectionListener);
        client1.getMessageChannelService().addChannelLifecycleListener(connectionListener);

        // network graphs should not be the same
        assertFalse(instancesHaveSameUnfilteredNetworkGraph(client1, client2));

        try {
            // startup instances
            client1.start();
            client2.start();
        } catch (InterruptedException e) {
            fail("Failed to start virtual instances.");
        }

        // network graphs should not be the same
        assertFalse(instancesHaveSameUnfilteredNetworkGraph(client1, client2));

        prepareWaitForNextMessage();
        client1.connectAsync(client2ContactPoint);
        client2.connectAsync(client1ContactPoint);
        waitForNextMessage();
        waitForNetworkSilence();

        // network graphs should now be the same
        assertTrue(instancesHaveSameUnfilteredNetworkGraph(client1, client2));

        EasyMock.verify(connectionListener);
    }

    /**
     * TODO This test might not be needed any more.
     * 
     * @throws Exception on uncaught exceptions
     */
    @SuppressWarnings("unused")
    // Currently not used code below remains commented in to be sensitive to refactoring.
    // Dead code warning should be suppressed anyways.
    @Test
    public void testAClientTalkingToAServer() throws Exception {
        // TODO recheck test; hangs after reworking startup mechanism; adapt comment at annotation "SuppressWarnings" above
        if (true) {
            // deactivating in JUnit 3
            return;
        }

        // currently not used code
        VirtualTransportTestConfiguration testConfiguration = new VirtualTransportTestConfiguration(true);
        NetworkTransportProvider transportProvider = testConfiguration.getTransportProvider();
        NetworkContactPointGenerator contactPointGenerator = testConfiguration.getContactPointGenerator();

        // create two virtual instances.
        VirtualInstance client = new VirtualInstance("The Client");
        VirtualInstance server = new VirtualInstance("The Server");

        // add the "virtual" transport method to the connection service
        client.registerNetworkTransportProvider(transportProvider);
        server.registerNetworkTransportProvider(transportProvider);

        // create a network contact point and make client 1 available under this contact
        NetworkContactPoint client1ContactPoint = contactPointGenerator.createContactPoint();
        client.addServerConfigurationEntry(client1ContactPoint);

        // create a network contact point and make client 2 available under this contact
        NetworkContactPoint serverContactPoint = contactPointGenerator.createContactPoint();
        server.addServerConfigurationEntry(serverContactPoint);

        // client 1 should know client 2 from the beginning
        // client1.addInitialNetworkPeer(client2ContactPoint);
        // client 2 should know client 1 from the beginning
        server.addInitialNetworkPeer(client1ContactPoint);
        // server.getConnectionService().addConnectionListener((NetworkConnectionListener) new
        // NetworkRoutingServiceImpl());

        // start
        client.start();
        server.start();

        try {
            // client connecting to server
            MessageChannelService messageChannelService = client.getMessageChannelService();
            Future<MessageChannel> connection = messageChannelService.connect(serverContactPoint, true);
            MessageChannel clientToServer = connection.get();
            messageChannelService.registerNewOutgoingChannel(clientToServer);

            assertEquals(
                server.getConfigurationService().getInitialNodeInformation().getInstanceNodeSessionId(),
                clientToServer.getRemoteNodeInformation().getInstanceNodeSessionId());

            // send messages
            client.performRoutedRequest("hello", server.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);

        } catch (CommunicationException e) {
            fail(FAILED_TO_CONNECT);
        } catch (InterruptedException e) {
            fail(FAILED_TO_CONNECT);
        } catch (ExecutionException e) {
            fail(FAILED_TO_CONNECT);
        }

        client.shutDown();
        server.shutDown();
    }

    /**
     * TODO This test might not be needed any more.
     * 
     * Test graph updates with different {@link LinkStateAdvertisement}s.
     */
    @Test
    @Ignore
    @Deprecated
    // TODO old test; review and probably delete - misc_ro, Nov 2013
    public void testLsaIntegration1() {

        VirtualInstance instance1 = new VirtualInstance("The Instance 1", true);
        VirtualInstance instance2 = new VirtualInstance("The Instance 2", true);

        final InstanceNodeSessionId instanceSessionId1 = instance1.getInstanceNodeSessionId();

        final InstanceNodeSessionId instanceSessionId2 = instance2.getInstanceNodeSessionId();

        final InstanceNodeSessionId instanceSessionId3 = NodeIdentifierTestUtils
            .createTestInstanceNodeSessionIdWithDisplayName("testnode3");

        // TODO Not such a nice workaround to get a connection service instance.
        LinkStateRoutingProtocolManager protocolManager1 =
            instance1.getRoutingService().getProtocolManager();
        // new LinkStateRoutingProtocolManager(new InitialNodeInformationImpl(NODE_1),
        // instance1.getConnectionService());
        LinkStateRoutingProtocolManager protocolManager2 =
            instance2.getRoutingService().getProtocolManager();
        // new LinkStateRoutingProtocolManager(new InitialNodeInformationImpl(NODE_2),
        // instance2.getConnectionService());

        protocolManager1.getTopologyMap().addLink(instanceSessionId1, instanceSessionId3, CONNECTION_ID_1);
        protocolManager2.getTopologyMap().addLink(instanceSessionId3, instanceSessionId1, CONNECTION_ID_1);

        assertFalse(protocolManager1.getTopologyMap().equals(protocolManager2.getTopologyMap()));

        // TODO Write more tests here without using an actual virtual connection.
        // protocol1.scanNetworkContacts();
        // protocol2.scanNetworkContacts();

        // protocol1.sendLinkStateAdvertisement();
        // protocol2.sendLinkStateAdvertisement();

        // assertEquals(protocol1.getNetworkGraph(), protocol2.getNetworkGraph());
    }

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(false);
    }
}
