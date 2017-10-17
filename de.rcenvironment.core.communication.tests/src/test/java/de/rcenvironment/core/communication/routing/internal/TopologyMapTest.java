/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.routing.internal;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import junit.framework.TestCase;

/**
 * Unit tests for {@link TopologyMap}.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public class TopologyMapTest extends TestCase {

    private static final int SHORT_TIMESTAMP_CHANGING_DELAY = 150;

    private static final String DIFFERENT_NUMBER_OF_VERTICES_IN_GRAPH_EXPECTED = "Different number of vertices in graph expected.";

    private static final String DIFFERENT_NUMBER_OF_EDGES_IN_GRAPH_EXPECTED = "Different number of edges in graph expected.";

    private static final String GRAPH_SHOULD_CONTAIN_CHANNEL = "Graph is expected to contain channel.";

    private static final String GRAPH_CONTAINS_CHANNEL = "Graph is expected to NOT contain channel.";

    private static final String GRAPH_DOES_NOT_CONTAIN_NODE = "Graph is expected to NOT contain platform.";

    private static final String GRAPH_CONTAINS_NODE = "Graph is expected to contain platform.";

    private static final String LSA_CAUSED_NO_UPDATE = "Merging this LSA into a graph was not considered an update as expected";

    private static final String LSA_CAUSED_UPDATE = "Merging this LSA into a graph was considered an update when it shouldn't";

    private static final InstanceNodeSessionId NODE_1 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node1");

    private static final InstanceNodeSessionId NODE_2 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node2");

    private static final InstanceNodeSessionId NODE_3 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node3");

    private static final InstanceNodeSessionId NODE_4 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node4");

    private static final InstanceNodeSessionId NODE_5 = NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("node5");

    private static final String NODE_1_NAME = "Name1";

    private static final String NODE_2_NAME = "Name2";

    private static final String NODE_4_NAME = "Name4";

    private static final String CONNECTION_ID_1 = "#1";

    private static final String CONNECTION_ID_2 = "#2";

    private static final String CONNECTION_ID_3 = "#3";

    @Deprecated
    // was used for sequence numbers, which does not work anymore
    private static final int ARBITRARY_INT = 4711;

    protected TopologyMap networkGraph;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    protected void setUp() throws Exception {}

    @Override
    protected void tearDown() throws Exception {}

    /**
     * Test for {@link TopologyMap#getAllLinks()}.
     */
    public final void testGetChannels() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        networkGraph.addLink(NODE_2, NODE_1, CONNECTION_ID_1);
        networkGraph.addLink(NODE_2, NODE_3, CONNECTION_ID_1);

        assertEquals(3, networkGraph.getAllLinks().size());

        assertNotNull(networkGraph.getLinkForConnection(CONNECTION_ID_1));
        assertEquals(networkGraph.getLinkForConnection(CONNECTION_ID_1), new TopologyLink(NODE_1, NODE_2, CONNECTION_ID_1));

        networkGraph.removeLink(null);
        networkGraph.removeLink(networkGraph.getLinkForConnection(CONNECTION_ID_1));

        assertEquals(2, networkGraph.getAllLinks().size());
        assertFalse(networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertFalse(networkGraph.containsLink(NODE_1, NODE_2, CONNECTION_ID_1));
    }

    /**
     * Simple test for graph setup: NODE_1 - NODE_2 - NODE_3.
     */
    public final void testGraphSetUp() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);

        assertTrue(GRAPH_CONTAINS_NODE, networkGraph.containsNode(NODE_1));
        assertTrue(GRAPH_CONTAINS_NODE, networkGraph.containsNode(NODE_2));
        assertTrue(GRAPH_CONTAINS_NODE, networkGraph.containsNode(NODE_3));
        assertFalse(GRAPH_DOES_NOT_CONTAIN_NODE, networkGraph.containsNode(NODE_4));
        assertFalse(GRAPH_DOES_NOT_CONTAIN_NODE, networkGraph.containsNode(NODE_5));

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        networkGraph.addLink(NODE_2, NODE_3, CONNECTION_ID_1);

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_3));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_4, NODE_5));
        assertEquals(DIFFERENT_NUMBER_OF_EDGES_IN_GRAPH_EXPECTED, 2, networkGraph.getLinkCount());
        assertEquals(DIFFERENT_NUMBER_OF_VERTICES_IN_GRAPH_EXPECTED, 3, networkGraph.getNodeCount());

    }

    /**
     * 
     */
    public final void testLinkStateAdvertisement() {
        LinkStateAdvertisement linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_1, NODE_1_NAME, true, ARBITRARY_INT, ARBITRARY_INT, true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_1, NODE_2, CONNECTION_ID_1),
                    new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1)
                }));

        assertEquals(linkStateAdvertisement.getOwner(), NODE_1);
        assertEquals(linkStateAdvertisement.getSequenceNumber(), ARBITRARY_INT);
    }

    /**
     * Build graph and do manipulations with some {@link LinkStateAdvertisement}s.
     */
    public final void testUpdateGraph() {
        // IMPORTANT: the test graph must be "owned" by a node that is not used in the test,
        // as TopologyMap was made to ignore external updates for its local node
        networkGraph = new TopologyMap(NODE_3);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);

        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_3));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        networkGraph.addLink(NODE_2, NODE_3, CONNECTION_ID_2);

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_3));

        LinkStateAdvertisement lsa =
            LinkStateAdvertisement.createUpdateLsa(NODE_1, NODE_1_NAME, true, networkGraph.getSequenceNumberOfNode(NODE_1) + 1,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_1, NODE_2, CONNECTION_ID_1),
                    new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_3)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(lsa));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_3));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));

        lsa =
            LinkStateAdvertisement.createUpdateLsa(NODE_1, NODE_1_NAME, true, networkGraph.getSequenceNumberOfNode(NODE_1) + 1,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(lsa));

        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_3));

        // inject a new node (NODE_4) via LSA
        lsa =
            LinkStateAdvertisement.createUpdateLsa(NODE_4, NODE_4_NAME, true, 1,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_4, NODE_3, CONNECTION_ID_1),
                    new TopologyLink(NODE_4, NODE_2, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(lsa));

        assertTrue(GRAPH_CONTAINS_NODE, networkGraph.containsNode(NODE_4));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_4, NODE_3));

        lsa =
            LinkStateAdvertisement.createUpdateLsa(NODE_4, NODE_4_NAME, true, networkGraph.getSequenceNumberOfNode(NODE_4) + 1,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_4, NODE_5, CONNECTION_ID_1)
                }));

        assertFalse(GRAPH_DOES_NOT_CONTAIN_NODE, networkGraph.containsNode(NODE_5));

        // log.info(NetworkFormatter.summary(networkGraph));
        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(lsa));

        assertTrue(GRAPH_CONTAINS_NODE, networkGraph.containsNode(NODE_5));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_4, NODE_5));
    }

    /**
     * Build graph and do manipulations with some {@link LinkStateAdvertisement}s.
     */
    public final void testEmptyGraphUpdate() {
        networkGraph = new TopologyMap(NODE_1);

        LinkStateAdvertisement linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true, ARBITRARY_INT,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_2, NODE_1, CONNECTION_ID_1),
                    new TopologyLink(NODE_2, NODE_3, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_1));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));

        linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true, ARBITRARY_INT + 1,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {}));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertEquals(DIFFERENT_NUMBER_OF_EDGES_IN_GRAPH_EXPECTED, 0, networkGraph.getLinkCount());
        assertEquals(DIFFERENT_NUMBER_OF_VERTICES_IN_GRAPH_EXPECTED, 3, networkGraph.getNodeCount());

    }

    /**
     * Test graph updates with different {@link LinkStateAdvertisement}s.
     */
    public final void testSequenceNumberUpdate() {
        networkGraph = new TopologyMap(NODE_1);

        LinkStateAdvertisement linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true, ARBITRARY_INT,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_2, NODE_1, CONNECTION_ID_1),
                    new TopologyLink(NODE_2, NODE_3, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_1));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));

        // this should be ignored, because the sequence number is to small/old
        linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true,
                ARBITRARY_INT - 1,
                networkGraph.hashCode(),
                true,
                Arrays.asList(new TopologyLink[] {}));

        assertFalse(LSA_CAUSED_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_1));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));
    }

    /**
     * TODO @krol_ph: Test description.
     */
    public final void testUpdateGraphEdges() {
        networkGraph = new TopologyMap(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_2);
        // should succeed
        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        // note: changed to assertFalse() after switch from NCP to connection ids
        try {
            networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
            fail("Exception expected");
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
        // note: changed to assertFalse() after switch from NCP to connection ids
        assertFalse(networkGraph.addLink(new TopologyLink(NODE_1, NODE_2, CONNECTION_ID_1)));
        networkGraph.getLinkForConnection(CONNECTION_ID_1).incReliability();
        networkGraph.getLinkForConnection(CONNECTION_ID_1).setWeight(7);
        assertEquals(1, networkGraph.getLinkForConnection(CONNECTION_ID_1).getReliability());
        assertEquals(7, networkGraph.getLinkForConnection(CONNECTION_ID_1).getWeight());
    }

    /**
     * Test graph updates with different {@link LinkStateAdvertisement}s.
     */
    public final void testLsaIntegration1() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_2);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));

        LinkStateAdvertisement linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true, ARBITRARY_INT,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_2, NODE_1, CONNECTION_ID_1),
                    new TopologyLink(NODE_2, NODE_3, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_1));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
    }

    /**
     * Test graph updates with different {@link LinkStateAdvertisement}s.
     */
    public final void testLsaIntegration2() {
        TopologyMap graph1 = new TopologyMap(NODE_1);
        TopologyMap graph2 = new TopologyMap(NODE_3);

        graph1.addLink(NODE_1, NODE_3, CONNECTION_ID_1);
        graph2.addLink(NODE_3, NODE_1, CONNECTION_ID_1);

        assertFalse(graph1.equals(graph2));

        graph1.update(graph2.generateNewLocalLSA());
        graph2.update(graph1.generateNewLocalLSA());

        assertEquals(graph2.generateNewLocalLSA().getGraphHashCode(), graph2.hashCode());

        assertEquals(graph1, graph2);
    }

    /**
     * Test if channels are usable in both directions.
     */
    public final void testDirectedEdges() {
        networkGraph = new TopologyMap(NODE_1);

        LinkStateAdvertisement linkStateAdvertisement =
            LinkStateAdvertisement.createUpdateLsa(NODE_2, NODE_2_NAME, true, ARBITRARY_INT,
                networkGraph.hashCode(), true,
                Arrays.asList(new TopologyLink[] {
                    new TopologyLink(NODE_2, NODE_1, CONNECTION_ID_1),
                    new TopologyLink(NODE_2, NODE_3, CONNECTION_ID_1)
                }));

        assertTrue(LSA_CAUSED_NO_UPDATE, networkGraph.update(linkStateAdvertisement));

        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_1));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_1, NODE_2));
        assertTrue(GRAPH_SHOULD_CONTAIN_CHANNEL, networkGraph.containsLinkBetween(NODE_2, NODE_3));
        assertFalse(GRAPH_CONTAINS_CHANNEL, networkGraph.containsLinkBetween(NODE_3, NODE_2));

    }

    /**
     * Test cloning.
     */
    public final void testCloning() {
        TopologyLink c1 = new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1);
        TopologyLink c2 = new TopologyLink(NODE_2, NODE_3, CONNECTION_ID_1);
        assertFalse(c1.equals(c2));
        assertEquals(c1, c1.clone());
        assertEquals(c1.clone(), c1.clone());
        assertNotSame(c1, c1.clone());
        assertNotSame(c1.clone(), c1.clone());

        TopologyNode n1 = new TopologyNode(NODE_1);
        TopologyNode n2 = new TopologyNode(NODE_2);

        assertFalse(n1.equals(n2));
        assertEquals(n1, n1.clone());
        assertEquals(n1.clone(), n1.clone());
        assertNotSame(n1, n1.clone());
        assertNotSame(n1.clone(), n1.clone());

    }

    /**
     * Tests for {@link TopologyLink#equals(Object)}.
     */
    public final void testEqualChannels() {
        // should be equal
        assertEquals(new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1), new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1));

        // should not be equal
        assertFalse((new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1)).equals(new TopologyLink(NODE_3, NODE_1, CONNECTION_ID_1)));

        // should not be equal
        assertFalse((new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_1)).equals(new TopologyLink(NODE_1, NODE_3, CONNECTION_ID_2)));

    }

    /**
     * Test for {@link TopologyMap#getAllLinksBetween(TopologyNode, TopologyNode)}.
     */
    public final void testAvailableChannels() {
        TopologyMap graph = new TopologyMap(NODE_2);
        graph.addNode(NODE_1);
        assertEquals(0, graph.getAllLinksBetween(graph.getNode(NODE_2), graph.getNode(NODE_1)).size());

        graph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        assertEquals(0, graph.getAllLinksBetween(graph.getNode(NODE_2), graph.getNode(NODE_1)).size());
        assertEquals(1, graph.getAllLinksBetween(graph.getNode(NODE_1), graph.getNode(NODE_2)).size());

        graph.addLink(NODE_1, NODE_2, CONNECTION_ID_2);
        assertEquals(0, graph.getAllLinksBetween(graph.getNode(NODE_2), graph.getNode(NODE_1)).size());
        assertEquals(2, graph.getAllLinksBetween(graph.getNode(NODE_1), graph.getNode(NODE_2)).size());

        graph.addLink(NODE_2, NODE_1, CONNECTION_ID_1);
        assertEquals(1, graph.getAllLinksBetween(graph.getNode(NODE_2), graph.getNode(NODE_1)).size());
        assertEquals(2, graph.getAllLinksBetween(graph.getNode(NODE_1), graph.getNode(NODE_2)).size());

        graph.addLink(NODE_2, NODE_1, CONNECTION_ID_2);
        assertEquals(2, graph.getAllLinksBetween(graph.getNode(NODE_2), graph.getNode(NODE_1)).size());
        assertEquals(2, graph.getAllLinksBetween(graph.getNode(NODE_1), graph.getNode(NODE_2)).size());
    }

    /**
     * Test for {@link TopologyMap#removeLink()}.
     */
    public final void testRemoveChannel() {
        TopologyMap graph = new TopologyMap(NODE_2);
        graph.addNode(NODE_1);

        assertFalse(graph.containsLink(NODE_1, NODE_2, CONNECTION_ID_1));

        graph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        graph.addLink(NODE_1, NODE_2, CONNECTION_ID_2);
        assertTrue(graph.containsLink(NODE_1, NODE_2, CONNECTION_ID_1));
        assertFalse(graph.containsLink(NODE_1, NODE_2, CONNECTION_ID_3));

        assertTrue(graph.removeLink(NODE_1, NODE_2, CONNECTION_ID_1));
        assertFalse(graph.removeLink(NODE_1, NODE_2, CONNECTION_ID_1));
        assertFalse(graph.containsLink(NODE_1, NODE_2, CONNECTION_ID_1));
        assertTrue(graph.containsLink(NODE_1, NODE_2, CONNECTION_ID_2));
    }

    /**
     * Test for {@link TopologyMap#removeNode()}.
     */
    public final void testRemoveNode() {
        TopologyMap graph = new TopologyMap(NODE_2);
        graph.addNode(NODE_1);
        graph.removeNode(NODE_2);
        graph.removeNode((TopologyNode) null);
        assertTrue(graph.containsNode(NODE_1));
        graph.removeNode(NODE_1);
        assertFalse(graph.containsNode(NODE_1));
    }

    /**
     * Test for {@link TopologyMap#removeNode()}.
     */
    public final void testRemoveNode2() {
        TopologyMap graph = new TopologyMap(NODE_2);
        graph.addNode(NODE_1);
        graph.addNode(NODE_3);

        graph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);
        graph.addLink(NODE_2, NODE_1, CONNECTION_ID_1);

        graph.addLink(NODE_2, NODE_3, CONNECTION_ID_1);
        graph.addLink(NODE_3, NODE_2, CONNECTION_ID_1);

        assertEquals(4, graph.getLinkCount());
        graph.removeNode(NODE_1);
        graph.removeNode(new TopologyNode(NODE_1));
        assertEquals(2, graph.getNodeCount());
        assertEquals(2, graph.getLinkCount());

    }

    /**
     * Tests for {@link TopologyNode#equals(Object)}.
     */
    public final void testEqualNodes() {
        // should be equal
        assertEquals(new TopologyNode(NODE_1), new TopologyNode(NODE_1));

        // should not be equal
        assertFalse((new TopologyNode(NODE_1)).equals(new TopologyNode(NODE_2)));

    }

    /**
     * Tests for {@link NetworkGraph#containsAnyChannel(NodeIdentifier, NodeIdentifier).
     */
    public final void testContainsAnyChannel() {
        networkGraph = new TopologyMap(NODE_5);

        // TODO review this test; some of it seem redundant

        assertFalse(networkGraph.containsLinkBetween(null, null));
        assertFalse(networkGraph.containsLinkBetween(NODE_5, null));
        assertFalse(networkGraph.containsLinkBetween(NODE_1, null));
        assertFalse(networkGraph.containsLinkBetween(null, NODE_5));
        assertFalse(networkGraph.containsLinkBetween(null, NODE_1));

        networkGraph.addLink(NODE_1, NODE_5, CONNECTION_ID_1);
        // TODO review: changed to assertTrue() after switch to connection ids; why was this
        // assertFalse() before? -- misc_ro
        assertTrue(networkGraph.containsLinkBetween(NODE_1, NODE_5));

        networkGraph.addNode(NODE_1);

        assertTrue(networkGraph.containsLink(NODE_1, NODE_5, CONNECTION_ID_1));
        assertFalse(networkGraph.containsLink(NODE_1, NODE_5, CONNECTION_ID_2));
    }

    /**
     * Tests for {@link TopologyMap#containsNode(InstanceNodeSessionId)}.
     * 
     * @throws IdentifierException on unexpected errors
     */
    public final void testContainsNode() throws IdentifierException {
        networkGraph = new TopologyMap(NODE_5);

        assertTrue(networkGraph.containsNode(NODE_5));
        assertFalse(networkGraph.containsNode(NODE_4));
        // also test for different instance node session ids of the existing instance node ids
        assertFalse(networkGraph.containsNode(NodeIdentifierTestUtils.createTestInstanceNodeSessionId(NODE_5.getInstanceNodeIdString())));
        assertFalse(networkGraph.containsNode(NodeIdentifierTestUtils.createTestInstanceNodeSessionId(NODE_4.getInstanceNodeIdString())));
        // test with round-trip deserialized node id
        assertTrue(networkGraph.containsNode(NodeIdentifierTestUtils.getTestNodeIdentifierService().parseInstanceNodeSessionIdString(
            NODE_5.getInstanceNodeSessionIdString())));
    }

    /**
     * Test for {@link TopologyMap#getShortestPath(InstanceNodeSessionId, InstanceNodeSessionId)}.
     */
    public final void testShortestPathComputation1() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        networkGraph.addLink(NODE_2, NODE_3, CONNECTION_ID_1);

        networkGraph.addLink(NODE_3, NODE_1, CONNECTION_ID_1);

        NetworkRoute route = networkGraph.getShortestPath(NODE_1, NODE_3);

        assertEquals(2, route.getNodes().size());
        assertEquals(NODE_2, route.getNodes().get(0));
        assertEquals(NODE_3, route.getNodes().get(1));
        assertEquals(route.getPath().size(), route.getNodes().size());

        route = networkGraph.getShortestPath(NODE_1, NODE_2);

        assertEquals(1, route.getNodes().size());
        assertEquals(NODE_2, route.getNodes().get(0));

    }

    /**
     * Test for {@link TopologyMap#getNode(InstanceNodeSessionId)}.
     */
    public final void testFindNetworkNode() {
        networkGraph = new TopologyMap(NODE_1);

        assertTrue(networkGraph.getNode(NODE_1) instanceof TopologyNode);
        assertNull(networkGraph.getNode(NODE_2));
    }

    /**
     * Simple shortest path implementation.
     */
    public final void testShortestPathComputation2() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        networkGraph.addLink(NODE_2, NODE_1, CONNECTION_ID_1);

        NetworkRoute route = networkGraph.getShortestPath(NODE_1, NODE_2);

        assertEquals(1, route.getNodes().size());
        assertEquals(NODE_2, route.getNodes().get(0));
        assertEquals(route.getPath().size(), route.getNodes().size());

        route = networkGraph.getShortestPath(NODE_2, NODE_1);

        assertEquals(1, route.getNodes().size());
        assertEquals(NODE_1, route.getNodes().get(0));

    }

    /**
     * Simple shortest path implementation.
     */
    public final void testShortestPathComputation3() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_1);
        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);
        networkGraph.addNode(NODE_4);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        networkGraph.addLink(NODE_2, NODE_3, CONNECTION_ID_1);

        networkGraph.addLink(NODE_3, NODE_4, CONNECTION_ID_1);

        NetworkRoute route = networkGraph.getShortestPath(NODE_1, NODE_4);

        assertEquals(3, route.getNodes().size());
        assertEquals(NODE_2, route.getNodes().get(0));
        assertEquals(NODE_3, route.getNodes().get(1));
        assertEquals(NODE_4, route.getNodes().get(2));

        networkGraph.addLink(NODE_2, NODE_4, CONNECTION_ID_1);

        route = networkGraph.getShortestPath(NODE_1, NODE_4);

        assertEquals(2, route.getNodes().size());
        assertEquals(NODE_2, route.getNodes().get(0));
        assertEquals(NODE_4, route.getNodes().get(1));
        assertEquals(route.getPath().size(), route.getNodes().size());

        route = networkGraph.getShortestPath(NODE_1, NODE_5);

    }

    /**
     * 
     */
    public final void testLinkStateAdvertisementProduction() {
        networkGraph = new TopologyMap(NODE_1);

        networkGraph.addNode(NODE_2);
        networkGraph.addNode(NODE_3);
        networkGraph.addNode(NODE_4);

        networkGraph.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        networkGraph.addLink(NODE_1, NODE_3, CONNECTION_ID_1);

        networkGraph.addLink(NODE_1, NODE_4, CONNECTION_ID_1);

        LinkStateAdvertisement lsa = networkGraph.generateNewLocalLSA();

        assertTrue(lsa.getOwner().equals(networkGraph.getLocalNodeId()));
        assertEquals("Collection sizes are expected to be the same.",
            lsa.getLinks().size(),
            networkGraph.getSuccessors(NODE_1).size());

        for (TopologyLink channel : lsa.getLinks()) {
            assertTrue(networkGraph.getPredecessors(networkGraph.getNode(channel.getDestination()))
                .contains(networkGraph.getNode(networkGraph.getLocalNodeId())));
        }

    }

    /**
     * 
     */
    public final void testEquals() {
        TopologyMap graph1 = new TopologyMap(NODE_1);
        TopologyMap graph2 = new TopologyMap(NODE_2);

        assertFalse(graph1.equals(graph2));

        graph1.addNode(NODE_2);

        assertFalse(graph1.equals(graph2));

        graph2.addNode(NODE_1);

        assertTrue(graph1.equals(graph2));

        graph1.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        assertFalse(graph1.equals(graph2));

        graph2.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        assertTrue(graph1.equals(graph2));
    }

    /**
     * @throws InterruptedException on interruption
     */
    public final void testLinkStateUpdate() throws InterruptedException {
        TopologyMap graph1 = new TopologyMap(NODE_1);
        // cause timestamp-based sequence numbers to be different
        Thread.sleep(SHORT_TIMESTAMP_CHANGING_DELAY);
        TopologyMap graph2 = new TopologyMap(NODE_2);
        // crude approach to ensure different timestamps; it would be better to inject an artificial
        // time source for testing -- misc_ro, June 2013
        while (graph2.getSequenceNumberOfNode(NODE_2) == graph1.getSequenceNumberOfNode(NODE_1)) {
            Thread.sleep(SHORT_TIMESTAMP_CHANGING_DELAY);
            graph2 = new TopologyMap(NODE_2);
        }

        // get the highest current sequence number (assuming node 3 was assigned the highest one)
        long node1OriginalSeqNo = graph1.getSequenceNumberOfNode(NODE_1);
        long node2OriginalSeqNo = graph2.getSequenceNumberOfNode(NODE_2);

        assertFalse(node1OriginalSeqNo == node2OriginalSeqNo);
        assertFalse(graph1.equals(graph2));

        LinkStateAdvertisement lsa = graph1.generateNewLocalLSA();

        // LSA generation should have increased the node 1 sequence number in graph 1
        long node1NewSeqNo = graph1.getSequenceNumberOfNode(NODE_1);
        assertEquals(node1NewSeqNo, lsa.getSequenceNumber());
        assertTrue(node1NewSeqNo > node1OriginalSeqNo);

        // verify graph update
        assertTrue(graph2.update(lsa));
        assertEquals(node1NewSeqNo, graph2.getSequenceNumberOfNode(NODE_1));

        lsa = graph2.generateNewLocalLSA();

        // LSA generation should have increased the node 2 sequence number in graph 2
        long node2NewSeqNo = graph2.getSequenceNumberOfNode(NODE_2);
        assertEquals(node2NewSeqNo, lsa.getSequenceNumber());
        assertTrue(node2NewSeqNo > node2OriginalSeqNo);

        // verify graph update
        assertTrue(graph1.update(lsa));
        assertEquals(node2NewSeqNo, graph1.getSequenceNumberOfNode(NODE_2));

        assertTrue(graph1.equals(graph2));

        graph1.addNode(NODE_3);
        graph1.addLink(NODE_1, NODE_3, CONNECTION_ID_1);

        assertFalse(graph1.equals(graph2));

        lsa = graph1.generateNewLocalLSA();
        assertTrue(lsa.getSequenceNumber() > node1NewSeqNo);
        node1NewSeqNo = graph1.getSequenceNumberOfNode(NODE_1);
        assertEquals(node1NewSeqNo, lsa.getSequenceNumber());

        assertTrue(graph2.update(lsa));
        assertEquals(node1NewSeqNo, graph2.getSequenceNumberOfNode(NODE_1));
        assertTrue(graph1.equals(graph2));

        graph2.addLink(NODE_2, NODE_1, CONNECTION_ID_1);

        assertFalse(graph2.equals(graph1));

        lsa = graph2.generateNewLocalLSA();
        graph1.update(lsa);

        node2NewSeqNo = graph2.getSequenceNumberOfNode(NODE_2);
        assertEquals(node2NewSeqNo, lsa.getSequenceNumber());

        assertTrue(graph2.equals(graph1));
        assertEquals(node1NewSeqNo, graph2.getSequenceNumberOfNode(NODE_1));
        assertEquals(node2NewSeqNo, graph2.getSequenceNumberOfNode(NODE_2));
        assertEquals(graph1.getSequenceNumberOfNode(NODE_3), graph2.getSequenceNumberOfNode(NODE_3));

        // log.info(NetworkFormatter.summary(graph2));
        // log.info(NetworkFormatter.summary(graph1));
    }

    /**
     * Test for {@link NetworkGraph#getSuccessors())}.
     */
    public final void testGetSuccessors() {
        TopologyMap graph1 = new TopologyMap(NODE_1);

        graph1.addNode(NODE_2);
        graph1.addNode(NODE_3);
        graph1.addNode(NODE_4);

        assertEquals(0, graph1.getSuccessors().size());

        graph1.addLink(NODE_1, NODE_2, CONNECTION_ID_1);

        assertEquals(1, graph1.getSuccessors().size());
        assertTrue(TopologyMap.toNodeIdentifiers(graph1.getSuccessors()).contains(NODE_2));
        assertFalse(TopologyMap.toNodeIdentifiers(graph1.getSuccessors()).contains(NODE_1));

        graph1.addLink(NODE_3, NODE_1, CONNECTION_ID_1);

        assertFalse(TopologyMap.toNodeIdentifiers(graph1.getSuccessors()).contains(NODE_3));
        assertTrue(TopologyMap.toNodeIdentifiers(graph1.getSuccessors()).contains(NODE_2));

    }
}
