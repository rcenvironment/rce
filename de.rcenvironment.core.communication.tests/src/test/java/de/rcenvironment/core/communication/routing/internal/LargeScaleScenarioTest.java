/*
 * Copyright 2006-2021 DLR, Germany
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
import de.rcenvironment.core.communication.testutils.TestNetworkRequestHandler;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.testing.CommonTestOptions;

/**
 * @author Phillip Kroll
 * @author Robert Mischke
 * 
 */
public class LargeScaleScenarioTest extends AbstractLargeScaleTest {

    private static final String DUMMY_REQUEST_CONTENT = "hello";

    private static final int DEFAULT_REQUEST_TIMEOUT = 10000;

    // set to a minimum of 6 instances to keep scenarios like "two rings with a chain" reasonable
    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(6, 10);

    private static final int EPOCHS = CommonTestOptions.selectStandardOrExtendedValue(2, 3);

    /**
     * @throws Exception on uncaught exceptions
     */
    @BeforeClass
    public static void setTestParameters() throws Exception {
        testSize = TEST_SIZE;
        epochs = EPOCHS;
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testTwoRingsAndOneChainTopology() throws Exception {
        log.info("Topology: two rings connected with a chain.");

        prepareWaitForNextMessage();

        int chainSize = 2; // used to be testSize / 4, which caused problems with test sizes != 10
        instanceUtils.connectToDoubleRingTopology(allInstances, 0, testSize / 2 - chainSize / 2);
        instanceUtils.connectToDoubleChainTopology(allInstances, testSize / 2 - (chainSize / 2 - 1), testSize / 2 + (chainSize / 2 - 1));
        instanceUtils.connectToDoubleRingTopology(allInstances, testSize / 2 + chainSize / 2, testSize - 1);
        testTopology.connect(testSize / 2 - chainSize / 2, testSize / 2 - (chainSize / 2 - 1), true);
        testTopology.connect(testSize / 2 + chainSize / 2, testSize / 2 + (chainSize / 2 - 1), true);
        waitForNextMessage();
        waitForNetworkSilence();

        // TODO review: check for topology hashcode, too?

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize * 2 + 2);
        }
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testLargeDoubleRingTopology() throws Exception {

        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
            assertNodeAndLinkCount(vi, testSize, testSize * 2);
        }

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
            testTopology.connect(i, newInstanceIndex, true);
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
            testTopology.connect(i, i + 1, true);
        }
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(testTopology.allInstancesConverged());
        for (int i = 0; i < allInstances.length - 1; i++) {
            assertEquals(allInstances.length, allInstances[i].getReachableNetworkGraph().getNodeCount());
        }
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
            testTopology.connect(connectedInstanceIndex, newInstanceIndex, true);
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
        for (int i = 1; i <= allInstances.length - 1; i++) {
            logIteration(i);
            int connectedInstanceIndex = random.nextInt(i); // 0..(i-1)
            testTopology.connect(connectedInstanceIndex, i, true);
        }
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue("Instances did not converge", instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * Test a simple ring topology.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testLargeRingTopology() throws Exception {

        log.info("Topology: ring"); // single direction; rename?

        prepareWaitForNextMessage();
        testTopology.connectToRing(false);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize);
        }
    }

    /**
     * Handle large network.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testLargeDoubleChainTopology() throws Exception {

        log.info("Topology: double chain");

        prepareWaitForNextMessage();
        testTopology.connectToChain(true);
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
    public void testLargeDoubleStarTopology() throws Exception {

        log.info("Topology: duplex star");

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleStarTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
            assertNodeAndLinkCount(vi, testSize, (testSize - 1) * 2);
        }

    }

    /**
     * Compute many routes.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testComputeManyRoutes() throws Exception {

        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            // assertTrue(NETWORK_NOT_FULLY_CONVERGED, vi.isFullyConverged());
            assertNodeAndLinkCount(vi, testSize, testSize * 2);
        }

        for (int i = 0; i < testSize; i++) {
            // compute a random route
            VirtualInstance vi1 = testTopology.getRandomInstance();
            VirtualInstance vi2 = testTopology.getRandomInstanceExcept(vi1);

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
     * Handle large network.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    @Ignore
    // TODO (p2) (test) does not succeed; review -- misc_ro, Dec 2012
    public void testDuplexRingFailureRecovery() throws Exception {

        // this test needs instances to keep their peer config after a stop/start cycle -- misc_ro
        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        // iterate over epochs randomly generated scenarios
        for (int i = 0; i < epochs; i++) {
            log.info("Epoch: " + i);
            // waitSomeTime(instances.length);

            String message = instanceUtils.generateUniqueMessageToken();

            VirtualInstance failingInstance = testTopology.getRandomInstance();
            VirtualInstance sender = testTopology.getRandomInstanceExcept(failingInstance);
            VirtualInstance receiver = testTopology.getRandomInstanceExcept(sender, failingInstance);

            // check that everything is normal (e.g. ring topology)
            for (VirtualInstance vi : allInstances) {
                // TODO (p2) (part of disabled test) check: this assertion fails frequently
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
    @Ignore
    // FIXME test does not succeed; review -- misc_ro, Dec 2012
    public void testDuplexChainFailureRecovery() throws Exception {

        // this test needs instances to keep their peer config after a stop/start cycle -- misc_ro
        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        // iterate over epochs randomly generated scenarios
        for (int i = 0; i < epochs; i++) {
            log.debug("Epoche: " + i);

            String messageContent = instanceUtils.generateUniqueMessageToken();
            VirtualInstance failingInstance = testTopology.getRandomInstance();
            VirtualInstance sender = testTopology.getRandomInstanceExcept(failingInstance);
            VirtualInstance receiver = testTopology.getRandomInstanceExcept(sender, failingInstance);

            // shutdown
            prepareWaitForNextMessage();
            failingInstance.shutDown();
            waitForNextMessage();
            waitForNetworkSilence();

            log.debug(StringUtils.format("%s attempts to communicate with %s.",
                instanceUtils.getFormattedName(sender),
                instanceUtils.getFormattedName(receiver)));

            try {
                // this can fail sometimes
                NetworkResponse response =
                    sender.performRoutedRequest(messageContent, receiver.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
                assertTrue(response.getDeserializedContent().toString(), response.isSuccess());

                assertTrue(
                    StringUtils.format("%s failed to send routed message to %s. \n\n %s",
                        instanceUtils.getFormattedName(sender),
                        instanceUtils.getFormattedName(receiver),
                        sender.getFormattedLegacyNetworkGraph()),
                    receiver.getRoutingService().getProtocolManager().messageReivedByContent(messageContent));

                assertTrue(receiver.checkMessageReceivedByContent(messageContent));
                // waitSomeTime(instances.length);

            } catch (CommunicationException e) {
                log.warn(e.getMessage());
            }

            // startup
            prepareWaitForNextMessage();
            failingInstance.start();
            waitForNextMessage();
            waitForNetworkSilence();

            // send a message
            NetworkResponse response =
                sender.performRoutedRequest(messageContent, receiver.getInstanceNodeSessionId(), DEFAULT_REQUEST_TIMEOUT);
            assertTrue(response.toString(), response.isSuccess());

            log.info(failingInstance.getFormattedNetworkStats());

        }

    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testMessageConfirmation() throws Exception {
        log.info("Topology: double ring");

        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        log.info(allInstances[0].getFormattedLegacyNetworkGraph());

        // note: added n repetitions to this test; arbitrary value
        for (int i = 0; i < testSize; i++) {

            long trafficCountBefore = getGlobalRequestCount();

            VirtualInstance sender = testTopology.getRandomInstance();
            VirtualInstance receiver = testTopology.getRandomInstanceExcept(sender);
            // old:
            // String id = sender.sendRoutedAsyncMessage("hello", receiver);
            // sender.getRoutingService().getProtocol().waitForMessageConfirmation(id);
            String requestBody = DUMMY_REQUEST_CONTENT + i;
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
    @Ignore
    // TODO (p3) (test) temporarily disabled; review
    public void testMessageFailure() throws Exception {

        log.info("Topology: double ring");

        VirtualInstance.setRememberRuntimePeersAfterRestarts(true);

        // create topology
        prepareWaitForNextMessage();
        testTopology.connectToRing(true);
        waitForNextMessage();
        waitForNetworkSilence();

        VirtualInstance failingInstance = testTopology.getRandomInstance();
        VirtualInstance sender = testTopology.getRandomInstanceExcept(failingInstance);

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
        // TODO document/clarify created topology
        instanceUtils.connectToDoubleRingTopology(allInstances);
        instanceUtils.connectToDoubleRingTopology(allInstances, 0, testSize / 2 - chainSize / 2);
        instanceUtils.connectToDoubleChainTopology(allInstances, testSize / 2 - (chainSize / 2
            - 1), testSize / 2 + (chainSize / 2 - 1));
        instanceUtils.connectToDoubleRingTopology(allInstances, testSize / 2 + chainSize / 2,
            testSize - 1);
        testTopology.connect(testSize / 2 - chainSize / 2, testSize / 2 - (chainSize / 2 - 1), true);
        testTopology.connect(testSize / 2 + chainSize / 2, testSize / 2 + (chainSize / 2 - 1), true);
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
        try {
            // TODO clarify failure response vs. exception behaviour for routed request method;
            // checking both execution paths with assertions here
            response =
                allInstances[0].performRoutedRequest(DUMMY_REQUEST_CONTENT,
                    allInstances[allInstances.length - 1].getInstanceNodeSessionId(),
                    DEFAULT_REQUEST_TIMEOUT);
            // TODO when available, query response metadata for failure location (node id / hop
            // count)
            assertFalse("Routed message sending should have failed; response=" + response.getDeserializedContent(), response.isSuccess());
        } catch (CommunicationException e) {
            assertTrue(e.getMessage().contains("could not find a route"));
        }

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
    public void testMergingNetworks() throws Exception {

        log.info("Topology: two rings unconnected rings");

        prepareWaitForNextMessage();
        // create two disconnected networks
        instanceUtils.connectToDoubleRingTopology(allInstances, 0, testSize / 2 - 1);
        instanceUtils.connectToDoubleRingTopology(allInstances, testSize / 2, testSize - 1);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize / 2, testSize);
        }

        // connect networks
        prepareWaitForNextMessage();
        testTopology.connect(testSize / 2 - 1, testSize / 2, true);
        waitForNextMessage();
        waitForNetworkSilence();

        for (VirtualInstance vi : allInstances) {
            assertNodeAndLinkCount(vi, testSize, testSize * 2 + 2);
        }

    }

    private void logIteration(int i) {
        log.debug("Starting test iteration " + i);
    }
}
