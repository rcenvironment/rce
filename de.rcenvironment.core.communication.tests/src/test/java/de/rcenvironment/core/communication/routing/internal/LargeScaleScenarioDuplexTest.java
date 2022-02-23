/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.TestNetworkRequestHandler;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.testutils.VirtualInstanceState;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;

/**
 * @author Robert Mischke
 * @author Phillip Kroll
 */
public class LargeScaleScenarioDuplexTest extends AbstractLargeScaleTest {

    private static final String DUMMY_REQUEST_CONTENT = "hello";

    @Deprecated
    private static final int TEMP_ARBITRARY_WAIT = 50;

    private static final int DEFAULT_REQUEST_TIMEOUT = 10000;

    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(3, 10);

    private static final int EPOCHS = CommonTestOptions.selectStandardOrExtendedValue(2, 3);

    /**
     * @throws Exception on uncaught exceptions
     */
    @BeforeClass
    public static void setTestParameters() throws Exception {
        testSize = TEST_SIZE;
        epochs = EPOCHS;
    }

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(true);
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Deprecated
    public void testIterativelyGrowingLinearNetwork() throws Exception {

        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;

            prepareWaitForNextMessage();
            instanceUtils.concatenateInstances(allInstances, i, newInstanceIndex);
            waitForNextMessage();
            waitForNetworkSilence();

            // note: the third parameter in Arrays.copyOfRange() is exclusive and must be "last + 1"
            assertTrue("Instances did not converge at i=" + i,
                instanceUtils.allInstancesHaveSameRawNetworkGraph(Arrays.copyOfRange(allInstances, 0, newInstanceIndex + 1)));
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Deprecated
    public void testConcurrentlyGrowingLinearNetwork() throws Exception {

        prepareWaitForNextMessage();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            instanceUtils.concatenateInstances(allInstances, i, i + 1);
        }
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));

    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Deprecated
    public void testIterativelyGrowingRandomNetwork() throws Exception {

        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int newInstanceIndex = i + 1;
            prepareWaitForNextMessage();
            int connectedInstanceIndex = random.nextInt(newInstanceIndex); // 0..newInstanceIndex-1
            instanceUtils.concatenateInstances(allInstances, connectedInstanceIndex, newInstanceIndex);
            waitForNextMessage();
            waitForNetworkSilence();

            // note: the third parameter in Arrays.copyOfRange() is exclusive and must be "last + 1"
            assertTrue("Instances did not converge at i=" + i,
                instanceUtils.allInstancesHaveSameRawNetworkGraph(Arrays.copyOfRange(allInstances, 0, newInstanceIndex + 1)));
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Deprecated
    public void testConcurrentlyGrowingRandomNetwork() throws Exception {

        prepareWaitForNextMessage();
        Random random = new Random();
        for (int i = 0; i < allInstances.length - 1; i++) {
            logIteration(i);
            int connectedInstanceIndex = random.nextInt(i + 1); // 0..i
            instanceUtils.concatenateInstances(allInstances, connectedInstanceIndex, i + 1);
        }
        waitForNextMessage();
        waitForNetworkSilence();
        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * Test a simple ring topology. In contrast to the non-duplex test, a double-connected ring should result, with N*2 channels.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testLargeDuplexRingTopology() throws Exception {

        log.info("Topology: ring");

        prepareWaitForNextMessage();
        instanceUtils.connectToRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize * 2);
        }
    }

    /**
     * Test a simple chain topology. Although connections are only initiated in one direction, a double-connected chain should result.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testLargeDuplexChainTopology() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize * 2 - 2);
        }
    }

    /**
     * Handle large network.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testInwardDuplexStarTopology() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToInwardStarTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
            assertNodeAndLinkCount(vi, testSize, (testSize - 1) * 2);
        }

        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * Compute many routes.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    // FIXME test does not succeed; review -- misc_ro, Dec 2012
    public void testComputeManyRoutes() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
            assertNodeAndLinkCount(vi, testSize, testSize * 2);
        }

        for (int i = 0; i < testSize; i++) {
            // compute a random route
            VirtualInstance vi1 = instanceUtils.getRandomInstance(allInstances);
            VirtualInstance vi2 = instanceUtils.getRandomInstance(allInstances, vi1);

            List<? extends NetworkGraphLink> route = vi1.getRouteTo(vi2);

            // Route should exist
            assertTrue(StringUtils.format("Could not find a valid route from %s to %s",
                vi1.getInstanceNodeSessionId(),
                vi2.getInstanceNodeSessionId()),
                route != null);

            // Route should not be larger than the number of nodes in the network
            assertTrue(route.size() < instanceUtils.getRandomInstance(allInstances).getKnownNodeCount());
        }
    }

    /**
     * Tests shutdown/restart recovery in a duplex ring topology.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    // FIXME test does not succeed; review -- misc_ro, Dec 2012
    public void testDuplexRingFailureRecovery() throws Exception {

        // this test needs instances to keep their peer config after a stop/start cycle -- misc_ro
        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        prepareWaitForNextMessage();
        instanceUtils.connectToRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        // iterate over epochs randomly generated scenarios
        for (int i = 0; i < epochs; i++) {

            String message = instanceUtils.generateUniqueMessageToken();

            VirtualInstance failingInstance = instanceUtils.getRandomInstance(allInstances);
            VirtualInstance sender = instanceUtils.getRandomInstance(allInstances, failingInstance);
            VirtualInstance receiver = instanceUtils.getRandomInstance(allInstances, sender, failingInstance);

            log.info("Stating i=" + i + "; instance to restart is " + failingInstance.getInstanceNodeSessionId());

            // check that everything is normal (e.g. ring topology)
            for (VirtualInstance vi : allInstances) {
                // TODO check removal of passive connections on shutdown
                assertNodeAndLinkCount(vi, testSize, testSize * 2);
            }

            prepareWaitForNextMessage();
            failingInstance.shutDown();
            waitForNextMessage();
            waitForNetworkSilence();

            // now every instance is expected to know that a node is missing
            for (VirtualInstance vi : allInstances) {
                // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
                if (!vi.equals(failingInstance)) {
                    assertNodeAndLinkCount(vi, testSize - 1, testSize * 2 - 4);
                }
            }

            log.debug(StringUtils.format("%s attempts to communicate with %s.",
                instanceUtils.getFormattedName(sender),
                instanceUtils.getFormattedName(receiver)));

            NetworkResponse response = sender.performRoutedRequest(message, receiver.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertTrue("Could not send a message", response.isSuccess());

            assertTrue(
                StringUtils.format("%s failed to send routed message to %s. \n\n %s",
                    instanceUtils.getFormattedName(sender),
                    instanceUtils.getFormattedName(receiver),
                    sender.getFormattedLegacyNetworkGraph()),
                receiver.checkMessageReceivedByContent(message));
            // waitSomeTime(instances.length);

            log.debug("starting up: " + instanceUtils.getFormattedName(failingInstance));

            prepareWaitForNextMessage();
            failingInstance.start();
            waitForNextMessage();
            waitForNetworkSilence();

            log.info(failingInstance.getFormattedNetworkStats());

        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testDuplexChainFailureRecovery() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        // iterate over epochs randomly generated scenarios
        for (int i = 0; i < epochs; i++) {
            log.debug("Epoche: " + i);

            String messageContent = instanceUtils.generateUniqueMessageToken();
            int failingIndex = randomGenerator.nextInt(testSize);
            VirtualInstance failingNode = allInstances[failingIndex];
            VirtualInstance firstNode = allInstances[0];
            VirtualInstance lastNode = allInstances[testSize - 1];

            NetworkResponse response;

            response = firstNode.performRoutedRequest(messageContent, lastNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertTrue("Request failed: " + response.getResultCode(), response.isSuccess());

            // shutdown
            prepareWaitForNextMessage();
            failingNode.shutDown();
            waitForNextMessage();
            waitForNetworkSilence();

            response = firstNode.performRoutedRequest(messageContent, lastNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertFalse(response.toString(), response.isSuccess());

            // startup
            prepareWaitForNextMessage();
            failingNode.start();
            // FIXME this should not be necessary; investigate
            Thread.sleep(TEMP_ARBITRARY_WAIT);
            assertEquals(VirtualInstanceState.STARTED, failingNode.getCurrentState());
            // connect outwards into both directions; this differs from the original setup, but
            // should result in the same topology due to duplex connections being used -- misc_ro
            if (failingIndex > 0) {
                testTopology.connect(failingIndex, failingIndex - 1);
            }
            if (failingIndex < testSize - 1) {
                testTopology.connect(failingIndex, failingIndex + 1);
            }
            waitForNextMessage();
            waitForNetworkSilence();

            // send a message
            response = firstNode.performRoutedRequest(messageContent, lastNode.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertTrue(response.toString(), response.isSuccess());

            // LOGGER.info(failingInstance.getFormattedNetworkStats());
            log.info(failingNode.getFormattedLegacyNetworkGraph());
        }

    }

    /**
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testMessageConfirmation() throws Exception {
        log.info("Topology: double ring");

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        log.info(allInstances[0].getFormattedLegacyNetworkGraph());

        // note: added n repetitions to this test; arbitrary value
        for (int i = 0; i < testSize; i++) {

            long trafficCountBefore = getGlobalRequestCount();

            VirtualInstance sender = instanceUtils.getRandomInstance(allInstances);
            VirtualInstance receiver = instanceUtils.getRandomInstance(allInstances, sender);
            // old:
            // String id = sender.sendRoutedAsyncMessage("hello", receiver);
            // sender.getRoutingService().getProtocol().waitForMessageConfirmation(id);
            String requestBody = DUMMY_REQUEST_CONTENT + 1;
            InstanceNodeSessionId receiverId = receiver.getInstanceNodeSessionId();
            // Systemx.out.println(receiverId);
            NetworkResponse response = sender.performRoutedRequest(requestBody, receiverId, DEFAULT_REQUEST_TIMEOUT);
            assertNotNull("Unexpected null response", response);
            assertTrue("Received failure response; code=" + response.getResultCode(), response.isSuccess());
            assertEquals(TestNetworkRequestHandler.getTestResponse(requestBody, receiverId),
                response.getDeserializedContent());

            log.debug("Routed message delivered and confirmed; registered " + (getGlobalRequestCount() - trafficCountBefore)
                + " new network requests");
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testMessageFailure() throws Exception {

        log.info("Topology: double ring");

        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        // create topology
        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        VirtualInstance failingInstance = instanceUtils.getRandomInstance(allInstances);
        VirtualInstance sender = instanceUtils.getRandomInstance(allInstances, failingInstance);

        // this should work
        NetworkResponse response =
            sender.performRoutedRequest(DUMMY_REQUEST_CONTENT, failingInstance.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
        assertTrue("Initial routed request failed", response.isSuccess());

        prepareWaitForNextMessage();
        failingInstance.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();

        try {
            response =
                sender.performRoutedRequest(DUMMY_REQUEST_CONTENT, failingInstance.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            // this should fail, because instance is shut down.
            assertFalse(NetworkFormatter.networkResponseToString(response), response.isSuccess());
        } catch (CommunicationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("could not find"));
        }

        prepareWaitForNextMessage();
        failingInstance.start();
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    // FIXME test does not succeed; review -- misc_ro, Dec 2012
    public void testDisconnectedNetworkParts() throws Exception {

        final int chainSize = testSize / 4;
        log.info("Topology: two rings connected with a chain.");

        prepareWaitForNextMessage();
        // TODO review: check topology here; not obvious how this relates to the description
        instanceUtils.connectToDoubleRingTopology(allInstances);
        instanceUtils.connectToDoubleRingTopology(allInstances, 0, testSize / 2 - chainSize / 2);
        instanceUtils.connectToDoubleChainTopology(allInstances, testSize / 2 - (chainSize / 2
            - 1), testSize / 2 + (chainSize / 2 - 1));
        instanceUtils.connectToDoubleRingTopology(allInstances, testSize / 2 + chainSize / 2,
            testSize - 1);
        instanceUtils.doubleConcatenateInstances(allInstances, testSize / 2 - chainSize / 2,
            testSize / 2 - (chainSize / 2 - 1));
        instanceUtils.doubleConcatenateInstances(allInstances, testSize / 2 + chainSize / 2,
            testSize / 2 + (chainSize / 2 - 1));
        waitForNextMessage();
        waitForNetworkSilence();

        // for (VirtualInstance vi : allInstances) {
        // TODO Include this test.
        // assertEquals(NUMBER_OF_NODES, testSize, vi.getTopologyMap().getNodeCount());
        // assertEquals(NUMBER_OF_CHANNELS, testSize * 2 + 2,
        // vi.getTopologyMap().getChannelCount());
        // }

        VirtualInstance failingInstance = allInstances[testSize / 2];

        prepareWaitForNextMessage();
        failingInstance.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();

        NetworkResponse response;
        // TODO clarify failure response vs. exception behaviour for routed request method;
        // checking both execution paths with assertions here
        try {
            response =
                allInstances[0].performRoutedRequest(DUMMY_REQUEST_CONTENT,
                    allInstances[allInstances.length - 1].getInstanceNodeSessionId(),
                    DEFAULT_REQUEST_TIMEOUT);
            assertFalse("Routed message sending should have failed; response=" + response.getDeserializedContent(), response.isSuccess());
        } catch (CommunicationException e) {
            assertTrue(e.getMessage().contains("could not find a route"));
        }
        // TODO when available, query response metadata for failure location (node id / hop count)

        for (VirtualInstance vi : allInstances) {
            if (!vi.equals(failingInstance)) {
                assertNodeAndLinkCount(vi, testSize - 1, testSize * 2 - 2);
            }
        }

        prepareWaitForNextMessage();
        failingInstance.start();
        waitForNextMessage();
        waitForNetworkSilence();
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    // FIXME test does not succeed; review -- misc_ro, Dec 2012
    public void testMergingNetworks() throws Exception {

        log.info("Topology: two rings unconnected rings");

        prepareWaitForNextMessage();
        // create two disconnected networks
        // TODO no non-double variants available
        instanceUtils.connectToDoubleRingTopology(allInstances, 0, testSize / 2 - 1);
        instanceUtils.connectToDoubleRingTopology(allInstances, testSize / 2, testSize - 1);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize / 2, testSize);
        }

        // connect networks
        prepareWaitForNextMessage();
        instanceUtils.doubleConcatenateInstances(allInstances, testSize / 2 - 1, testSize / 2);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize * 2 + 2);
        }

    }

    private void logIteration(int i) {
        log.debug("i: " + i);
    }
}
