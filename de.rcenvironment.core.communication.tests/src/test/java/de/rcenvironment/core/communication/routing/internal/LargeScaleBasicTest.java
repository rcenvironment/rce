/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
    private static final int TEST_SIZE = CommonTestOptions.selectStandardOrExtendedValue(5, 20);

    private static final int EPOCHS = CommonTestOptions.selectStandardOrExtendedValue(2, 5);

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
