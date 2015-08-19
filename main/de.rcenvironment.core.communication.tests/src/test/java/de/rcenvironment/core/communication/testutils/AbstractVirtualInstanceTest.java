/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;
import de.rcenvironment.core.communication.transport.virtual.testutils.VirtualInstanceTestUtils;
import de.rcenvironment.core.communication.transport.virtual.testutils.VirtualTopology;

/**
 * Common base class for {@link VirtualInstance} tests.
 * 
 * @author Robert Mischke
 */
public abstract class AbstractVirtualInstanceTest extends AbstractTransportBasedTest {

    protected static final String ERROR_MSG_NUMBER_OF_NODES = "Unexpected number of nodes in topology: ";

    protected static final String ERROR_MSG_NUMBER_OF_LINKS = "Unexpected number of links in topology: ";

    protected static final String ERROR_MSG_INSTANCES_DID_NOT_CONVERGE = "Instances did not converge";

    protected static final String ERROR_MSG_A_NO_ROUTE_TO_B = "%s could not find a route to %s";

    protected static int testSize;

    protected VirtualInstanceTestUtils instanceUtils;

    protected VirtualInstance[] allInstances;

    protected VirtualTopology testTopology;

    private final TestNetworkTrafficListener trafficListener = new TestNetworkTrafficListener();

    /**
     * Common setup method; sets up instanceUtils.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Before
    public void setUp() throws Exception {
        instanceUtils = new VirtualInstanceTestUtils(transportProvider, contactPointGenerator);
    }

    /**
     * Common teardown method; logs traffic statistics and resets static flags.
     * 
     * @throws Exception on uncaught exceptions
     */
    @After
    public void tearDown() throws Exception {

        // log statistics
        if (allInstances != null) {
            log.info(getGlobalTrafficListener().getFormattedTrafficReport(testSize));
        }
        // reset static flags in case they were changed
        VirtualInstance.setRememberRuntimePeersAfterRestarts(false);
    }

    protected void setupInstances(int numNodes, boolean useDuplexTransport, boolean startInstances) throws InterruptedException {
        log.debug(String.format("Setting up test '%s' with topology size %d", getCurrentTestName(), numNodes));
        allInstances = instanceUtils.spawnDefaultInstances(numNodes, useDuplexTransport, startInstances);
        addGlobalTrafficListener(allInstances);
        testTopology = new VirtualTopology(allInstances);
        testSize = numNodes;
    }

    protected void assertAllInstancesKnowSameTopology() {
        assertInstancesKnowSameTopology(allInstances);
    }

    protected void assertInstancesKnowSameTopology(VirtualInstance[] instances) {
        assertTrue(ERROR_MSG_INSTANCES_DID_NOT_CONVERGE, instanceUtils.allInstancesHaveSameRawNetworkGraph(instances));
    }

    protected boolean instancesHaveSameUnfilteredNetworkGraph(VirtualInstance vi1, VirtualInstance vi2) {
        // TODO merge with similar "assert" method?
        return vi1.getRawNetworkGraph().getCompactRepresentation().equals(vi2.getRawNetworkGraph().getCompactRepresentation());
    }

    protected void addGlobalTrafficListener(VirtualInstance[] instances) {
        // wrap & delegate
        addGlobalTrafficListener(new VirtualInstanceGroup(instances));
    }

    protected void addGlobalTrafficListener(VirtualInstanceGroup group) {
        group.addNetworkTrafficListener(trafficListener);
    }

    protected void prepareWaitForNextMessage() {
        trafficListener.clearCustomTrafficFlag();
    }

    protected void waitForNextMessage() throws TimeoutException, InterruptedException {
        trafficListener.waitForCustomTrafficFlag(testConfiguration.getDefaultTrafficWaitTimeout());
    }

    protected void waitForNetworkSilence() throws TimeoutException, InterruptedException {
        trafficListener.waitForNetworkSilence(testConfiguration.getDefaultNetworkSilenceWait(),
            testConfiguration.getDefaultNetworkSilenceWaitTimeout());
    }

    protected void waitForNetworkSilence(int timeoutMsec) throws TimeoutException, InterruptedException {
        trafficListener.waitForNetworkSilence(testConfiguration.getDefaultNetworkSilenceWait(), timeoutMsec);
    }

    protected TestNetworkTrafficListener getGlobalTrafficListener() {
        return trafficListener;
    }

    protected void assertNodeAndLinkCount(VirtualInstance vi, int expectedNodeCount, int expectedLinkCount) {
        NetworkGraph rawNetworkGraph = vi.getRawNetworkGraph();
        try {
            assertEquals(ERROR_MSG_NUMBER_OF_NODES, expectedNodeCount, rawNetworkGraph.getNodeCount());
            assertEquals(ERROR_MSG_NUMBER_OF_LINKS, expectedLinkCount, rawNetworkGraph.getLinkCount());
        } catch (AssertionError e) {
            log.debug(NetworkFormatter.networkGraphToGraphviz(rawNetworkGraph, false));
            log.debug(vi.getFormattedLSAKnowledge());
            throw e;
        }
    }
}
