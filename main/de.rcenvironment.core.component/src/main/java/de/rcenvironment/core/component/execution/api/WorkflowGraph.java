/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils.DotFileBuilder;

/**
 * Encapsulates information about the workflow graph and processes particular graph-related requests.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * 
 * Note: The workflow graph was introduced to reset nested loops and later to realize fault-tolerant loops. From a semantic point of
 * view, it is kind of redundant to the WorkflowDescription or at least keeps semantically similar information but is not linked to
 * it at all. This is only for historical reasons and should changed/merged in the future. --seid_do
 * 
 */
public class WorkflowGraph implements Serializable {

    private static final long serialVersionUID = -2028814913500870207L;

    // execution identifier of node -> WorkflowGraphNode
    private final Map<String, WorkflowGraphNode> nodes;

    // identifier created by WorkflowGraph#createEdgeIdentifier -> set of WorkflowGraphEdge
    private final Map<String, Set<WorkflowGraphEdge>> edges;

    private final Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> determinedHopsToDriverForReset = new HashMap<>();

    private final Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> determinedHopsToDriverOnFailure = new HashMap<>();

    private final Map<String, WorkflowGraphNode> determinedDriverNodes = new HashMap<>();

    public WorkflowGraph(Map<String, WorkflowGraphNode> nodes, Map<String, Set<WorkflowGraphEdge>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * Returns {@link Deque}s of {@link WorkflowGraphHop}s that need to be traversed when node with given execution identifier need to reset
     * its loop.
     * 
     * @param startNodeExecutionId execution identifier of the node that need to reset its loop
     * @return {@link Deque}s of {@link WorkflowGraphHop}s to traverse for each output of the node that need to reset its loop
     * 
     * @throws ComponentExecutionException if searching for driver node fails (might be an user error)
     */
    public Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseWhenResetting(String startNodeExecutionId)
        throws ComponentExecutionException {
        return getHopsToTraverseToGetToLoopDriver(startNodeExecutionId, EndpointCharacter.SAME_LOOP,
            determinedHopsToDriverForReset, true);
    }

    /**
     * Returns {@link Deque}s of {@link WorkflowGraphHop}s that need to be traversed when node with given execution identifier failed within
     * a fault-tolerant loop.
     * 
     * @param startNodeEecutionId execution identifier of the node that failed within a fault-tolerant loop
     * @return {@link Deque}s of {@link WorkflowGraphHop}s to traverse for each output of the node that failed
     * 
     * @throws ComponentExecutionException if searching for driver node fails (might be an user error)
     */
    public Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseOnFailure(String startNodeEecutionId)
        throws ComponentExecutionException {

        Map<String, Set<Deque<WorkflowGraphHop>>> hopsDequesPerOutput;
        if (nodes.get(startNodeEecutionId).isDriver()) {
            hopsDequesPerOutput = getHopsToTraverseToGetToLoopDriver(startNodeEecutionId, EndpointCharacter.OUTER_LOOP,
                determinedHopsToDriverOnFailure, false);
        } else {
            hopsDequesPerOutput = getHopsToTraverseToGetToLoopDriver(startNodeEecutionId, EndpointCharacter.SAME_LOOP,
                determinedHopsToDriverOnFailure, false);
        }

        Set<String> visitedInputs = new HashSet<>();
        Iterator<Entry<String, Set<Deque<WorkflowGraphHop>>>> hopsDequesPerOutputIterator = hopsDequesPerOutput.entrySet().iterator();
        while (hopsDequesPerOutputIterator.hasNext()) {
            Entry<String, Set<Deque<WorkflowGraphHop>>> hopsDequesForOutput = hopsDequesPerOutputIterator.next();
            Iterator<Deque<WorkflowGraphHop>> hopsForOutputIterator = hopsDequesForOutput.getValue().iterator();
            while (hopsForOutputIterator.hasNext()) {
                Deque<WorkflowGraphHop> hops = hopsForOutputIterator.next();
                String targetInputName = hops.getLast().getTargetInputName();
                if (visitedInputs.contains(targetInputName)) {
                    hopsForOutputIterator.remove();
                } else {
                    visitedInputs.add(targetInputName);
                }
            }
            if (hopsDequesForOutput.getValue().isEmpty()) {
                hopsDequesPerOutputIterator.remove();
            }
        }
        return hopsDequesPerOutput;
    }

    /**
     * Returns the {@link WorkflowGraphNode} of the driver that controls the node with the given execution identifier.
     * 
     * @param executionIdentifier execution identifier of the node which driver needs to be found
     * @return {@link WorkflowGraphNode} of the driver that controls the node with the given execution identifier.
     * 
     * @throws ComponentExecutionException if searching for driver node fails (might be an user error)
     */
    // TODO review the kind of exception: when is it thrown? is it caused by an user or only a developer error?
    public WorkflowGraphNode getLoopDriver(String executionIdentifier) throws ComponentExecutionException {
        if (!determinedDriverNodes.containsKey(executionIdentifier)) {
            getHopsToTraverseOnFailure(executionIdentifier);
        }
        return determinedDriverNodes.get(executionIdentifier);
    }

    private Map<String, Set<Deque<WorkflowGraphHop>>> getHopsToTraverseToGetToLoopDriver(String startNodeExecutionId,
        EndpointCharacter startEnpointCharacter, Map<String, Map<String, Set<Deque<WorkflowGraphHop>>>> alreadyDeterminedHops,
        boolean isResetSearch) throws ComponentExecutionException {
        synchronized (alreadyDeterminedHops) {
            if (!alreadyDeterminedHops.containsKey(startNodeExecutionId)) {
                Map<String, Set<Deque<WorkflowGraphHop>>> hopsDequesPerOutput = new HashMap<String, Set<Deque<WorkflowGraphHop>>>();
                WorkflowGraphNode startNode = nodes.get(startNodeExecutionId);
                for (String startNodeOutputId : startNode.getOutputIdentifiers()) {
                    Set<Deque<WorkflowGraphHop>> hopsDeques = new HashSet<Deque<WorkflowGraphHop>>();
                    if (edges.containsKey(WorkflowGraph.createEdgeKey(startNode, startNodeOutputId))) {
                        for (WorkflowGraphEdge startEdge : edges.get(WorkflowGraph.createEdgeKey(startNode, startNodeOutputId))) {
                            if (startNode.isDriver()) {
                                if (startEnpointCharacter.equals(startEdge.getOutputCharacter())) {
                                    hopsDeques = startNewHopsSearch(startNode, startEdge, isResetSearch);
                                }
                            } else {
                                hopsDeques = startNewHopsSearch(startNode, startEdge, isResetSearch);
                            }
                        }
                        hopsDequesPerOutput.put(startNode.getEndpointName(startNodeOutputId), hopsDeques);
                    } else {
                        hopsDequesPerOutput.put(startNode.getEndpointName(startNodeOutputId), hopsDeques);
                    }
                }
                alreadyDeterminedHops.put(startNodeExecutionId, hopsDequesPerOutput);
            }
        }
        return createSnapshotOfHopsDeques(alreadyDeterminedHops.get(startNodeExecutionId));
    }

    private Map<String, Set<Deque<WorkflowGraphHop>>> createSnapshotOfHopsDeques(Map<String, Set<Deque<WorkflowGraphHop>>> hopDeques) {
        Map<String, Set<Deque<WorkflowGraphHop>>> hopsDequesSnapshot = new HashMap<>();
        for (String outputName : hopDeques.keySet()) {
            hopsDequesSnapshot.put(outputName, new HashSet<Deque<WorkflowGraphHop>>());
            for (Deque<WorkflowGraphHop> hops : hopDeques.get(outputName)) {
                hopsDequesSnapshot.get(outputName).add(new LinkedList<>(hops));
            }
        }
        return hopsDequesSnapshot;
    }

    private Set<Deque<WorkflowGraphHop>> startNewHopsSearch(WorkflowGraphNode startNode, WorkflowGraphEdge edge, boolean isResetSearch)
        throws ComponentExecutionException {

        Set<Deque<WorkflowGraphHop>> hopsDeques = new HashSet<Deque<WorkflowGraphHop>>();

        WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());
        
        Deque<WorkflowGraphHop> hopsDeque = new LinkedList<WorkflowGraphHop>();
        
        WorkflowGraphHop firstHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
            startNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
            nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());
        hopsDeque.add(firstHop);

        determineHopsRecursively(startNode, edge, nextNode, hopsDeque, hopsDeques, isResetSearch);

        return hopsDeques;
    }

    private void determineHopsRecursively(WorkflowGraphNode startNode, WorkflowGraphEdge edge, WorkflowGraphNode targetNode, 
        Deque<WorkflowGraphHop> hopsDeque, Set<Deque<WorkflowGraphHop>> hopsDeques, boolean isResetSearch)
        throws ComponentExecutionException {
        if (targetNode.getExecutionIdentifier().equals(startNode.getExecutionIdentifier())) {
            // got to start node again
            if (targetNode.isDriver() && isResetSearch) {
                hopsDeques.add(hopsDeque);
            }
            return;
        }
        if (nodeAlreadyVisitedThatWay(hopsDeque, targetNode, edge)) {
            return;
        }
        
        if (targetNode.isDriver()) {
            if (EndpointCharacter.OUTER_LOOP.equals(edge.getInputCharacter())) {
                // No end of the recursion
                continueHopSearch(startNode, targetNode, hopsDeque, hopsDeques, isResetSearch, EndpointCharacter.OUTER_LOOP);
            } else if (EndpointCharacter.SAME_LOOP.equals(edge.getInputCharacter())) {
                hopsDeques.add(hopsDeque);
                addNodeToDeterminedDriverNodes(startNode, targetNode);
            }
        } else {
            // No end of the recursion
            EndpointCharacter outputCharacterToConsider = EndpointCharacter.SAME_LOOP;
            if (hasNodeOppositeOutputCharacters(targetNode)) {
                switch (edge.getInputCharacter()) {
                case SAME_LOOP:
                    outputCharacterToConsider = EndpointCharacter.OUTER_LOOP;
                    break;
                case OUTER_LOOP:
                    outputCharacterToConsider = EndpointCharacter.SAME_LOOP;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown endpoint character: " + edge.getInputCharacter());
                }
            }
            continueHopSearch(startNode, targetNode, hopsDeque, hopsDeques, isResetSearch, outputCharacterToConsider);
        }
    }

    private boolean nodeAlreadyVisitedThatWay(Deque<WorkflowGraphHop> hopsDeque, WorkflowGraphNode nodeToVisit,
        WorkflowGraphEdge usedEdge) {

        Iterator<WorkflowGraphHop> hopsDequeIterator = hopsDeque.iterator();
        while (hopsDequeIterator.hasNext()) {
            WorkflowGraphHop hop = hopsDequeIterator.next();
            if (hop.getHopExecutionIdentifier().equals(nodeToVisit.getExecutionIdentifier())) {
                if (!hasNodeOppositeOutputCharacters(nodeToVisit)) {
                    return true;
                } else {
                    // expect at least one edge otherwise it is no valid hop
                    EndpointCharacter hopEdgesOutputCharacter =
                        edges.get(WorkflowGraph.createEdgeKey(nodes.get(hop.getHopExecutionIdentifier()), hop.getHopOutputIdentifier()))
                            .iterator().next().getOutputCharacter();
                    EndpointCharacter usedEdgesInputCharacter = usedEdge.getInputCharacter();
                    // if node was already visited, it must be visited via a different input character than last time
                    // note: only outgoing edge can be get from the hop here, so the comparison of the newly visited input character is done
                    // with output character from last visit and must be equal then (to consider it as a new visit)
                    if (!hopEdgesOutputCharacter.equals(usedEdgesInputCharacter)) {
                        return true;
                    }                    
                }
            }
        }
        return false;
    }
    
    private boolean hasNodeOppositeOutputCharacters(WorkflowGraphNode node) {
        boolean sameLoop = false;
        boolean outerLoop = false;
        for (String nodeOutputId : node.getOutputIdentifiers()) {
            if (edges.containsKey(WorkflowGraph.createEdgeKey(node, nodeOutputId))) {
                for (WorkflowGraphEdge edge : edges.get(WorkflowGraph.createEdgeKey(node, nodeOutputId))) {
                    if (edge.getOutputCharacter().equals(EndpointCharacter.SAME_LOOP)) {
                        sameLoop = true;
                    } else if (edge.getOutputCharacter().equals(EndpointCharacter.OUTER_LOOP)) {
                        outerLoop = true;
                    }
                }
            }
        }
        return sameLoop && outerLoop;
    }
    
    private void continueHopSearch(WorkflowGraphNode startNode, WorkflowGraphNode targetNode, Deque<WorkflowGraphHop> hopsDeque,
        Set<Deque<WorkflowGraphHop>> hopsDeques, boolean isResetSearch, EndpointCharacter... outputCharacters)
            throws ComponentExecutionException {
        for (String targetNodeOutputId : targetNode.getOutputIdentifiers()) {
            if (edges.containsKey(WorkflowGraph.createEdgeKey(targetNode, targetNodeOutputId))) {
                for (WorkflowGraphEdge nextEdge : edges.get(WorkflowGraph.createEdgeKey(targetNode, targetNodeOutputId))) {
                    if (Arrays.asList(outputCharacters).contains(nextEdge.getOutputCharacter())) {
                        continueHopsSearch(startNode, targetNode, nextEdge, hopsDeque, hopsDeques, isResetSearch);
                    }
                }
            }
        }
    }
    
    private void continueHopsSearch(WorkflowGraphNode startNode, WorkflowGraphNode currentNode, WorkflowGraphEdge edge,
        Deque<WorkflowGraphHop> hopsDeque, Set<Deque<WorkflowGraphHop>> hopsDeques, boolean isResetSearch)
        throws ComponentExecutionException {

        Deque<WorkflowGraphHop> newHopDeque = new LinkedList<WorkflowGraphHop>(hopsDeque);

        WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());
        
        WorkflowGraphHop nextHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
            currentNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
            nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());
        
        newHopDeque.add(nextHop);

        determineHopsRecursively(startNode, edge, nextNode, newHopDeque, hopsDeques, isResetSearch);
    }

    private void addNodeToDeterminedDriverNodes(WorkflowGraphNode startNode, WorkflowGraphNode driverNode)
        throws ComponentExecutionException {
        if (driverNode != null) {
            if (determinedDriverNodes.get(startNode.getExecutionIdentifier()) == null) {
                determinedDriverNodes.put(startNode.getExecutionIdentifier(), driverNode);
            } else if (!determinedDriverNodes.get(startNode.getExecutionIdentifier()).getExecutionIdentifier()
                .equals(driverNode.getExecutionIdentifier())) {
                throw new ComponentExecutionException(
                    "Error in workflow graph search: newly determined driver node differs from driver node determined earlier");
            }
        }
    }
    
    /**
     * Generates dot language string describing the workflow graph. Can be used to visualize the graph with Graphviz.
     * (Command: dot -Tpng <wf.dot> -o <wf.png>)
     * 
     * @return dot language string representation
     */
    public String toDotScript() {
        
        final String propNameColor = "color";
        
        DotFileBuilder builder = GraphvizUtils.createDotFileBuilder("wf_graph");
        // TODO properties should be set more efficiently (e.g., by defining shape, font size etc. as global properties of the digraph)
        for (WorkflowGraphNode node : nodes.values()) {
            builder.addVertex(node.getExecutionIdentifier(), node.getName());            
            if (node.isDriver()) {
                builder.addVertexProperty(node.getExecutionIdentifier(), propNameColor, "#AA3939");
            } else if (hasNodeOppositeOutputCharacters(node)) {
                builder.addVertexProperty(node.getExecutionIdentifier(), propNameColor, "#D4AA6A");
            }
            builder.addVertexProperty(node.getExecutionIdentifier(), "shape", "rectangle");
            builder.addVertexProperty(node.getExecutionIdentifier(), "fontsize", "10");
            builder.addVertexProperty(node.getExecutionIdentifier(), "fontname", "Consolas");
        }
        
        for (Set<WorkflowGraphEdge> edgesSet : edges.values()) {
            for (WorkflowGraphEdge edge : edgesSet) {
                Map<String, String> edgeProps = new HashMap<>();
                edgeProps.put("fontsize", "10");
                edgeProps.put("fontname", "Consolas");
                
                if (edge.getInputCharacter().equals(EndpointCharacter.OUTER_LOOP)) {
                    edgeProps.put(propNameColor, "#55AA55");
                } else if (edge.getOutputCharacter().equals(EndpointCharacter.OUTER_LOOP)) {
                    edgeProps.put(propNameColor, "#4B698B");
                }
                String label = StringUtils.format("%s > %s", nodes.get(edge.getSourceExecutionIdentifier())
                    .getEndpointName(edge.getOutputIdentifier()), nodes.get(edge.getTargetExecutionIdentifier())
                    .getEndpointName(edge.getInputIdentifier()));
                builder.addEdge(edge.getSourceExecutionIdentifier(), edge.getTargetExecutionIdentifier(), label, edgeProps);
            }
            
        }
        return builder.getScriptContent();
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
