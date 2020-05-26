/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.communication.model.NetworkRoutingInformation;
import de.rcenvironment.core.communication.routing.internal.v2.NoRouteToNodeException;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;

/**
 * {@link NetworkGraphImpl} unit test.
 * 
 * @author Robert Mischke
 */
public class NetworkGraphImplTest {

    private InstanceNodeSessionId node1;

    private InstanceNodeSessionId node2;

    private InstanceNodeSessionId node3;

    private InstanceNodeSessionId node4;

    private InstanceNodeSessionId node5;

    private NetworkGraphLinkImpl link12;

    private NetworkGraphLinkImpl link23;

    private NetworkGraphLinkImpl link34;

    private NetworkGraphLinkImpl link45;

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
        node1 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("1");
        node2 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("2");
        node3 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("3");
        node4 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("4");
        node5 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("5");
        link12 = new NetworkGraphLinkImpl("12s-x", node1, node2);
        link23 = new NetworkGraphLinkImpl("23s-x", node2, node3);
        link34 = new NetworkGraphLinkImpl("34s-x", node3, node4);
        link45 = new NetworkGraphLinkImpl("45s-x", node4, node5);
    }

    /**
     * Tests the computation of the reachable graph from various source graphs.
     */
    @Test
    public void testReductionToReachable() {
        int n = 5;
        InstanceNodeSessionId[] nodes = new InstanceNodeSessionId[n];
        for (int i = 0; i < n; i++) {
            nodes[i] = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("Node" + i);
        }
        // test with linear chains
        for (int i = 0; i < n; i++) {
            NetworkGraphImpl rawGraph = new NetworkGraphImpl(nodes[i]);
            for (int j = 0; j < n - 1; j++) {
                rawGraph.addLink(j + "-", nodes[j], nodes[j + 1]);
            }
            assertEquals(n, rawGraph.getNodeCount());
            assertEquals(n - 1, rawGraph.getLinkCount());
            NetworkGraphImpl reachableGraph = rawGraph.reduceToReachableGraph();

            assertEquals(n - i, reachableGraph.getNodeCount());
            assertEquals((n - i) - 1, reachableGraph.getLinkCount());
            // raw graph should be unchanged
            assertEquals(n, rawGraph.getNodeCount());
            assertEquals(n - 1, rawGraph.getLinkCount());
        }
        // test with closed loops
        for (int i = 0; i < n; i++) {
            NetworkGraphImpl rawGraph = new NetworkGraphImpl(nodes[i]);
            for (int j = 0; j < n - 1; j++) {
                rawGraph.addLink(j + "-", nodes[j], nodes[j + 1]);
            }
            rawGraph.addLink("back-", nodes[n - 1], nodes[0]);

            assertEquals(n, rawGraph.getNodeCount());
            assertEquals(n, rawGraph.getLinkCount());
            NetworkGraphImpl reachableGraph = rawGraph.reduceToReachableGraph();
            // reachable graph should be full graph
            assertEquals(n, reachableGraph.getNodeCount());
            assertEquals(n, reachableGraph.getLinkCount());
            // raw graph should be unchanged
            assertEquals(n, rawGraph.getNodeCount());
            assertEquals(n, rawGraph.getLinkCount());
        }
        NetworkGraphImpl rawGraph;
        NetworkGraphImpl reachableGraph;
        // test detached nodes
        rawGraph = new NetworkGraphImpl(node1);
        rawGraph.addNode(node2);
        rawGraph.addNode(node3);
        rawGraph.addLink("x-", node1, node2);
        assertEquals(3, rawGraph.getNodeCount());
        assertEquals(1, rawGraph.getLinkCount());
        reachableGraph = rawGraph.reduceToReachableGraph();
        assertEquals(2, reachableGraph.getNodeCount());
        assertEquals(1, reachableGraph.getLinkCount());
        // test attached, but unreachable nodes
        rawGraph = new NetworkGraphImpl(node1);
        rawGraph.addNode(node2);
        rawGraph.addNode(node3);
        rawGraph.addLink("x-", node1, node2);
        rawGraph.addLink("y-", node3, node1);
        assertEquals(3, rawGraph.getNodeCount());
        assertEquals(2, rawGraph.getLinkCount());
        reachableGraph = rawGraph.reduceToReachableGraph();
        assertEquals(2, reachableGraph.getNodeCount());
        assertEquals(1, reachableGraph.getLinkCount());
    }

    /**
     * Test of routing map generation and caching.
     * 
     * @throws NoRouteToNodeException on unexpected routing failures
     */
    @Test
    public void testChainWithRootInMiddle() throws NoRouteToNodeException {
        InstanceNodeSessionId rootNode = node3;
        NetworkGraphImpl rawGraph = createFiveNodeChainWithRootAt(rootNode);

        NetworkRoutingInformation routingInformation = rawGraph.generateRoutingInformation();
        assertRoutingToLocalNodeFails(routingInformation, rootNode);

        // check adjacent node
        assertEquals(link34, routingInformation.getNextLinkTowards(node4));
        assertEquals(1, getRoutingCacheMisses(routingInformation));
        // repeat -> should not cause another cache miss
        assertEquals(link34, routingInformation.getNextLinkTowards(node4));
        assertEquals(1, getRoutingCacheMisses(routingInformation));

        // check adjacent, but unreachble node (only inbound link)
        try {
            routingInformation.getNextLinkTowards(node2);
            fail("Routing should have failed");
        } catch (NoRouteToNodeException e) {
            assertEquals(node2, e.getTargetNodeId());
        }
        // make no assumptions about caching of routing failures, as they should be the exception

        // reset cache miss counter
        resetCacheMisses(routingInformation);

        // check reachable, non-adjacent node
        assertEquals(link34, routingInformation.getNextLinkTowards(node5));
        assertEquals(1, getRoutingCacheMisses(routingInformation));
        // repeat -> should not cause another cache miss
        assertEquals(link34, routingInformation.getNextLinkTowards(node5));
        assertEquals(1, getRoutingCacheMisses(routingInformation));

        // check unreachable, non-adjacent node
        try {
            routingInformation.getNextLinkTowards(node1);
            fail("Routing should have failed");
        } catch (NoRouteToNodeException e) {
            assertEquals(node1, e.getTargetNodeId());
        }
    }

    /**
     * Test 5-node chain, with node 1 being root, going forward (2..5) with routing requests.
     * 
     * @throws NoRouteToNodeException on unexpected routing failures
     */
    @Test
    public void testChainWithRootAtStartGoingForward() throws NoRouteToNodeException {
        InstanceNodeSessionId rootNode = node1;
        NetworkGraphImpl rawGraph = createFiveNodeChainWithRootAt(rootNode);

        NetworkGraphImpl processedGraph = rawGraph.reduceToReachableGraph();
        NetworkRoutingInformation routingInformation = processedGraph.getRoutingInformation();
        assertRoutingToLocalNodeFails(routingInformation, rootNode);

        assertEquals(link12, routingInformation.getNextLinkTowards(node2));
        assertEquals(1, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node3));
        assertEquals(2, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node4));
        assertEquals(3, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node5));
        assertEquals(4, getRoutingCacheMisses(routingInformation));

        // repeat arbitrary node -> should not cause another cache miss
        resetCacheMisses(routingInformation);
        assertEquals(link12, routingInformation.getNextLinkTowards(node4));
        assertEquals(0, getRoutingCacheMisses(routingInformation));
    }

    /**
     * Test 5-node chain, with node 1 being root, going backward (5..2) with routing requests.
     * 
     * @throws NoRouteToNodeException on unexpected routing failures
     */
    @Test
    public void testChainWithRootAtStartGoingBackward() throws NoRouteToNodeException {
        InstanceNodeSessionId rootNode = node1;
        NetworkGraphImpl rawGraph = createFiveNodeChainWithRootAt(rootNode);

        NetworkGraphImpl processedGraph = rawGraph.reduceToReachableGraph();
        NetworkRoutingInformation routingInformation = processedGraph.getRoutingInformation();
        assertRoutingToLocalNodeFails(routingInformation, rootNode);

        assertEquals(link12, routingInformation.getNextLinkTowards(node5));
        // cache misses should immediately jump to 4, then stay there
        assertEquals(4, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node4));
        assertEquals(4, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node3));
        assertEquals(4, getRoutingCacheMisses(routingInformation));
        assertEquals(link12, routingInformation.getNextLinkTowards(node2));
        assertEquals(4, getRoutingCacheMisses(routingInformation));

        // repeat arbitrary node -> should not cause another cache miss
        resetCacheMisses(routingInformation);
        assertEquals(link12, routingInformation.getNextLinkTowards(node4));
        assertEquals(0, getRoutingCacheMisses(routingInformation));
    }

    /**
     * Tests basic route calculation.
     */
    @Test
    public final void testBasicRouteComputation() {
        NetworkGraphImpl networkGraph = new NetworkGraphImpl(node1);
        networkGraph.addNode(node2);
        networkGraph.addNode(node3);

        networkGraph.addLink("link12", node1, node2);
        networkGraph.addLink("link23", node2, node3);
        networkGraph.addLink("link31", node3, node1);
        networkGraph.addLink("link41", node4, node1);

        NetworkRoutingInformation routingInformation = networkGraph.generateRoutingInformation();

        List<? extends NetworkGraphLink> route = routingInformation.getRouteTo(node3);

        assertEquals(2, route.size());
        assertEquals(node2, route.get(0).getTargetNodeId());
        assertEquals(node3, route.get(1).getTargetNodeId());

        route = routingInformation.getRouteTo(node2);

        assertEquals(1, route.size());
        assertEquals(node2, route.get(0).getTargetNodeId());

        // test failing route (unreachable node)
        route = routingInformation.getRouteTo(node4);
        assertNull("Route should be null", route);

        // test exception when passing the local node
        try {
            route = routingInformation.getRouteTo(node1);
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    /**
     * Tests implicit addition of nodes from added edges, additionally, the edges are added from different threads.
     */
    public void testMultiThreadedGraphCreationFromAddedEdges() {
        final NetworkGraphImpl rawGraph = new NetworkGraphImpl(node1);
        int n = 10 * 10; // yay for Checkstyle :)
        CallablesGroup<Void> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Void.class);
        for (int i = 0; i < n; i++) {
            final int i2 = i;
            callablesGroup.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    InstanceNodeSessionId tempNode =
                        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("tempNode" + i2);
                    if (i2 % 2 == 0) {
                        rawGraph.addLink("link" + i2, node1, tempNode);
                    } else {
                        rawGraph.addLink("link" + i2, tempNode, node1);
                    }
                    return null;
                }
            });
            callablesGroup.executeParallel(null);
        }
        assertEquals(n + 1, rawGraph.getNodeCount());
        assertEquals(n + 1, rawGraph.getNodeIds().size());
        assertEquals(n * 2, rawGraph.getLinkCount());
    }

    private NetworkGraphImpl createFiveNodeChainWithRootAt(InstanceNodeSessionId rootNodeId) {
        NetworkGraphImpl rawGraph = new NetworkGraphImpl(rootNodeId);
        rawGraph.addNode(node1);
        rawGraph.addNode(node2);
        rawGraph.addNode(node3);
        rawGraph.addNode(node4);
        rawGraph.addNode(node5);
        rawGraph.addLink(link12);
        rawGraph.addLink(link23);
        rawGraph.addLink(link34);
        rawGraph.addLink(link45);
        return rawGraph;
    }

    /**
     * Common test method to verify that trying to route to the local node fails.
     */
    private void assertRoutingToLocalNodeFails(NetworkRoutingInformation routingInformation, InstanceNodeSessionId rootNode) {
        try {
            routingInformation.getNextLinkTowards(rootNode);
            fail("Routing to local node should have failed");
        } catch (NoRouteToNodeException e) {
            assertEquals(rootNode, e.getTargetNodeId());
        }
        // cache should be untouched
        assertEquals(0, getRoutingCacheMisses(routingInformation));
    }

    private int getRoutingCacheMisses(NetworkRoutingInformation routingInformation) {
        return ((NetworkRoutingInformationImpl) routingInformation).getRoutingCacheMisses();
    }

    private void resetCacheMisses(NetworkRoutingInformation routingInformation) {
        ((NetworkRoutingInformationImpl) routingInformation).resetCacheMisses();
    }
}
