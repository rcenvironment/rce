/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Encapsulates information about the workflow graph and processes particular graph-related
 * requests.
 * 
 * @author Doreen Seider
 */
public class WorkflowGraph implements Serializable {

    private static final long serialVersionUID = -2028814913500870207L;

    // executionIdentifier -> WorkflowGraphNode
    private final Map<String, WorkflowGraphNode> nodes;

    // identifier created by WorkflowGraph#createEdgeIdentifier -> WorkflowGraphEdge
    private final Map<String, Set<WorkflowGraphEdge>> edges;

    private Map<String, Map<String, Set<Queue<WorkflowGraphHop>>>> cachedResetCycleHops = new HashMap<>();

    private Map<String, Set<WorkflowGraphNode>> innerResetLinkNodes = new HashMap<>();

    public WorkflowGraph(Map<String, WorkflowGraphNode> nodes, Map<String, Set<WorkflowGraphEdge>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * @param executionIdentifier execution identifier of node
     * @return ordered list of hops to traverse
     */
    public Map<String, Set<Queue<WorkflowGraphHop>>> getHopsToTraverseWhenResetting(String executionIdentifier) {
        if (!cachedResetCycleHops.containsKey(executionIdentifier)) {
            Map<String, Set<Queue<WorkflowGraphHop>>> resetCycleHops = new HashMap<String, Set<Queue<WorkflowGraphHop>>>();
            WorkflowGraphNode startNode = nodes.get(executionIdentifier);
            for (String outputIdentifier : startNode.getOutputIdentifiers()) {
                Set<Queue<WorkflowGraphHop>> endpointHopsSet = new HashSet<Queue<WorkflowGraphHop>>();
                if (edges.containsKey(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                    for (WorkflowGraphEdge e : edges.get(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                        Queue<WorkflowGraphHop> hopsQueue = new LinkedList<WorkflowGraphHop>();
                        hopsQueue.add(new WorkflowGraphHop(executionIdentifier, startNode.getEndpointName(e.getOutputIdentifier()),
                            e.getTargetExecutionIdentifier(),
                            nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(e.getInputIdentifier())));
                        getHopsRecursive(e.getTargetExecutionIdentifier(), hopsQueue, endpointHopsSet, startNode);
                    }
                    resetCycleHops.put(startNode.getEndpointName(outputIdentifier), endpointHopsSet);
                }
            }
            cachedResetCycleHops.put(executionIdentifier, resetCycleHops);
        }
        Map<String, Set<Queue<WorkflowGraphHop>>> cachedResetCycleHopForExeId = cachedResetCycleHops.get(executionIdentifier);
        Map<String, Set<Queue<WorkflowGraphHop>>> snapshot = new HashMap<>();
        for (String outputName : cachedResetCycleHopForExeId.keySet()) {
            snapshot.put(outputName, new HashSet<Queue<WorkflowGraphHop>>());
            for (Queue<WorkflowGraphHop> hops : cachedResetCycleHopForExeId.get(outputName)) {
                snapshot.get(outputName).add(new LinkedList<>(hops));
            }
        }
        return snapshot;
    }

    private void getHopsRecursive(String targetExecutionIdentifier, Queue<WorkflowGraphHop> hopsQueue,
        Set<Queue<WorkflowGraphHop>> endpointHopsSet, WorkflowGraphNode startNode) {
        WorkflowGraphNode currentNode = nodes.get(targetExecutionIdentifier);
        if (targetExecutionIdentifier.equals(startNode.getExecutionIdentifier())) {
            // returned to origin OR current node has no outputs to go to.
            endpointHopsSet.add(hopsQueue);
            return;
        }

        if ((currentNode.isResetSink() && !getInnerResetLinkNodes(startNode.getExecutionIdentifier()).contains(
            currentNode.getExecutionIdentifier()))
            || currentNode.getOutputIdentifiers().isEmpty()) {
            // We reached another sink that is in the outer loop or starts an inner loop -> no
            // further reset needed.
            return;
        }
        if (currentNode.isResetSink()) {
            for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
                if (edges.containsKey(StringUtils.escapeAndConcat(currentNode.getExecutionIdentifier(), outputIdentifier))) {
                    for (WorkflowGraphEdge e : edges.get(WorkflowGraph.createEdgeKey(currentNode, outputIdentifier))) {
                        Queue<WorkflowGraphHop> newQueue = new LinkedList<WorkflowGraphHop>(hopsQueue);
                        newQueue.add(new WorkflowGraphHop(currentNode.getExecutionIdentifier(), currentNode
                            .getEndpointName(outputIdentifier),
                            e.getTargetExecutionIdentifier(), nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(
                                e.getInputIdentifier())));
                        getHopsRecursive(e.getTargetExecutionIdentifier(), newQueue, endpointHopsSet, startNode);
                    }
                }
            }
            return;
        }
        // No end of the recursion
        for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
            if (edges.containsKey(StringUtils.escapeAndConcat(currentNode.getExecutionIdentifier(), outputIdentifier))) {
                for (WorkflowGraphEdge e : edges.get(WorkflowGraph.createEdgeKey(currentNode, outputIdentifier))) {
                    Queue<WorkflowGraphHop> newQueue = new LinkedList<WorkflowGraphHop>(hopsQueue);
                    newQueue.add(new WorkflowGraphHop(currentNode.getExecutionIdentifier(), currentNode.getEndpointName(outputIdentifier),
                        e.getTargetExecutionIdentifier(), nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(
                            e.getInputIdentifier())));
                    getHopsRecursive(e.getTargetExecutionIdentifier(), newQueue, endpointHopsSet, startNode);
                }
            }
        }
    }

    private Set<WorkflowGraphNode> getInnerResetLinkNodes(String executionIdentifier) {
        if (!innerResetLinkNodes.containsKey(executionIdentifier)) {
            Set<WorkflowGraphNode> resetLinkNodes = new HashSet<WorkflowGraphNode>();
            WorkflowGraphNode currentNode = nodes.get(executionIdentifier);
            if (currentNode.getOutputIdentifiers() != null) {
                for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
                    if (outputIdentifier.equals(ComponentConstants.ENDPOINT_NAME_OUTERLOOP_DONE)
                        && edges.containsKey(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                        for (WorkflowGraphEdge e : edges.get(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                            if (nodes.get(e.getTargetExecutionIdentifier()).isResetSink()) {
                                resetLinkNodes.add(nodes.get(e.getTargetExecutionIdentifier()));
                            }
                        }
                    }
                }
            }
            innerResetLinkNodes.put(executionIdentifier, resetLinkNodes);
        }
        return innerResetLinkNodes.get(executionIdentifier);
    }

    /**
     * Creates a key out of the {@link WorkflowGraphEdge}, which can be used as key of a map.
     * 
     * @param edge {@link WorkflowGraphEdge} to get the identifier for
     * @return key for the {@link WorkflowGraphEdge}
     */
    public static String createEdgeKey(WorkflowGraphEdge edge) {
        return StringUtils.escapeAndConcat(edge.getSourceExecutionIdentifier(), edge.getOutputIdentifier());
    }

    /**
     * Creates a key out of the {@link WorkflowGraphNode}, which can be used as key of a map.
     * 
     * @param node source {@link WorkflowGraphNode} of the edge to get the identifier for
     * @param outputIdentifier source endpoint of the edge to get the identifier for
     * @return key for the {@link WorkflowGraphEdge}
     */
    public static String createEdgeKey(WorkflowGraphNode node, String outputIdentifier) {
        return StringUtils.escapeAndConcat(node.getExecutionIdentifier(), outputIdentifier);
    }
}
