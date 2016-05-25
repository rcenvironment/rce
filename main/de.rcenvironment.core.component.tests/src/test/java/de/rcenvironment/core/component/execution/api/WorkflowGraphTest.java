/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import junit.framework.Assert;

/**
 * Test for the {link WorkflowGraph}'s method getHopsToTraverseWhenResetting.
 * 
 * @author Sascha Zur
 */
public class WorkflowGraphTest {

    private static final String ZERO = "0";

    private static final String OUTPUT = "output";

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
            Assert.fail("Unexpected exception");
        }
    }

    /** Test. */
    @Test
    public void testWorkflowGraphReset() {
        WorkflowGraph graph = null;
        Map<String, Set<Deque<WorkflowGraphHop>>> hops = null;
        Map<String, List<Integer>> hopsCount = new HashMap<>();
        List<Integer> counts = new LinkedList<>();
        try {
            graph = createCircleWorkflowGraph();
            hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
            hopsCount = new HashMap<>();
            counts = new LinkedList<>();
            counts.add(4);
            hopsCount.put(OUTPUT + ZERO, counts);
            checkResultCorrect(hops, 1, new int[] { 1 }, hopsCount);

            graph = createCircleWorkflowGraphWithInnerLoopBack();
            hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
            hopsCount = new HashMap<>();
            counts = new LinkedList<>();
            counts.add(4);
            hopsCount.put(OUTPUT + ZERO, counts);
            checkResultCorrect(hops, 1, new int[] { 1 }, hopsCount);

            graph = createTwoSinksWorkflowGraph();
            hops =
                graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
            hopsCount = new HashMap<>();
            counts = new LinkedList<>();
            hopsCount.put(OUTPUT + ZERO, counts);
            checkResultCorrect(hops, 1, new int[] { 0 }, hopsCount);

            graph = createTwoSinksWithInnerLoopWorkflowGraph();
            hops =
                graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
            hopsCount = new HashMap<>();
            counts = new LinkedList<>();
            counts.add(2);
            hopsCount.put(OUTPUT + ZERO, counts);
            hopsCount.put(OUTPUT + "1", new LinkedList<Integer>());
            checkResultCorrect(hops, 2, new int[] { 1, 0 }, hopsCount);
        } catch (ComponentExecutionException e) {
            Assert.fail("Unexpected exception");
        }
    }

    /**
     * 
     * @return A graph were two sinks are in a row with no nested loop, so no resetting Deque should be produced.
     */
    private WorkflowGraph createTwoSinksWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode outerLoopNode = createNewNode(1, 1, true, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, outerLoopNode);
        nodes.put(outerLoopNode.getExecutionIdentifier(), outerLoopNode);

        for (int i = 0; i < 2; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true, false);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE1), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));

        for (WorkflowGraphEdge e : edgesSet) {
            String key = WorkflowGraph.createEdgeKey(e);
            edges.put(key, new HashSet<WorkflowGraphEdge>());
            edges.get(key).add(e);
        }
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A graph were two sinks are in a row with no nested loop, so no resetting Deque should be produced.
     */
    private WorkflowGraph createReducedInputsWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode driverNode = createNewNode(1, 1, true, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, driverNode);
        nodes.put(driverNode.getExecutionIdentifier(), driverNode);

        WorkflowGraphNode node0 = createNewNode(1, 2, false, false);
        nodeNamesToNodes.put(NODE + 0, node0);
        nodes.put(node0.getExecutionIdentifier(), node0);

        WorkflowGraphNode node1 = createNewNode(2, 1, false, false);
        nodeNamesToNodes.put(NODE + 1, node1);
        nodes.put(node1.getExecutionIdentifier(), node1);

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 1, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE1), 1, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));

        for (WorkflowGraphEdge e : edgesSet) {
            String key = WorkflowGraph.createEdgeKey(e);
            edges.put(key, new HashSet<WorkflowGraphEdge>());
            edges.get(key).add(e);
        }
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A graph where two sinks are in a row and both have a nested loop. The reset should . contain 2 hops.
     */
    private WorkflowGraph createTwoSinksWithInnerLoopWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        WorkflowGraphNode outerLoopNode = createNewNode(1, 2, true, true);
        nodeNamesToNodes.put(OUTER_LOOP_NODE, outerLoopNode);
        nodes.put(outerLoopNode.getExecutionIdentifier(), outerLoopNode);

        for (int i = 0; i < 2; i++) {
            WorkflowGraphNode node = createNewNode(3, 2, true, false);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 4; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        Set<WorkflowGraphEdge> edgesSet = new HashSet<>();
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 1, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 1, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE1), 0, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE1), 1, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 1, LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE3), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE3), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.InnerLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 2, LoopComponentConstants.LoopEndpointType.InnerLoopEndpoint.name()));
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, LoopComponentConstants.LoopEndpointType.InnerLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE1), 2, LoopComponentConstants.LoopEndpointType.InnerLoopEndpoint.name()));

        for (WorkflowGraphEdge e : edgesSet) {
            String key = WorkflowGraph.createEdgeKey(e);
            edges.put(key, new HashSet<WorkflowGraphEdge>());
            edges.get(key).add(e);
        }
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A circle of nodes connected to a sink, so the circle should be resetted.
     */
    private WorkflowGraph createCircleWorkflowGraph() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true, false);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        for (int i = 0; i < 3; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, false, false);
            nodeNamesToNodes.put(NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }

        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        WorkflowGraphEdge e1 =
            createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
                nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e2 = createEdge(nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e3 = createEdge(nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e4 = createEdge(nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());

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
     * 
     * @return A circle of nodes connected to a sink, so the circle should be resetted.
     */
    private WorkflowGraph createCircleWorkflowGraphWithInnerLoopBack() {

        nodeNamesToNodes = new HashMap<>();
        Map<String, WorkflowGraphNode> nodes = new HashMap<>();
        for (int i = 0; i < 1; i++) {
            WorkflowGraphNode node = createNewNode(1, 1, true, false);
            nodeNamesToNodes.put(SINK_NODE + i, node);
            nodes.put(node.getExecutionIdentifier(), node);
        }
        WorkflowGraphNode node0 = createNewNode(2, 1, false, false);
        nodeNamesToNodes.put(NODE + 0, node0);
        nodes.put(node0.getExecutionIdentifier(), node0);
        WorkflowGraphNode node = createNewNode(1, 1, false, false);
        nodeNamesToNodes.put(NODE + 1, node);
        nodes.put(node.getExecutionIdentifier(), node);
        WorkflowGraphNode lastNode = createNewNode(1, 2, false, false);
        nodeNamesToNodes.put(NODE + 2, lastNode);
        nodes.put(lastNode.getExecutionIdentifier(), lastNode);
        Map<String, Set<WorkflowGraphEdge>> edges = new HashMap<>();
        WorkflowGraphEdge e1 =
            createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
                nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e2 = createEdge(nodeNamesToNodes.get(NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e3 = createEdge(nodeNamesToNodes.get(NODE1), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e4 = createEdge(nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(SINK_NODE0), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
        WorkflowGraphEdge e5 = createEdge(nodeNamesToNodes.get(NODE2), 0, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name(),
            nodeNamesToNodes.get(NODE0), 1, LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name());
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
     * Checks whether the given result is correct.
     * 
     * @param hops calculated result
     * @param hopsSize size of the map's keys (should be number of outputs of starting node.
     * @param outputDequeSize number of Deques per output (array field 0 = 'output0', 1 = 'output1', ..)
     * @param hopsCounts map of counts for hops for every output.
     */
    private void checkResultCorrect(Map<String, Set<Deque<WorkflowGraphHop>>> hops, int hopsSize, int[] outputDequeSize,
        Map<String, List<Integer>> hopsCounts) {
        Assert.assertEquals(hopsSize, hops.size());
        for (int i = 0; i < outputDequeSize.length; i++) {
            Assert.assertEquals(outputDequeSize[i], hops.get(OUTPUT + i).size());
        }
        for (String hopsName : hops.keySet()) {
            List<Integer> hopsCount = hopsCounts.get(hopsName);
            for (Deque<WorkflowGraphHop> hopDeque : hops.get(hopsName)) {
                if (hopsCount.contains(hopDeque.size())) {
                    hopsCount.remove((Integer) hopDeque.size());
                } else {
                    Assert.fail("Expected result for " + hopsName + " does not contain hops count " + hopDeque.size());
                }

                String startIdentifier = hopDeque.peek().getHopExecutionIdentifier();
                String currentTargetIdentifier = hopDeque.poll().getTargetExecutionIdentifier();
                while (hopDeque.size() > 1) {
                    WorkflowGraphHop hop = hopDeque.poll();
                    Assert.assertEquals(currentTargetIdentifier, hop.getHopExecutionIdentifier());
                    currentTargetIdentifier = hop.getTargetExecutionIdentifier();
                }
                WorkflowGraphHop lastHop = hopDeque.poll();
                Assert.assertEquals(currentTargetIdentifier, lastHop.getHopExecutionIdentifier());
                Assert.assertEquals(startIdentifier, lastHop.getTargetExecutionIdentifier());
            }
            if (!hopsCount.isEmpty()) {
                Assert.fail(hopsName + " did not have all result hops.");
            }
        }
    }

    private WorkflowGraphEdge createEdge(WorkflowGraphNode source, int outputNumber, String outputType, WorkflowGraphNode target,
        int inputNumber, String inputType) {
        String outputIdentifier = null;
        for (String output : source.getOutputIdentifiers()) {
            if (source.getEndpointName(output).equals(OUTPUT + outputNumber)) {
                outputIdentifier = output;
            }
            if (outputNumber == 0 && source.getEndpointName(output).equals(LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
                outputIdentifier = output;
            }
        }
        String inputIdentifier = null;
        for (String input : target.getInputIdentifiers()) {
            if (target.getEndpointName(input).equals("input" + inputNumber)) {
                inputIdentifier = input;
            }
        }
        if (outputIdentifier != null && inputIdentifier != null) {
            return new WorkflowGraphEdge(source.getExecutionIdentifier(), outputIdentifier, outputType, target.getExecutionIdentifier(),
                inputIdentifier, inputType);
        }
        return null;
    }

    private WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isDriver, boolean isOuterLoopDriver) {
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
            endpointnames.put(input, "input" + i);
            i++;
        }
        i = 0;
        for (String output : outputs) {
            if (i == 0 && isOuterLoopDriver) {
                endpointnames.put(output, LoopComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE);
            } else {
                endpointnames.put(output, OUTPUT + i);
            }
            i++;
        }
        return new WorkflowGraphNode(UUID.randomUUID().toString(), inputs, outputs, endpointnames, isDriver, false);
    }
}
