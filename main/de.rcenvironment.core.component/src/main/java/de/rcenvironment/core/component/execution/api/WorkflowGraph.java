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

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Encapsulates information about the workflow graph and processes particular graph-related
 * requests.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 */
public class WorkflowGraph implements Serializable {

    private static final long serialVersionUID = -2028814913500870207L;

    // execution identifier of starting node -> WorkflowGraphNode
    private final Map<String, WorkflowGraphNode> nodes;

    // identifier created by WorkflowGraph#createEdgeIdentifier -> set of WorkflowGraphEdge
    private final Map<String, Set<WorkflowGraphEdge>> edges;

    private final Map<String, Map<String, Set<Queue<WorkflowGraphHop>>>> cachedHopsToDriverForReset = new HashMap<>();

    private final Map<String, Map<String, Set<Queue<WorkflowGraphHop>>>> cachedHopsToDriverOnFailure = new HashMap<>();

    private final Map<String, WorkflowGraphNode> cachedDriverNodes = new HashMap<>();

    public WorkflowGraph(Map<String, WorkflowGraphNode> nodes, Map<String, Set<WorkflowGraphEdge>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * @param executionIdentifier execution identifier of the starting node
     * @return ordered list of hops to traverse for each output of the starting node
     * @throws ComponentExecutionException if searching for driver node failes (might be an user
     *         error)
     */
    public Map<String, Set<Queue<WorkflowGraphHop>>> getHopsToTraverseWhenResetting(String executionIdentifier)
        throws ComponentExecutionException {
        return getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.SelfLoopEndpoint.name(),
            cachedHopsToDriverForReset, true);
    }

    /**
     * @param executionIdentifier execution identifier of the node, which failed within a
     *        failure-tolerant loop
     * @return ordered list of hops to traverse for each output of the node failed
     * @throws ComponentExecutionException if searching for driver node failes (might be an user
     *         error)
     */
    public Map<String, Set<Queue<WorkflowGraphHop>>> getHopsToTraverseOnFailure(String executionIdentifier)
        throws ComponentExecutionException {
        if (nodes.get(executionIdentifier).isDriver()) {
            return getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.OuterLoopEndpoint.name(),
                cachedHopsToDriverOnFailure, false);
        } else {
            return getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.SelfLoopEndpoint.name(),
                cachedHopsToDriverOnFailure, false);
        }
    }

    /**
     * @param executionIdentifier execution identifier of the affected node, which is part of a loop
     * @return {@link WorkflowGraphNode} representing the node, which drives the loop, the given
     *         affected node is part of
     * @throws ComponentExecutionException if searching for driver node failes (might be an user
     *         error)
     */
    public WorkflowGraphNode getLoopDriver(String executionIdentifier) throws ComponentExecutionException {
        if (!cachedDriverNodes.containsKey(executionIdentifier)) {
            getHopsToTraverseOnFailure(executionIdentifier);
        }
        return cachedDriverNodes.get(executionIdentifier);

    }

    private Map<String, Set<Queue<WorkflowGraphHop>>> getHopsToTraverseToGetToLoopDriver(String executionIdentifier,
        String startingEnpointType, Map<String, Map<String, Set<Queue<WorkflowGraphHop>>>> hopsCache, boolean isResetSearch)
        throws ComponentExecutionException {
        synchronized (hopsCache) {
            if (!hopsCache.containsKey(executionIdentifier)) {
                Map<String, Set<Queue<WorkflowGraphHop>>> failureCycleHops = new HashMap<String, Set<Queue<WorkflowGraphHop>>>();
                WorkflowGraphNode startNode = nodes.get(executionIdentifier);
                for (String outputIdentifier : startNode.getOutputIdentifiers()) {
                    Set<Queue<WorkflowGraphHop>> endpointFailureHopsSet = new HashSet<Queue<WorkflowGraphHop>>();
                    if (edges.containsKey(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                        for (WorkflowGraphEdge e : edges.get(StringUtils.escapeAndConcat(executionIdentifier, outputIdentifier))) {
                            if (startNode.isDriver()) {
                                if (startingEnpointType.equals(e.getOutputEndpointType())) {
                                    startNewHopsSearch(executionIdentifier, startNode, endpointFailureHopsSet, e, isResetSearch);
                                }
                            } else {
                                startNewHopsSearch(executionIdentifier, startNode, endpointFailureHopsSet, e, isResetSearch);
                            }
                        }
                        failureCycleHops.put(startNode.getEndpointName(outputIdentifier), endpointFailureHopsSet);
                    } else {
                        failureCycleHops.put(startNode.getEndpointName(outputIdentifier), new HashSet<Queue<WorkflowGraphHop>>());
                    }
                }
                hopsCache.put(executionIdentifier, failureCycleHops);
            }
        }
        return createSnapshotFromGivenMap(executionIdentifier, hopsCache);
    }

    private Map<String, Set<Queue<WorkflowGraphHop>>> createSnapshotFromGivenMap(String executionIdentifier,
        Map<String, Map<String, Set<Queue<WorkflowGraphHop>>>> map) {
        Map<String, Set<Queue<WorkflowGraphHop>>> cachedResetCycleHopForExeId = map.get(executionIdentifier);
        Map<String, Set<Queue<WorkflowGraphHop>>> snapshot = new HashMap<>();
        for (String outputName : cachedResetCycleHopForExeId.keySet()) {
            snapshot.put(outputName, new HashSet<Queue<WorkflowGraphHop>>());
            for (Queue<WorkflowGraphHop> hops : cachedResetCycleHopForExeId.get(outputName)) {
                snapshot.get(outputName).add(new LinkedList<>(hops));
            }
        }
        return snapshot;
    }

    private void startNewHopsSearch(String executionIdentifier, WorkflowGraphNode startNode,
        Set<Queue<WorkflowGraphHop>> endpointFailureHopsSet, WorkflowGraphEdge e, boolean isResetSearch)
        throws ComponentExecutionException {
        Queue<WorkflowGraphHop> hopsQueue = new LinkedList<WorkflowGraphHop>();
        hopsQueue
            .add(new WorkflowGraphHop(executionIdentifier, startNode.getEndpointName(e.getOutputIdentifier()),
                e.getTargetExecutionIdentifier(), nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(e.getInputIdentifier())));
        getHopsRecursive(e.getTargetExecutionIdentifier(), hopsQueue, endpointFailureHopsSet, e,
            startNode, isResetSearch);
    }

    private void getHopsRecursive(String targetExecutionIdentifier, Queue<WorkflowGraphHop> hopsQueue,
        Set<Queue<WorkflowGraphHop>> endpointHopsSet, WorkflowGraphEdge edge, WorkflowGraphNode startNode, boolean isResetSearch)
        throws ComponentExecutionException {
        WorkflowGraphNode currentNode = nodes.get(targetExecutionIdentifier);
        if (targetExecutionIdentifier.equals(startNode.getExecutionIdentifier())) {
            // returned to origin OR current node has no outputs to go to.
            if (currentNode.isDriver()) {
                endpointHopsSet.add(hopsQueue);
                if (!isResetSearch) {
                    throw new ComponentExecutionException(
                        "Found loop driver that is the same as the start node whilst not searching reset loop.");
                }
            }
            return;
        }
        for (WorkflowGraphHop hop : hopsQueue) {
            if (hop.getHopExecutionIdentifier().equals(targetExecutionIdentifier)) {
                // reached already visited node.
                return;
            }
        }
        if (currentNode.isDriver()) {
            if (LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name().equals(edge.getInputEndpointType())) {
                // No end of the recursion
                for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
                    if (edges.containsKey(StringUtils.escapeAndConcat(currentNode.getExecutionIdentifier(), outputIdentifier))) {
                        for (WorkflowGraphEdge e : edges.get(WorkflowGraph.createEdgeKey(currentNode, outputIdentifier))) {
                            if (LoopComponentConstants.LoopEndpointType.OuterLoopEndpoint.name().equals(e.getOutputEndpointType())) {
                                recursiveHop(hopsQueue, endpointHopsSet, startNode, currentNode, outputIdentifier, e, isResetSearch);
                            }
                        }
                    }
                }
            } else if (LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name().equals(edge.getInputEndpointType())) {
                endpointHopsSet.add(hopsQueue);
                addNodeToDriverCache(startNode, currentNode);
            }
        } else {
            // No end of the recursion
            for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
                if (edges.containsKey(StringUtils.escapeAndConcat(currentNode.getExecutionIdentifier(), outputIdentifier))) {
                    for (WorkflowGraphEdge e : edges.get(WorkflowGraph.createEdgeKey(currentNode, outputIdentifier))) {
                        recursiveHop(hopsQueue, endpointHopsSet, startNode, currentNode, outputIdentifier, e, isResetSearch);
                    }
                }
            }
        }
    }

    private void recursiveHop(Queue<WorkflowGraphHop> hopsQueue, Set<Queue<WorkflowGraphHop>> endpointHopsSet,
        WorkflowGraphNode startNode,
        WorkflowGraphNode currentNode, String outputIdentifier, WorkflowGraphEdge e, boolean isResetSearch)
        throws ComponentExecutionException {
        Queue<WorkflowGraphHop> newQueue = new LinkedList<WorkflowGraphHop>(hopsQueue);
        newQueue.add(new WorkflowGraphHop(currentNode.getExecutionIdentifier(),
            currentNode.getEndpointName(outputIdentifier), e.getTargetExecutionIdentifier(),
            nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(e.getInputIdentifier())));
        getHopsRecursive(e.getTargetExecutionIdentifier(), newQueue, endpointHopsSet, e, startNode, isResetSearch);
    }

    private void addNodeToDriverCache(WorkflowGraphNode startNode, WorkflowGraphNode driverNode) throws ComponentExecutionException {
        if (driverNode != null) {
            if (cachedDriverNodes.get(startNode.getExecutionIdentifier()) == null) {
                cachedDriverNodes.put(startNode.getExecutionIdentifier(), driverNode);
            } else if (!cachedDriverNodes.get(startNode.getExecutionIdentifier()).getExecutionIdentifier()
                .equals(driverNode.getExecutionIdentifier())) {
                throw new ComponentExecutionException(
                    "Error in searching workflow graph: current driver node differs from already set node.");
            }
        }
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
