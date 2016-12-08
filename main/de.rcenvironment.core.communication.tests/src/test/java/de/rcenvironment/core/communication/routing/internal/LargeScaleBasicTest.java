/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rcenvironment.core.utils.testing.CommonTestOptions;

/**
 * Obsolete test container; move/rework the contained tests.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class LargeScaleBasicTest extends AbstractLargeScaleTest {

    /*
     * Parameters for test scenarios.
     */
    // test size 10 for fast/standard testing, 20 for extended testing; TODO review values
    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(10, 20);

    // note: previous value was 5; arbitrarily reduced to 3 for standard testing
    private static final int EPOCHS = CommonTestOptions.selectStandardOrExtendedValue(3, 5);

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
    public void testSendLinkStateAdvertisementBarrierRing() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testSendLinkStateAdvertisementBarrierChain() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testSendLinkStateAdvertisementBarrierStar() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleStarTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testBarrierInDoubleRingTopology() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testBarrierInDoubleChainTopoglogy() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleRingTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

    /**
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testBarrierInDoubleStarTopology() throws Exception {

        prepareWaitForNextMessage();
        instanceUtils.connectToDoubleChainTopology(allInstances);
        waitForNextMessage();
        waitForNetworkSilence();

        assertTrue(instanceUtils.allInstancesHaveSameRawNetworkGraph(allInstances));
    }

}
