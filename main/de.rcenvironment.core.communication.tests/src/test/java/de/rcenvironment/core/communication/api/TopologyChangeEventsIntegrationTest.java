/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.routing.NetworkRoutingService;
import de.rcenvironment.core.communication.routing.internal.NetworkRoutingServiceImpl;
import de.rcenvironment.core.communication.spi.NetworkTopologyChangeListener;
import de.rcenvironment.core.communication.testutils.AbstractVirtualInstanceTest;
import de.rcenvironment.core.communication.testutils.TestConfiguration;
import de.rcenvironment.core.communication.testutils.VirtualCommunicationBundle;
import de.rcenvironment.core.communication.testutils.VirtualInstance;
import de.rcenvironment.core.communication.transport.virtual.VirtualTransportTestConfiguration;

/**
 * Integration tests for events related to network topology changes.
 * 
 * @author Robert Mischke
 */
public class TopologyChangeEventsIntegrationTest extends AbstractVirtualInstanceTest {

    private static final int WAIT_FOR_ASYNC_EVENT_MSEC = 100;

    @Override
    protected TestConfiguration defineTestConfiguration() {
        return new VirtualTransportTestConfiguration(true);
    }

    /**
     * Tests proper callbacks to registered {@link NetworkTopologyChangeListener}s.
     * 
     * @throws Exception on uncaught exceptions
     */
    @Test
    public void testNetworkTopologyChangeListeners() throws Exception {
        setupInstances(3, true, false);
        final VirtualInstance initialNode = testTopology.getInstance(0);
        initialNode.start();

        final VirtualCommunicationBundle communicationBundle = initialNode.getVirtualCommunicationBundle();
        final NetworkRoutingServiceImpl networkRoutingService =
            (NetworkRoutingServiceImpl) communicationBundle.getService(NetworkRoutingService.class);
        Capture<Set<NodeIdentifier>> reachableNodesCapture = new Capture<Set<NodeIdentifier>>();
        Capture<Set<NodeIdentifier>> addedNodesCapture = new Capture<Set<NodeIdentifier>>();
        Capture<Set<NodeIdentifier>> removedNodesCapture = new Capture<Set<NodeIdentifier>>();
        final NetworkTopologyChangeListener topologyChangeListener = EasyMock.createMock(NetworkTopologyChangeListener.class);

        // set expectation on subscription
        topologyChangeListener.onReachableNodesChanged(capture(reachableNodesCapture), capture(addedNodesCapture),
            capture(removedNodesCapture));
        topologyChangeListener.onReachableNetworkChanged(EasyMock.anyObject(NetworkGraph.class));

        // execute
        EasyMock.replay(topologyChangeListener);
        networkRoutingService.addNetworkTopologyChangeListener(topologyChangeListener);
        Thread.sleep(WAIT_FOR_ASYNC_EVENT_MSEC);
        EasyMock.verify(topologyChangeListener);

        // check callback parameters
        assertEquals(1, reachableNodesCapture.getValue().size());
        assertTrue(reachableNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertEquals(1, addedNodesCapture.getValue().size());
        assertTrue(addedNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertEquals(0, removedNodesCapture.getValue().size());

        // define expectation on new reachable node
        EasyMock.reset(topologyChangeListener);
        topologyChangeListener.onNetworkTopologyChanged();
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNetworkChanged(EasyMock.anyObject(NetworkGraph.class)); // TODO check parameter?
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNodesChanged(capture(reachableNodesCapture), capture(addedNodesCapture),
            capture(removedNodesCapture));

        // execute
        EasyMock.replay(topologyChangeListener);
        final VirtualInstance addedNode1 = testTopology.getInstance(1);
        addedNode1.start();
        prepareWaitForNextMessage();
        testTopology.connect(0, 1, true);
        waitForNextMessage();
        waitForNetworkSilence();
        EasyMock.verify(topologyChangeListener);

        // check callback parameters
        assertEquals(2, reachableNodesCapture.getValue().size());
        assertTrue(reachableNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertTrue(reachableNodesCapture.getValue().contains(addedNode1.getNodeId()));
        assertEquals(1, addedNodesCapture.getValue().size());
        assertTrue(addedNodesCapture.getValue().contains(addedNode1.getNodeId()));
        assertEquals(0, removedNodesCapture.getValue().size());

        // define expectation on 2nd new reachable node
        EasyMock.reset(topologyChangeListener);
        topologyChangeListener.onNetworkTopologyChanged();
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNetworkChanged(EasyMock.anyObject(NetworkGraph.class)); // TODO check parameter?
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNodesChanged(capture(reachableNodesCapture), capture(addedNodesCapture),
            capture(removedNodesCapture));

        // execute
        EasyMock.replay(topologyChangeListener);
        final VirtualInstance addedNode2 = testTopology.getInstance(2);
        addedNode2.start();
        prepareWaitForNextMessage();
        testTopology.connect(0, 2, true);
        waitForNextMessage();
        waitForNetworkSilence();
        EasyMock.verify(topologyChangeListener);

        // check callback parameters
        assertEquals(3, reachableNodesCapture.getValue().size());
        assertTrue(reachableNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertTrue(reachableNodesCapture.getValue().contains(addedNode1.getNodeId()));
        assertTrue(reachableNodesCapture.getValue().contains(addedNode2.getNodeId()));
        assertEquals(1, addedNodesCapture.getValue().size());
        assertTrue(addedNodesCapture.getValue().contains(addedNode2.getNodeId()));
        assertEquals(0, removedNodesCapture.getValue().size());

        // define expectation on shutting down the first added node
        EasyMock.reset(topologyChangeListener);
        topologyChangeListener.onNetworkTopologyChanged();
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNetworkChanged(EasyMock.anyObject(NetworkGraph.class)); // TODO check parameter?
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNodesChanged(capture(reachableNodesCapture), capture(addedNodesCapture),
            capture(removedNodesCapture));

        // execute
        EasyMock.replay(topologyChangeListener);
        prepareWaitForNextMessage();
        addedNode1.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();
        EasyMock.verify(topologyChangeListener);

        // check callback parameters
        assertEquals(2, reachableNodesCapture.getValue().size());
        assertTrue(reachableNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertTrue(reachableNodesCapture.getValue().contains(addedNode2.getNodeId()));
        assertEquals(0, addedNodesCapture.getValue().size());
        assertEquals(1, removedNodesCapture.getValue().size());
        assertTrue(removedNodesCapture.getValue().contains(addedNode1.getNodeId()));

        // define expectation on shutting down the second added node
        EasyMock.reset(topologyChangeListener);
        topologyChangeListener.onNetworkTopologyChanged();
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNetworkChanged(EasyMock.anyObject(NetworkGraph.class)); // TODO check parameter?
        EasyMock.expectLastCall().atLeastOnce();
        topologyChangeListener.onReachableNodesChanged(capture(reachableNodesCapture), capture(addedNodesCapture),
            capture(removedNodesCapture));

        // execute
        EasyMock.replay(topologyChangeListener);
        prepareWaitForNextMessage();
        addedNode2.shutDown();
        waitForNextMessage();
        waitForNetworkSilence();
        EasyMock.verify(topologyChangeListener);

        // check callback parameters
        assertEquals(1, reachableNodesCapture.getValue().size());
        assertTrue(reachableNodesCapture.getValue().contains(initialNode.getNodeId()));
        assertEquals(0, addedNodesCapture.getValue().size());
        assertEquals(1, removedNodesCapture.getValue().size());
        assertTrue(removedNodesCapture.getValue().contains(addedNode2.getNodeId()));
    }
}
