/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopEndpointType;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Encapsulates information about the workflow graph and processes particular graph-related requests.
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

    private final Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> cachedHopsToDriverForReset = new HashMap<>();

    private final Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> cachedHopsToDriverOnFailure = new HashMap<>();

    private final Map<String, WorkflowGraphNode> cachedDriverNodes = new HashMap<>();

    public WorkflowGraph(Map<String, WorkflowGraphNode> nodes, Map<String, Set<WorkflowGraphEdge>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * @param executionIdentifier execution identifier of the starting node
     * @return ordered list of hops to traverse for each output of the starting node
     * @throws ComponentExecutionException if searching for driver node failes (might be an user error)
     */
    public Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseWhenResetting(String executionIdentifier)
        throws ComponentExecutionException {
        return getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.SelfLoopEndpoint.name(),
            cachedHopsToDriverForReset, true);
    }

    /**
     * @param executionIdentifier execution identifier of the node, which failed within a failure-tolerant loop
     * @return ordered list of hops to traverse for each output of the node failed
     * @throws ComponentExecutionException if searching for driver node fails (might be an user error)
     */
    public Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseOnFailure(String executionIdentifier)
        throws ComponentExecutionException {
        Map<String, Set<Deque<WorkflowGraphHop>>> searchResult = null;
        if (nodes.get(executionIdentifier).isDriver()) {
            searchResult = getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.OuterLoopEndpoint.name(),
                cachedHopsToDriverOnFailure, false);
        } else {
            searchResult = getHopsToTraverseToGetToLoopDriver(executionIdentifier, LoopEndpointType.SelfLoopEndpoint.name(),
                cachedHopsToDriverOnFailure, false);
        }
        Set<String> visitedInputs = new HashSet<>();
        Iterator<Entry<String, Set<Deque<WorkflowGraphHop>>>> outputIterator = searchResult.entrySet().iterator();
        while (outputIterator.hasNext()) {
            Entry<String, Set<Deque<WorkflowGraphHop>>> currentOutput = outputIterator.next();
            Iterator<Deque<WorkflowGraphHop>> hopsIterator = currentOutput.getValue().iterator();
            while (hopsIterator.hasNext()) {
                Deque<WorkflowGraphHop> hops = hopsIterator.next();
                String targetInputName = hops.getLast().getTargetInputName();
                if (visitedInputs.contains(targetInputName)) {
                    hopsIterator.remove();
                } else {
                    visitedInputs.add(targetInputName);
                }
            }
            if (currentOutput.getValue().isEmpty()) {
                outputIterator.remove();
            }
        }
        return searchResult;
    }

    /**
     * @param executionIdentifier execution identifier of the affected node, which is part of a loop
     * @return {@link WorkflowGraphNode} representing the node, which drives the loop, the given affected node is part of
     * @throws ComponentExecutionException if searching for driver node failes (might be an user error)
     */
    public WorkflowGraphNode getLoopDriver(String executionIdentifier) throws ComponentExecutionException {
        if (!cachedDriverNodes.containsKey(executionIdentifier)) {
            getHopsToTraverseOnFailure(executionIdentifier);
        }
        return cachedDriverNodes.get(executionIdentifier);

    }

    private Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseToGetToLoopDriver(String executionIdentifier,
        String startingEnpointType, Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> hopsCache, boolean isResetSearch)
        throws ComponentExecutionException {
        synchronized (hopsCache) {
            if (!hopsCache.containsKey(executionIdentifier)) {
                Map<String, Set<Deque<WorkflowGraphHop>>> failureCycleHops = new HashMap<String, Set<Deque<WorkflowGraphHop>>>();
                WorkflowGraphNode startNode = nodes.get(executionIdentifier);
                for (String outputIdentifier : startNode.getOutputIdentifiers()) {
                    Set<Deque<WorkflowGraphHop>> endpointFailureHopsSet = new HashSet<Deque<WorkflowGraphHop>>();
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
                        failureCycleHops.put(startNode.getEndpointName(outputIdentifier), new HashSet<Deque<WorkflowGraphHop>>());
                    }
                }
                hopsCache.put(executionIdentifier, failureCycleHops);
            }
        }
        return createSnapshotFromGivenMap(executionIdentifier, hopsCache);
    }

    private Map<String, Set<Deque<WorkflowGraphHop>>> createSnapshotFromGivenMap(String executionIdentifier,
        Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> map) {
        Map<String, Set<Deque<WorkflowGraphHop>>> cachedResetCycleHopForExeId = map.get(executionIdentifier);
        Map<String, Set<Deque<WorkflowGraphHop>>> snapshot = new HashMap<>();
        for (String outputName : cachedResetCycleHopForExeId.keySet()) {
            snapshot.put(outputName, new HashSet<Deque<WorkflowGraphHop>>());
            for (Deque<WorkflowGraphHop> hops : cachedResetCycleHopForExeId.get(outputName)) {
                snapshot.get(outputName).add(new LinkedList<>(hops));
            }
        }
        return snapshot;
    }

    private void startNewHopsSearch(String executionIdentifier, WorkflowGraphNode startNode,
        Set<Deque<WorkflowGraphHop>> endpointFailureHopsSet, WorkflowGraphEdge e, boolean isResetSearch)
        throws ComponentExecutionException {
        Deque<WorkflowGraphHop> hopsDeque = new LinkedList<WorkflowGraphHop>();
        hopsDeque
            .add(new WorkflowGraphHop(executionIdentifier, startNode.getEndpointName(e.getOutputIdentifier()),
                e.getTargetExecutionIdentifier(), nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(e.getInputIdentifier())));
        getHopsRecursive(e.getTargetExecutionIdentifier(), hopsDeque, endpointFailureHopsSet, e,
            startNode, isResetSearch);
    }

    private void getHopsRecursive(String targetExecutionIdentifier, Deque<WorkflowGraphHop> hopsDeque,
        Set<Deque<WorkflowGraphHop>> endpointHopsSet, WorkflowGraphEdge edge, WorkflowGraphNode startNode, boolean isResetSearch)
        throws ComponentExecutionException {
        WorkflowGraphNode currentNode = nodes.get(targetExecutionIdentifier);
        if (targetExecutionIdentifier.equals(startNode.getExecutionIdentifier())) {
            // returned to origin OR current node has no outputs to go to.
            if (currentNode.isDriver() && isResetSearch) {
                endpointHopsSet.add(hopsDeque);
            }
            return;
        }
        for (WorkflowGraphHop hop : hopsDeque) {
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
                                recursiveHop(hopsDeque, endpointHopsSet, startNode, currentNode, outputIdentifier, e, isResetSearch);
                            }
                        }
                    }
                }
            } else if (LoopComponentConstants.LoopEndpointType.SelfLoopEndpoint.name().equals(edge.getInputEndpointType())) {
                endpointHopsSet.add(hopsDeque);
                addNodeToDriverCache(startNode, currentNode);
            }
        } else {
            // No end of the recursion
            for (String outputIdentifier : currentNode.getOutputIdentifiers()) {
                if (edges.containsKey(StringUtils.escapeAndConcat(currentNode.getExecutionIdentifier(), outputIdentifier))) {
                    for (WorkflowGraphEdge e : edges.get(WorkflowGraph.createEdgeKey(currentNode, outputIdentifier))) {
                        recursiveHop(hopsDeque, endpointHopsSet, startNode, currentNode, outputIdentifier, e, isResetSearch);
                    }
                }
            }
        }
    }

    private void recursiveHop(Deque<WorkflowGraphHop> hopsDeque, Set<Deque<WorkflowGraphHop>> endpointHopsSet,
        WorkflowGraphNode startNode,
        WorkflowGraphNode currentNode, String outputIdentifier, WorkflowGraphEdge e, boolean isResetSearch)
        throws ComponentExecutionException {
        Deque<WorkflowGraphHop> newDeque = new LinkedList<WorkflowGraphHop>(hopsDeque);
        newDeque.add(new WorkflowGraphHop(currentNode.getExecutionIdentifier(),
            currentNode.getEndpointName(outputIdentifier), e.getTargetExecutionIdentifier(),
            nodes.get(e.getTargetExecutionIdentifier()).getEndpointName(e.getInputIdentifier())));
        getHopsRecursive(e.getTargetExecutionIdentifier(), newDeque, endpointHopsSet, e, startNode, isResetSearch);
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
