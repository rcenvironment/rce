/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentConstants;

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
    public void testWorkflowGraph() {
        WorkflowGraph graph = null;
        Map<String, Set<Queue<WorkflowGraphHop>>> hops = null;
        Map<String, List<Integer>> hopsCount = new HashMap<>();
        List<Integer> counts = new LinkedList<>();

        graph = createCircleWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        hopsCount = new HashMap<>();
        counts = new LinkedList<>();
        counts.add(4);
        hopsCount.put(OUTPUT + ZERO, counts);
        checkResultCorrect(hops, 1, new int[] { 1 }, hopsCount);

        graph = createTwoSinksWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        hopsCount = new HashMap<>();
        counts = new LinkedList<>();
        hopsCount.put(OUTPUT + ZERO, counts);
        checkResultCorrect(hops, 1, new int[] { 0 }, hopsCount);

        graph = createTwoSinksWithInnerLoopWorkflowGraph();
        hops = graph.getHopsToTraverseWhenResetting(nodeNamesToNodes.get(SINK_NODE0).getExecutionIdentifier());
        hopsCount = new HashMap<>();
        counts = new LinkedList<>();
        counts.add(2);
        hopsCount.put(OUTPUT + ZERO, counts);
        hopsCount.put(OUTPUT + "1", new LinkedList<Integer>());
        checkResultCorrect(hops, 2, new int[] { 1, 0 }, hopsCount);

    }

    /**
     * 
     * @return A graph were two sinks are in a row with no nested loop, so no resetting queue should
     *         be produced.
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
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, nodeNamesToNodes.get(SINK_NODE0), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, nodeNamesToNodes.get(SINK_NODE1), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, nodeNamesToNodes.get(NODE0), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, nodeNamesToNodes.get(OUTER_LOOP_NODE), 0));

        for (WorkflowGraphEdge e : edgesSet) {
            String key = WorkflowGraph.createEdgeKey(e);
            edges.put(key, new HashSet<WorkflowGraphEdge>());
            edges.get(key).add(e);
        }
        return new WorkflowGraph(nodes, edges);

    }

    /**
     * 
     * @return A graph where two sinks are in a row and both have a nested loop. The reset should .
     *         contain 2 hops.
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
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, nodeNamesToNodes.get(NODE0), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE0), 0, nodeNamesToNodes.get(SINK_NODE0), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, nodeNamesToNodes.get(NODE1), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE1), 0, nodeNamesToNodes.get(SINK_NODE0), 1));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE0), 1, nodeNamesToNodes.get(SINK_NODE1), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 0, nodeNamesToNodes.get(NODE2), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE2), 0, nodeNamesToNodes.get(SINK_NODE1), 1));
        edgesSet.add(createEdge(nodeNamesToNodes.get(SINK_NODE1), 1, nodeNamesToNodes.get(NODE3), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(NODE3), 0, nodeNamesToNodes.get(OUTER_LOOP_NODE), 0));
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, nodeNamesToNodes.get(SINK_NODE0), 2));
        edgesSet.add(createEdge(nodeNamesToNodes.get(OUTER_LOOP_NODE), 0, nodeNamesToNodes.get(SINK_NODE1), 2));

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
        WorkflowGraphEdge e1 = createEdge(nodeNamesToNodes.get(SINK_NODE0), 0, nodeNamesToNodes.get(NODE0), 0);
        WorkflowGraphEdge e2 = createEdge(nodeNamesToNodes.get(NODE0), 0, nodeNamesToNodes.get(NODE1), 0);
        WorkflowGraphEdge e3 = createEdge(nodeNamesToNodes.get(NODE1), 0, nodeNamesToNodes.get(NODE2), 0);
        WorkflowGraphEdge e4 = createEdge(nodeNamesToNodes.get(NODE2), 0, nodeNamesToNodes.get(SINK_NODE0), 0);

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
     * Checks whether the given result is correct.
     * 
     * @param hops calculated result
     * @param hopsSize size of the map's keys (should be number of outputs of starting node.
     * @param outputQueueSize number of queues per output (array field 0 = 'output0', 1 = 'output1',
     *        ..)
     * @param hopsCounts map of counts for hops for every output.
     */
    private void checkResultCorrect(Map<String, Set<Queue<WorkflowGraphHop>>> hops, int hopsSize, int[] outputQueueSize,
        Map<String, List<Integer>> hopsCounts) {
        Assert.assertEquals(hopsSize, hops.size());
        for (int i = 0; i < outputQueueSize.length; i++) {
            Assert.assertEquals(outputQueueSize[i], hops.get(OUTPUT + i).size());
        }
        for (String hopsName : hops.keySet()) {
            List<Integer> hopsCount = hopsCounts.get(hopsName);
            for (Queue<WorkflowGraphHop> hopQueue : hops.get(hopsName)) {
                if (hopsCount.contains(hopQueue.size())) {
                    hopsCount.remove((Integer) hopQueue.size());
                } else {
                    Assert.fail("Expected result for " + hopsName + " does not contain hops count " + hopQueue.size());
                }

                String startIdentifier = hopQueue.peek().getHopExecutionIdentifier();
                String currentTargetIdentifier = hopQueue.poll().getTargetExecutionIdentifier();
                while (hopQueue.size() > 1) {
                    WorkflowGraphHop hop = hopQueue.poll();
                    Assert.assertEquals(currentTargetIdentifier, hop.getHopExecutionIdentifier());
                    currentTargetIdentifier = hop.getTargetExecutionIdentifier();
                }
                WorkflowGraphHop lastHop = hopQueue.poll();
                Assert.assertEquals(currentTargetIdentifier, lastHop.getHopExecutionIdentifier());
                Assert.assertEquals(startIdentifier, lastHop.getTargetExecutionIdentifier());
            }
            if (!hopsCount.isEmpty()) {
                Assert.fail(hopsName + " did not have all result hops.");
            }
        }
    }

    private WorkflowGraphEdge createEdge(WorkflowGraphNode source, int outputNumber, WorkflowGraphNode target, int inputNumber) {
        String outputIdentifier = null;
        for (String output : source.getOutputIdentifiers()) {
            if (source.getEndpointName(output).equals(OUTPUT + outputNumber)) {
                outputIdentifier = output;
            }
            if (outputNumber == 0 && source.getEndpointName(output).equals(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)) {
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
            return new WorkflowGraphEdge(source.getExecutionIdentifier(), outputIdentifier, target.getExecutionIdentifier(),
                inputIdentifier);
        }
        return null;
    }

    private WorkflowGraphNode createNewNode(int inputCount, int outputCount, boolean isResetSink, boolean isOuterLoopDriver) {
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
                endpointnames.put(output, ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE);
            } else {
                endpointnames.put(output, OUTPUT + i);
            }
            i++;
        }
        return new WorkflowGraphNode(UUID.randomUUID().toString(), inputs, outputs, endpointnames, isResetSink);
    }
}
