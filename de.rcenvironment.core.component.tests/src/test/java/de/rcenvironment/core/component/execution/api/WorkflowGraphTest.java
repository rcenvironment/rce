/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.utils.common.StringUtils;
import junit.framework.Assert;

/**
 * Tests for {link WorkflowGraph}.
 * 
 * @author Sascha Zur
 * @author Doreen Seider
 * @author Tobias Brieden
 */
public class WorkflowGraphTest {

    private static final String DUMMY = "dummy";

    private static final String INPUT_PREFIX = "inp_";

    private static final String INPUT0 = "inp_0";

    private static final String INPUT1 = "inp_1";

    private static final String OUTPUT_PREFIX = "out_";

    private static final String OUTPUT0 = "out_0";

    private static final String OUTPUT1 = "out_1";

    private static final String NODE = "node";

    private static final String SINK_NODE = "sinkNode";

    private static final String NODE3 = "node3";

    private static final String NODE2 = "node2";

    private static final String NODE1 = "node1";

    private static final String SINK_NODE0 = "sinkNode0";

    private static final String NODE0 = "node0";

    private static final String SINK_NODE1 = "sinkNode1";

    private static final String OUTER_LOOP_NODE = "outerLoopNode";

    private Map<String, WorkflowGraphNode> nodeNamesToNodes;

    /** Test. */
    @Test
    public void testWorkflowGraphFailure() {
        WorkflowGraph graph = null;

        graph = createCircleWorkflowGraph();
        WorkflowGraphNode driver;
        try {
            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());

            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE1).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE2).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            graph = createCircleWorkflowGraphWithInnerLoopBack();
            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE1).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE2).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            graph = createTwoSinksWorkflowGraph();
            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(OUTER_LOOP_NODE).getExecutionIdentifier(), driver.getExecutionIdentifier());

            graph = createTwoSinksWithInnerLoopWorkflowGraph();
            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE3).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(OUTER_LOOP_NODE).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(OUTER_LOOP_NODE).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE1).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(), driver.getExecutionIdentifier());

            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE2).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(SINK_NODE1).getExecutionIdentifier(), driver.getExecutionIdentifier());

            graph = createReducedInputsWorkflowGraph();
            driver = graph.getLoopDriver(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());
            Assert.assertEquals(nodeNamesToNodes.get(OUTER_LOOP_NODE).getExecutionIdentifier(), driver.getExecutionIdentifier());
            Map<String, Set<Deque<WorkflowGraphHop>>> hops =
                graph.getHopsToTraverseOnFailure(nodeNamesToNodes.get(NODE0).getExecutionIdentifier());
            Assert.assertEquals(1, hops.keySet().size());
            Assert.assertEquals(1, hops.get(hops.keySet().iterator().next()).size());
        } catch (ComponentExecutionException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    /** Test. */
    @Test
    public void testWorkflowGraphReset() {
        WorkflowGraph graph = null;
        Set<Deque<WorkflowGraphHop>> hops = null;

        graph = createCircleWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        Deque<String> expectedHops = new LinkedList<>();
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE0).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE0).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE2).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE2).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier()));
        assertWorkflowGraphHops(expectedHops, hops.iterator().next());

        graph = createCircleWorkflowGraphWithInnerLoopBack();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        expectedHops = new LinkedList<>();
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE0).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE0).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE2).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE2).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier()));
        assertWorkflowGraphHops(expectedHops, hops.iterator().next());
        assertEquals(hops.size(), 1);

        graph = createTwoSinksWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        assertEquals(hops.size(), 0);

        graph = createTwoSinksWithInnerLoopWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        assertEquals(hops.size(), 1);
        expectedHops = new LinkedList<>();
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier(),
            INPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier()));
        expectedHops.add(StringUtils.escapeAndConcat(OUTPUT0, nodeNamesToNodes.get(NODE1).getExecutionIdentifier(),
            INPUT1, nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier()));
        assertWorkflowGraphHops(expectedHops, hops.iterator().next());
    }

    /**
     * 
     * @return A graph were two sinks are in a row with no nested loop, so no resetting Deque should be produced.
     */
    private WorkflowGraph createTwoSinksWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode outerLoopNode = createNewNode(1, 1, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, outerLoopNode);
        nodes.put(outerLoopNode.getExecutionIdentifier(), outerLoopNode);

        for (int i = 0; i < 2; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(SINK_NODE1), 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP));

        generateAndPutEdgeKeys(edges, edgesSet);
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A graph were two sinks are in a row with no nested loop, so no resetting Deque should be produced.
     */
    private WorkflowGraph createReducedInputsWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode driverNode = createNewNode(1, 1, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, driverNode);
        nodes.put(driverNode.getExecutionIdentifier(), driverNode);

        WorkflowGraphNode node0 = createNewNode(1, 2, false);
        nodeNamesToNodes.put(NODE + 0, node0);
        nodes.put(node0.getExecutionIdentifier(), node0);

        WorkflowGraphNode node1 = createNewNode(2, 1, false);
        nodeNamesToNodes.put(NODE + 1, node1);
        nodes.put(node1.getExecutionIdentifier(), node1);

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 1, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(NODE1), 1, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP));

        generateAndPutEdgeKeys(edges, edgesSet);
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A graph where two sinks are in a row and both have a nested loop. The reset should contain 2 hops.
     */
    private WorkflowGraph createTwoSinksWithInnerLoopWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode outerLoopNode = createNewNode(1, 2, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, outerLoopNode);
        nodes.put(outerLoopNode.getExecutionIdentifier(), outerLoopNode);

        for (int i = 0; i < 2; i++) {
            WorkflowGraphNode node = createNewNode(3, 2, true);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 4; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE0), 1, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 1, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(SINK_NODE1), 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE1), 1, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 1, EndpointCharacter.OUTER_LOOP,
            nodeNamesToNodes.get(NODE3), 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE3), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, EndpointCharacter.SAME_LOOP));
        generateAndPutEdgeKeys(edges, edgesSet);
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * Creates a cyclic graph.
     * 
     * @return A circle of nodes connected to a sink, so the circle should be reset.
     */
    private WorkflowGraph createCircleWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 3; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        WorkflowGraphEdge e1 =
            createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP,
                nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e2 = createEdge(nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e3 = createEdge(nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e4 = createEdge(nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP);

        String key1 = WorkflowGraph.createEdgeKey(e1);
        String key2 = WorkflowGraph.createEdgeKey(e2);
        String key3 = WorkflowGraph.createEdgeKey(e3);
        String key4 = WorkflowGraph.createEdgeKey(e4);

        edges.put(key1, new HashSet<WorkflowGraphEdge>());
        edges.get(key1).add(e1);
        edges.put(key2, new HashSet<WorkflowGraphEdge>());
        edges.get(key2).add(e2);
        edges.put(key3, new HashSet<WorkflowGraphEdge>());
        edges.get(key3).add(e3);
        edges.put(key4, new HashSet<WorkflowGraphEdge>());
        edges.get(key4).add(e4);
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * Creates a cyclic graph.
     * 
     * @return A circle of nodes connected to a sink, so the circle should be resetted.
     */
    private WorkflowGraph createCircleWorkflowGraphWithInnerLoopBack() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        WorkflowGraphNode node0 = createNewNode(2, 1, false);
        nodeNamesToNodes.put(NODE + 0, node0);
        nodes.put(node0.getExecutionIdentifier(), node0);
        WorkflowGraphNode node = createNewNode(1, 1, false);
        nodeNamesToNodes.put(NODE + 1, node);
        nodes.put(node.getExecutionIdentifier(), node);
        WorkflowGraphNode lastNode = createNewNode(1, 2, false);
        nodeNamesToNodes.put(NODE + 2, lastNode);
        nodes.put(lastNode.getExecutionIdentifier(), lastNode);
        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        WorkflowGraphEdge e1 =
            createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP,
                nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e2 = createEdge(nodeNamesToNodes.get(NODE0), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e3 = createEdge(nodeNamesToNodes.get(NODE1), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e4 = createEdge(nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(SINK_NODE0), 0, EndpointCharacter.SAME_LOOP);
        WorkflowGraphEdge e5 = createEdge(nodeNamesToNodes.get(NODE2), 0, EndpointCharacter.SAME_LOOP,
            nodeNamesToNodes.get(NODE0), 1, EndpointCharacter.SAME_LOOP);
        String key1 = WorkflowGraph.createEdgeKey(e1);
        String key2 = WorkflowGraph.createEdgeKey(e2);
        String key3 = WorkflowGraph.createEdgeKey(e3);
        String key4 = WorkflowGraph.createEdgeKey(e4);
        edges.put(key1, new HashSet<WorkflowGraphEdge>());
        edges.get(key1).add(e1);
        edges.put(key2, new HashSet<WorkflowGraphEdge>());
        edges.get(key2).add(e2);
        edges.put(key3, new HashSet<WorkflowGraphEdge>());
        edges.get(key3).add(e3);
        edges.put(key4, new HashSet<WorkflowGraphEdge>());
        edges.get(key4).add(e4);
        edges.get(key4).add(e5);
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * Tests the reset behavior for a loop that has sub-loops. A sub-loop is created if a component like the evaluation memory component is
     * used. Such a component has inputs and outputs of type 'outer loop' and 'same loop', but it doesn't control the loop.
     *
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testWorkflowGraphResetInLoopWithSubLoops() throws ComponentExecutionException {

        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode outerDriverNode = createNewNode(1, 1, true, "outer-driver");
        nodes.put(outerDriverNode.getExecutionIdentifier(), outerDriverNode);
        WorkflowGraphNode nestedDriverNode = createNewNode(1, 1, true, "nested-driver");
        nodes.put(nestedDriverNode.getExecutionIdentifier(), nestedDriverNode);
        WorkflowGraphNode evalMemNode = createNewNode(2, 2, false, "eval-mem");
        nodes.put(evalMemNode.getExecutionIdentifier(), evalMemNode);
        WorkflowGraphNode node = createNewNode(1, 1, false, "node");
        nodes.put(node.getExecutionIdentifier(), node);

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(outerDriverNode, 0, EndpointCharacter.SAME_LOOP,
            nestedDriverNode, 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(nestedDriverNode, 0, EndpointCharacter.SAME_LOOP,
            evalMemNode, 0, EndpointCharacter.OUTER_LOOP));
        edgesSet.add(createEdge(evalMemNode, 1, EndpointCharacter.SAME_LOOP,
            node, 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(node, 0, EndpointCharacter.SAME_LOOP,
            evalMemNode, 1, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(evalMemNode, 0, EndpointCharacter.OUTER_LOOP,
            nestedDriverNode, 0, EndpointCharacter.SAME_LOOP));
        edgesSet.add(createEdge(nestedDriverNode, 0, EndpointCharacter.OUTER_LOOP,
            outerDriverNode, 0, EndpointCharacter.SAME_LOOP));

        generateAndPutEdgeKeys(edges, edgesSet);

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        Set<Deque<WorkflowGraphHop>> hops = graph.getHopsToTraverseWhenResetting(nestedDriverNode.getExecutionIdentifier());
        assertEquals(2, hops.size());

        Iterator<Deque<WorkflowGraphHop>> iterator = hops.iterator();
        Deque<WorkflowGraphHop> actualHops1 = iterator.next();
        Deque<WorkflowGraphHop> actualHops2 = iterator.next();

        // the order of the hops queues is not fixed
        if (actualHops1.size() == 2 && actualHops2.size() == 3) {
            // since this is not the expected order, switch the queues for the following validation
            Deque<WorkflowGraphHop> tmp = actualHops1;
            actualHops1 = actualHops2;
            actualHops2 = tmp;
        } else if (actualHops1.size() != 3 || actualHops2.size() != 2) {
            fail();
        }

        assertEquals(3, actualHops1.size());
        Deque<String> expectedHops1 = new LinkedList<>();
        expectedHops1.add(StringUtils.escapeAndConcat(OUTPUT0, nestedDriverNode.getExecutionIdentifier(),
            INPUT0, evalMemNode.getExecutionIdentifier()));
        expectedHops1.add(StringUtils.escapeAndConcat(OUTPUT1, evalMemNode.getExecutionIdentifier(),
            INPUT0, node.getExecutionIdentifier()));
        expectedHops1.add(StringUtils.escapeAndConcat(DUMMY, node.getExecutionIdentifier(),
            DUMMY, DUMMY));
        assertWorkflowGraphHops(expectedHops1, actualHops1);

        assertEquals(2, actualHops2.size());
        Deque<String> expectedHops2 = new LinkedList<>();
        expectedHops2.add(StringUtils.escapeAndConcat(OUTPUT0, nestedDriverNode.getExecutionIdentifier(),
            INPUT0, evalMemNode.getExecutionIdentifier()));
        expectedHops2.add(StringUtils.escapeAndConcat(OUTPUT0, evalMemNode.getExecutionIdentifier(),
            INPUT0, nestedDriverNode.getExecutionIdentifier()));
        assertWorkflowGraphHops(expectedHops2, actualHops2);
    }

    /**
     * Checks if two queues represent the same WorkflowGraphHops.
     */
    private void assertWorkflowGraphHops(Deque<String> expectedHops, Deque<WorkflowGraphHop> actualHops) {
        assertEquals(expectedHops.size(), actualHops.size());
        Iterator<WorkflowGraphHop> hopsIterator = actualHops.iterator();
        while (hopsIterator.hasNext()) {
            WorkflowGraphHop hop = hopsIterator.next();
            String[] hopParts = StringUtils.splitAndUnescape(expectedHops.poll());
            assertEquals(hopParts[1], hop.getHopExecutionIdentifier());

            // if the hop sequence is not circular, the last hop will point to a dummy node
            if (hopParts[0].equals(DUMMY) && hopParts[2].equals(DUMMY) && hopParts[3].equals(DUMMY)) {
                assertTrue(hop.getHopOuputName().startsWith(DUMMY));
                assertTrue(hop.getTargetInputName().startsWith(DUMMY));
                assertTrue(hop.getTargetExecutionIdentifier().startsWith(DUMMY));
            } else {
                assertEquals(hopParts[0], hop.getHopOuputName());
                assertEquals(hopParts[2], hop.getTargetInputName());
                assertEquals(hopParts[3], hop.getTargetExecutionIdentifier());
            }

        }
    }

    private static WorkflowGraphEdge createEdge(WorkflowGraphNode source, int outputNumber, EndpointCharacter outputType,
        WorkflowGraphNode target,
        int inputNumber, EndpointCharacter inputType) {
        String outputIdentifier = null;
        for (String output : source.getOutputIdentifiers()) {
            if (source.getEndpointName(output).equals(OUTPUT_PREFIX + outputNumber)) {
                outputIdentifier = output;
            }
        }
        String inputIdentifier = null;
        for (String input : target.getInputIdentifiers()) {
            if (target.getEndpointName(input).equals(INPUT_PREFIX + inputNumber)) {
                inputIdentifier = input;
            }
        }
        if (outputIdentifier != null && inputIdentifier != null) {
            return new WorkflowGraphEdge(source.getExecutionIdentifier(), outputIdentifier, outputType, target.getExecutionIdentifier(),
                inputIdentifier, inputType);
        }
        return null;
    }

    private static WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isDriver) {
        return createNewNode(inputCount, outputCount, isDriver, RandomStringUtils.randomAlphabetic(5));
    }

    private static WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isDriver, String name) {
        Set<String> inputs = new HashSet<>();
        for (int i = 0; i < inputCount; i++) {
            inputs.add(UUID.randomUUID().toString());
        }
        Set<String> outputs = new HashSet<>();
        for (int i = 0; i < outputCount; i++) {
            outputs.add(UUID.randomUUID().toString());
        }
        Map<String, String> endpointnames = new HashMap<>();
        int i = 0;
        for (String input : inputs) {
            endpointnames.put(input, INPUT_PREFIX + i);
            i++;
        }
        i = 0;
        for (String output : outputs) {
            endpointnames.put(output, OUTPUT_PREFIX + i);
            i++;
        }
        return new WorkflowGraphNode(UUID.randomUUID().toString(), inputs, outputs, endpointnames, isDriver, false, name);
    }

    private void generateAndPutEdgeKeys(Map<String, Set<WorkflowGraphEdge>> edges, Set<WorkflowGraphEdge> edgesSet) {
        for (WorkflowGraphEdge egdge : edgesSet) {
            String key = WorkflowGraph.createEdgeKey(egdge);
            if (!edges.containsKey(key)) {
                edges.put(key, new HashSet<WorkflowGraphEdge>());
            }
            edges.get(key).add(egdge);
        }
    }
}
