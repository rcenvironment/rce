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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils.DotFileBuilder;

/**
 * Encapsulates information about the workflow graph and processes particular graph-related requests.
 * 
 * @author Doreen Seider
 * @author Sascha Zur
 * @author Tobias Brieden
 * 
 *         Note: The workflow graph was introduced to reset nested loops and later to realize fault-tolerant loops. From a semantic point of
 *         view, it is kind of redundant to the WorkflowDescription or at least keeps semantically similar information but is not linked to
 *         it at all. This is only for historical reasons and should changed/merged in the future. --seid_do
 * 
 *         Right now this class contains two different kinds of recursive calculation. One for the calculation of the hops that need to be
 *         visited for a reset and another one for handling loop failures. The resetting failed in some cases, as described in [0], and
 *         needed to be fixed. Nevertheless, we need to check if the failure calculations are also problematic and they most likely should
 *         be migrated to the calculations which are user for resetting. --rode_to
 * 
 *         [0] https://mantis.sc.dlr.de/view.php?id=15854
 */
public class WorkflowGraph implements Serializable {

    private static final String DUMMY = "dummy";

    private static final long serialVersionUID = -2028814913500870207L;

    // execution identifier of node -> WorkflowGraphNode
    private final Map<String, WorkflowGraphNode> nodes;

    // identifier created by WorkflowGraph#createEdgeIdentifier -> set of WorkflowGraphEdge
    private final Map<String, Set<WorkflowGraphEdge>> edges;

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
     * TODO This calculation should only be performed once for each node and then be cached for later retrieval.
     * 
     * @param startNodeExecutionId execution identifier of the node that need to reset its loop
     * @return {@link Deque}s of {@link WorkflowGraphHop}s to traverse for each output of the node that need to reset its loop
     */
    public synchronized Set<Deque<WorkflowGraphHop>> getHopsToTraverseWhenResetting(String startNodeExecutionId) {
        // A set of node that stores all visited nodes during the recursive calculation of the hops to traverse for resetting a nested loop.
        Set<WorkflowGraphNode> alreadyVisitedNodes = new HashSet<>();

        List<List<WorkflowGraphEdge>> recursionResult = new LinkedList<>();

        recursion(alreadyVisitedNodes, nodes.get(startNodeExecutionId), EndpointCharacter.SAME_LOOP, new LinkedList<WorkflowGraphEdge>(),
            recursionResult);

        // create a copy of the data structure
        Set<Deque<WorkflowGraphHop>> hopsDequesSnapshot = new HashSet<>();
        for (List<WorkflowGraphEdge> edgeList : recursionResult) {

            Deque<WorkflowGraphHop> deque = new LinkedList<>();

            // transform edges into the necessary WorkflowGraphHops
            for (WorkflowGraphEdge edge : edgeList) {

                WorkflowGraphNode currentNode = nodes.get(edge.getSourceExecutionIdentifier());
                WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());

                WorkflowGraphHop nextHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
                    currentNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
                    nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());

                deque.addLast(nextHop);
            }

            // check if the first node and the last node in the queue are the same. if this is not the case we need to add a dummy hop with
            // an unreachable target node, to work around a sanity check in ComponentExecutionScheduler.handleInternalEndpointDatumAdded()
            String firstExeId = edgeList.get(0).getSourceExecutionIdentifier();
            String lastExeId = edgeList.get(edgeList.size() - 1).getTargetExecutionIdentifier();
            if (firstExeId.equals(lastExeId)) {
                hopsDequesSnapshot.add(deque);
            } else {

                // hopOutputName must not be a existing outputName!
                String dummyHopOuputName = DUMMY + UUID.randomUUID().toString();
                String dummyTargetExecutionIdentifier = DUMMY + UUID.randomUUID().toString();
                String dummyTargetInputName = DUMMY + UUID.randomUUID().toString();

                WorkflowGraphHop nextHop =
                    new WorkflowGraphHop(lastExeId, dummyHopOuputName, dummyTargetExecutionIdentifier, dummyTargetInputName);
                deque.addLast(nextHop);

                hopsDequesSnapshot.add(deque);
            }
        }

        return hopsDequesSnapshot;
    }

    private void recursion(Set<WorkflowGraphNode> alreadyVisitedNodes, WorkflowGraphNode node, EndpointCharacter startEndpointCharacter,
        List<WorkflowGraphEdge> currentChain, List<List<WorkflowGraphEdge>> completedChains) {

        // get a list of all edges to follow next
        List<WorkflowGraphEdge> nextEdgesToVisit = nextEdgesToVisit(alreadyVisitedNodes, node, startEndpointCharacter);

        if (nextEdgesToVisit.isEmpty()) {
            // if there are no more edges to follow
            if (!currentChain.isEmpty()) {
                // and the currentChain already contains some hops, add the currentChain to the result list
                completedChains.add(currentChain);
            }

            return;

        } else {
            for (WorkflowGraphEdge nextEdge : nextEdgesToVisit) {

                WorkflowGraphNode nextNode = nodes.get(nextEdge.getTargetExecutionIdentifier());

                List<WorkflowGraphEdge> currentChainCopy = new LinkedList<>(currentChain);
                currentChainCopy.add(nextEdge);

                recursion(alreadyVisitedNodes, nextNode, nextEdge.getInputCharacter(), currentChainCopy, completedChains);
            }
        }
    }

    /**
     * Returns a list of {@link WorkflowGraphEdge}s that should be visited next, starting from the given {@link WorkflowGraphNode}.
     */
    private List<WorkflowGraphEdge> nextEdgesToVisit(Set<WorkflowGraphNode> alreadyVisitedNodes, WorkflowGraphNode startNode,
        EndpointCharacter startEndpointCharacter) {

        List<WorkflowGraphEdge> nextEdgesToVisit = new LinkedList<>();

        // have a look at each output
        for (String startNodeOutputId : startNode.getOutputIdentifiers()) {

            String tmpEdgeKey = WorkflowGraph.createEdgeKey(startNode, startNodeOutputId);

            // if the given output has a connection to at least one other node...
            if (edges.containsKey(tmpEdgeKey)) {

                // then check all edges
                for (WorkflowGraphEdge edge : edges.get(tmpEdgeKey)) {

                    WorkflowGraphNode targetNode = nodes.get(edge.getTargetExecutionIdentifier());
                    if (alreadyVisitedNodes.contains(targetNode)) {
                        // if the target node of this edge is already considered, skip this edge
                        continue;
                    }

                    // if the start node is a driver,
                    if (startNode.isDriver()) {
                        // we also need to check that we are not leaving the loop level
                        if (startEndpointCharacter.equals(edge.getOutputCharacter())) {
                            nextEdgesToVisit.add(edge);
                            alreadyVisitedNodes.add(targetNode);
                        }
                    } else {
                        nextEdgesToVisit.add(edge);
                        alreadyVisitedNodes.add(targetNode);
                    }
                }
            }
        }

        return nextEdgesToVisit;
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
                // we are going to calculate a set of deques for each output of the start node
                for (String startNodeOutputId : startNode.getOutputIdentifiers()) {
                    Set<Deque<WorkflowGraphHop>> hopsDeques = new HashSet<Deque<WorkflowGraphHop>>();
                    // if the given output has a connection to another node...
                    if (edges.containsKey(WorkflowGraph.createEdgeKey(startNode, startNodeOutputId))) {
                        // ... we follow them down the graph
                        for (WorkflowGraphEdge startEdge : edges.get(WorkflowGraph.createEdgeKey(startNode, startNodeOutputId))) {
                            if (startNode.isDriver()) {
                                // if the start node is a driver, we also need to check that we are not leaving the loop level
                                if (startEnpointCharacter.equals(startEdge.getOutputCharacter())) {
                                    // TODO doens't this override the same value over and over again
                                    hopsDeques = startNewHopsSearch(startNode, startEdge, isResetSearch);
                                }
                            } else {
                                // TODO doesn't this override the same value over and over again
                                hopsDeques = startNewHopsSearch(startNode, startEdge, isResetSearch);
                            }
                        }
                        // TODO and finally only the value of the last iteration is added to the set?
                        hopsDequesPerOutput.put(startNode.getEndpointName(startNodeOutputId), hopsDeques);
                        // if the given output is not connected ...
                    } else {
                        // ... we add an empty set
                        hopsDequesPerOutput.put(startNode.getEndpointName(startNodeOutputId), hopsDeques);
                    }
                }
                alreadyDeterminedHops.put(startNodeExecutionId, hopsDequesPerOutput);
            }
        }
        return createSnapshotOfHopsDeques(alreadyDeterminedHops.get(startNodeExecutionId));
    }

    /**
     * Creates a copy for later destructive consumption by other classes without destroying the alreadyDeterminedHops data structure for
     * this class.
     */
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
                // if a hopsDeque is complete, we add it to the set of all hopsDeques
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

            // usually only the components that are connected on same loop outputs should be considered
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

    /**
     * Checks if a given node has connected outputs that are connected to the same loop and to the outer loop.
     * 
     * @param node
     * @return <code>true</code>, if the node has connections to the same loop and the outer loop.
     */
    private boolean hasNodeOppositeOutputCharacters(WorkflowGraphNode node) {
        boolean sameLoop = false;
        boolean outerLoop = false;
        for (String nodeOutputId : node.getOutputIdentifiers()) {
            // check if the node has outgoing connections for the output identified with nodeOutputId
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
     * Generates dot language string describing the workflow graph. Can be used to visualize the graph with Graphviz. (Command: dot -Tpng
     * <wf.dot> -o <wf.png>)
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
