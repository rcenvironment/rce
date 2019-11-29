/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * @author Alexander Weinert (refactoring)
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

    private final Map<ComponentExecutionIdentifier, WorkflowGraphNode> nodes;

    private final WorkflowGraphEdges edges;

    private final Map<ComponentExecutionIdentifier, Map<String, Set<WorkflowGraphPath>>> determinedHopsToDriverOnFailure =
        new HashMap<>();

    private final Map<ComponentExecutionIdentifier, WorkflowGraphNode> determinedDriverNodes = new HashMap<>();

    // TODO this internal map should be created within the constructor and not handed into the constructor
    public WorkflowGraph(Map<ComponentExecutionIdentifier, WorkflowGraphNode> nodes, Set<WorkflowGraphEdge> edgeSet) {
        this.nodes = nodes;
        this.edges = WorkflowGraphEdges.create(edgeSet);
    }

    /**
     * Returns {@link WorkflowGraphPath}s that need to be traversed when node with given execution identifier need to reset its loop.
     * 
     * @param startNodeExecutionId execution identifier of the node that need to reset its loop
     * @return {@link WorkflowGraphPath}s to traverse for each output of the node that need to reset its loop
     */
    public synchronized Set<WorkflowGraphPath> getHopsToTraverseWhenResetting(ComponentExecutionIdentifier startNodeExecutionId) {
        // A set of node that stores all visited nodes during the recursive calculation of the hops to traverse for resetting a nested loop.
        Set<WorkflowGraphNode> alreadyVisitedNodes = new HashSet<>();

        List<List<WorkflowGraphEdge>> recursionResult = new ArrayList<>();

        recursion(alreadyVisitedNodes, nodes.get(startNodeExecutionId), EndpointCharacter.SAME_LOOP, new ArrayList<WorkflowGraphEdge>(),
            recursionResult);

        // create a copy of the data structure
        Set<WorkflowGraphPath> pathsSnapshot = new HashSet<>();
        for (List<WorkflowGraphEdge> edgeList : recursionResult) {

            WorkflowGraphPath path = new WorkflowGraphPath();

            // transform edges into the necessary WorkflowGraphHops
            for (WorkflowGraphEdge edge : edgeList) {

                WorkflowGraphNode currentNode = nodes.get(edge.getSourceExecutionIdentifier());
                WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());

                WorkflowGraphHop nextHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
                    currentNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
                    nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());

                path.append(nextHop);
            }

            // check if the first node and the last node in the path are the same. if this is not the case we need to add a dummy hop with
            // an unreachable target node, to work around a sanity check in ComponentExecutionScheduler.handleInternalEndpointDatumAdded()
            ComponentExecutionIdentifier firstExeId = edgeList.get(0).getSourceExecutionIdentifier();
            ComponentExecutionIdentifier lastExeId = edgeList.get(edgeList.size() - 1).getTargetExecutionIdentifier();
            if (firstExeId.equals(lastExeId)) {
                pathsSnapshot.add(path);
            } else {

                // hopOutputName must not be a existing outputName!
                String dummyHopOuputName = DUMMY + UUID.randomUUID().toString();
                ComponentExecutionIdentifier dummyTargetExecutionIdentifier =
                    new ComponentExecutionIdentifier(DUMMY + UUID.randomUUID().toString());
                String dummyTargetInputName = DUMMY + UUID.randomUUID().toString();

                WorkflowGraphHop nextHop =
                    new WorkflowGraphHop(lastExeId, dummyHopOuputName, dummyTargetExecutionIdentifier, dummyTargetInputName);
                path.append(nextHop);

                pathsSnapshot.add(path);
            }
        }

        return pathsSnapshot;
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
        } else {
            for (WorkflowGraphEdge nextEdge : nextEdgesToVisit) {

                WorkflowGraphNode nextNode = nodes.get(nextEdge.getTargetExecutionIdentifier());

                List<WorkflowGraphEdge> currentChainCopy = new ArrayList<>(currentChain);
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

        List<WorkflowGraphEdge> nextEdgesToVisit = new ArrayList<>();

        // have a look at each output
        for (String startNodeOutputId : startNode.getOutputIdentifiers()) {

            // if the given output has a connection to at least one other node,
            // then check all edges
            for (WorkflowGraphEdge edge : edges.getOutgoingEdges(startNode, startNodeOutputId)) {

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

        return nextEdgesToVisit;
    }

    /**
     * Returns {@link WorkflowGraphPath}s that need to be traversed when node with given execution identifier failed within a fault-tolerant
     * loop.
     * 
     * @param startNodeExecutionId execution identifier of the node that failed within a fault-tolerant loop
     * @return {@link WorkflowGraphPath}s to traverse for each output of the node that failed
     * 
     * @throws ComponentExecutionException if searching for driver node fails (might be an user error)
     */
    public Map<String, Set<WorkflowGraphPath>> getHopsToTraverseOnFailure(ComponentExecutionIdentifier startNodeExecutionId)
        throws ComponentExecutionException {

        Map<String, Set<WorkflowGraphPath>> pathsPerOutput;
        if (nodes.get(startNodeExecutionId).isDriver()) {
            pathsPerOutput = getHopsToTraverseToGetToLoopDriver(startNodeExecutionId, EndpointCharacter.OUTER_LOOP,
                determinedHopsToDriverOnFailure);
        } else {
            pathsPerOutput = getHopsToTraverseToGetToLoopDriver(startNodeExecutionId, EndpointCharacter.SAME_LOOP,
                determinedHopsToDriverOnFailure);
        }

        return pathsPerOutput;
    }

    /**
     * The recursive search for loop drivers implemented in
     * {@link WorkflowGraph#getHopsToTraverseToGetToLoopDriver(ComponentExecutionIdentifier, EndpointCharacter, Map)} returns, for each
     * output of the node represented by the given {@link ComponentExecutionIdentifier}, a set of paths leading to some input of the loop
     * driver. Thus, the method may return multiple paths (possibly from differing outputs of the given node) that all lead to the same
     * input of the same loop driver. In further processing, however, we only require one path per input of the loop driver.
     * 
     * Thus, this method ``cleans up'' the given mapping such that for each input of the loop driver, at most one path exists in the sets
     * contained in the given map.
     * 
     * @param pathsPerOutput A map from output identifiers of some node (which is not given as a parameter to this method) to sets of paths
     *                       leading from that node to its loop driver. As the loop driver of a given node is unique, there exists a unique
     *                       source and target that is common to all paths in all sets contained in the map.
     */
    private void sanitizePathsPerOutput(Map<String, Set<WorkflowGraphPath>> pathsPerOutput) {
        Set<String> visitedInputs = new HashSet<>();
        Iterator<Entry<String, Set<WorkflowGraphPath>>> pathsPerOutputIterator = pathsPerOutput.entrySet().iterator();
        while (pathsPerOutputIterator.hasNext()) {
            Entry<String, Set<WorkflowGraphPath>> pathsForOutput = pathsPerOutputIterator.next();
            Iterator<WorkflowGraphPath> hopsForOutputIterator = pathsForOutput.getValue().iterator();
            while (hopsForOutputIterator.hasNext()) {
                WorkflowGraphPath hops = hopsForOutputIterator.next();
                String targetInputName = hops.getLast().getTargetInputName();
                if (visitedInputs.contains(targetInputName)) {
                    hopsForOutputIterator.remove();
                } else {
                    visitedInputs.add(targetInputName);
                }
            }
            if (pathsForOutput.getValue().isEmpty()) {
                pathsPerOutputIterator.remove();
            }
        }
    }

    /**
     * Returns the {@link WorkflowGraphNode} of the driver that controls the node with the given execution identifier.
     * 
     * @param executionIdentifier execution identifier of the node which driver needs to be found
     * @return {@link WorkflowGraphNode} of the driver that controls the node with the given execution identifier.
     * 
     * @throws ComponentExecutionException if searching for driver node fails (signifies a developer error)
     */
    public WorkflowGraphNode getLoopDriver(ComponentExecutionIdentifier executionIdentifier) throws ComponentExecutionException {

        if (!determinedDriverNodes.containsKey(executionIdentifier)) {
            getHopsToTraverseOnFailure(executionIdentifier);
        }

        return determinedDriverNodes.get(executionIdentifier);
    }

    private Map<String, Set<WorkflowGraphPath>> getHopsToTraverseToGetToLoopDriver(ComponentExecutionIdentifier startNodeExecutionId,
        EndpointCharacter startEndpointCharacter,
        Map<ComponentExecutionIdentifier, Map<String, Set<WorkflowGraphPath>>> alreadyDeterminedHops) throws ComponentExecutionException {
        synchronized (alreadyDeterminedHops) {
            if (!alreadyDeterminedHops.containsKey(startNodeExecutionId)) {
                final Map<String, Set<WorkflowGraphPath>> pathsPerOutput = new HashMap<String, Set<WorkflowGraphPath>>();
                final WorkflowGraphNode startNode = nodes.get(startNodeExecutionId);
                // we are going to calculate a set of paths for each output of the start node
                for (String startNodeOutputId : startNode.getOutputIdentifiers()) {
                    Set<WorkflowGraphPath> paths = new HashSet<WorkflowGraphPath>();
                    // if the given output has a connection to another node...
                    if (edges.containsOutgoingEdge(startNode, startNodeOutputId)) {
                        // ... we follow them down the graph
                        for (WorkflowGraphEdge startEdge : edges.getOutgoingEdges(startNode, startNodeOutputId)) {
                            if (startNode.isDriver()) {
                                // if the start node is a driver, we also need to check that we are not leaving the loop level
                                if (startEndpointCharacter.equals(startEdge.getOutputCharacter())) {
                                    // TODO doens't this override the same value over and over again
                                    paths = startNewHopsSearch(startNode, startEdge);
                                }
                            } else {
                                // TODO doesn't this override the same value over and over again
                                paths = startNewHopsSearch(startNode, startEdge);
                            }
                        }
                        // TODO and finally only the value of the last iteration is added to the set?
                        pathsPerOutput.put(startNode.getEndpointName(startNodeOutputId), paths);
                        // if the given output is not connected ...
                    } else {
                        // ... we add an empty set
                        pathsPerOutput.put(startNode.getEndpointName(startNodeOutputId), paths);
                    }
                }
                alreadyDeterminedHops.put(startNodeExecutionId, pathsPerOutput);
            }
        }
        sanitizePathsPerOutput(alreadyDeterminedHops.get(startNodeExecutionId));

        return createSnapshotOfPaths(alreadyDeterminedHops.get(startNodeExecutionId));
    }

    /**
     * Creates a copy for later destructive consumption by other classes without destroying the alreadyDeterminedHops data structure for
     * this class.
     */
    private Map<String, Set<WorkflowGraphPath>> createSnapshotOfPaths(Map<String, Set<WorkflowGraphPath>> pathsMap) {
        Map<String, Set<WorkflowGraphPath>> pathsSnapshot = new HashMap<>();
        for (Map.Entry<String, Set<WorkflowGraphPath>> entry : pathsMap.entrySet()) {
            final Set<WorkflowGraphPath> snapshotSet = new HashSet<>();
            for (WorkflowGraphPath path : entry.getValue()) {
                snapshotSet.add(WorkflowGraphPath.createCopy(path));
            }
            pathsSnapshot.put(entry.getKey(), snapshotSet);
        }
        return pathsSnapshot;
    }

    private Set<WorkflowGraphPath> startNewHopsSearch(WorkflowGraphNode startNode, WorkflowGraphEdge edge)
        throws ComponentExecutionException {
        Set<WorkflowGraphPath> paths = new HashSet<>();

        WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());

        WorkflowGraphPath path = new WorkflowGraphPath();

        WorkflowGraphHop firstHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
            startNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
            nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());
        path.append(firstHop);

        determineHopsRecursively(startNode, edge, nextNode, path, paths);

        return paths;
    }

    private void determineHopsRecursively(WorkflowGraphNode startNode, WorkflowGraphEdge edge, WorkflowGraphNode targetNode,
        WorkflowGraphPath path, Set<WorkflowGraphPath> paths)
        throws ComponentExecutionException {

        if (targetNode.getExecutionIdentifier().equals(startNode.getExecutionIdentifier())) {
            // got to start node again
            return;
        }
        if (nodeAlreadyVisitedThatWay(path, targetNode, edge)) {
            return;
        }

        if (targetNode.isDriver()) {
            if (EndpointCharacter.OUTER_LOOP.equals(edge.getInputCharacter())) {
                // No end of the recursion
                continueHopSearch(startNode, targetNode, path, paths, EndpointCharacter.OUTER_LOOP);
            } else {
                // In this case, we have EndpointCharacter.SAME_LOOP.equals(edge.getInputCharacter())
                paths.add(path);
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
            continueHopSearch(startNode, targetNode, path, paths, outputCharacterToConsider);
        }
    }

    private boolean nodeAlreadyVisitedThatWay(WorkflowGraphPath path, WorkflowGraphNode nodeToVisit,
        WorkflowGraphEdge usedEdge) {

        for (final WorkflowGraphHop hop : path) {
            if (hop.getHopExecutionIdentifier().equals(nodeToVisit.getExecutionIdentifier())) {
                if (!hasNodeOppositeOutputCharacters(nodeToVisit)) {
                    return true;
                } else {
                    // expect at least one edge otherwise it is no valid hop
                    EndpointCharacter hopEdgesOutputCharacter =
                        edges.getOutgoingEdges(nodes.get(hop.getHopExecutionIdentifier()), hop.getHopOutputIdentifier())
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
            for (WorkflowGraphEdge edge : edges.getOutgoingEdges(node, nodeOutputId)) {
                if (edge.getOutputCharacter().equals(EndpointCharacter.SAME_LOOP)) {
                    sameLoop = true;
                } else if (edge.getOutputCharacter().equals(EndpointCharacter.OUTER_LOOP)) {
                    outerLoop = true;
                }
            }
        }
        return sameLoop && outerLoop;
    }

    private void continueHopSearch(WorkflowGraphNode startNode, WorkflowGraphNode targetNode, WorkflowGraphPath path,
        Set<WorkflowGraphPath> paths, EndpointCharacter outputCharacter)
        throws ComponentExecutionException {

        for (String targetNodeOutputId : targetNode.getOutputIdentifiers()) {
            for (WorkflowGraphEdge nextEdge : edges.getOutgoingEdges(targetNode, targetNodeOutputId)) {
                if (outputCharacter.equals(nextEdge.getOutputCharacter())) {
                    continueHopsSearch(startNode, targetNode, nextEdge, path, paths);
                }
            }
        }
    }

    private void continueHopsSearch(WorkflowGraphNode startNode, WorkflowGraphNode currentNode, WorkflowGraphEdge edge,
        WorkflowGraphPath path, Set<WorkflowGraphPath> paths)
        throws ComponentExecutionException {

        WorkflowGraphPath newPath = WorkflowGraphPath.createCopy(path);

        WorkflowGraphNode nextNode = nodes.get(edge.getTargetExecutionIdentifier());

        WorkflowGraphHop nextHop = new WorkflowGraphHop(edge.getSourceExecutionIdentifier(),
            currentNode.getEndpointName(edge.getOutputIdentifier()), edge.getTargetExecutionIdentifier(),
            nextNode.getEndpointName(edge.getInputIdentifier()), edge.getOutputIdentifier());

        newPath.append(nextHop);

        determineHopsRecursively(startNode, edge, nextNode, newPath, paths);
    }

    /**
     * @param startNode  The node for which we record the driver node
     * @param driverNode The loop driver of the given startNode. Must not be null.
     * @throws ComponentExecutionException If, for the given startNode, a driver node has already been determined, and if that driver node
     *                                     differs from driverNode.
     */
    private void addNodeToDeterminedDriverNodes(WorkflowGraphNode startNode, WorkflowGraphNode driverNode)
        throws ComponentExecutionException {

        final WorkflowGraphNode previousDriverNode = determinedDriverNodes.put(startNode.getExecutionIdentifier(), driverNode);
        if (previousDriverNode != null && !previousDriverNode.getExecutionIdentifier().equals(driverNode.getExecutionIdentifier())) {
            // We guard against programmer errors here
            throw new ComponentExecutionException(
                "Error in workflow graph search: newly determined driver node differs from driver node determined earlier");
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
            builder.addVertex(node.getExecutionIdentifier().toString(), node.getName());
            if (node.isDriver()) {
                builder.addVertexProperty(node.getExecutionIdentifier().toString(), propNameColor, "#AA3939");
            } else if (hasNodeOppositeOutputCharacters(node)) {
                builder.addVertexProperty(node.getExecutionIdentifier().toString(), propNameColor, "#D4AA6A");
            }
            builder.addVertexProperty(node.getExecutionIdentifier().toString(), "shape", "rectangle");
            builder.addVertexProperty(node.getExecutionIdentifier().toString(), "fontsize", "10");
            builder.addVertexProperty(node.getExecutionIdentifier().toString(), "fontname", "Consolas");
        }

        for (Set<WorkflowGraphEdge> edgesSet : edges.getAllEdges()) {
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
                    .getEndpointName(edge.getOutputIdentifier()),
                    nodes.get(edge.getTargetExecutionIdentifier())
                        .getEndpointName(edge.getInputIdentifier()));
                builder.addEdge(edge.getSourceExecutionIdentifier().toString(), edge.getTargetExecutionIdentifier().toString(), label,
                    edgeProps);
            }

        }
        return builder.getScriptContent();
    }

}
