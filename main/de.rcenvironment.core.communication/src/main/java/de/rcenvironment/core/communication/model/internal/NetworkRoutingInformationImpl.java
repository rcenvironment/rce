/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.model.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NetworkRoutingInformation;
import de.rcenvironment.core.communication.routing.internal.NetworkFormatter;
import de.rcenvironment.core.communication.routing.internal.v2.NoRouteToNodeException;
import de.rcenvironment.core.utils.common.AutoCreationMap;
import de.rcenvironment.core.utils.common.StatsCounter;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

/**
 * Internal implementation of {@link NetworkRoutingInformation}.
 * 
 * @author Robert Mischke
 */
public final class NetworkRoutingInformationImpl implements NetworkRoutingInformation {

    private Map<NodeIdentifier, NetworkGraphLink> routingTable;

    private Map<NodeIdentifier, NetworkGraphLink> incomingEdgesById;

    private Set<NetworkGraphLink> spanningTreeLinkSet;

    private Map<NodeIdentifier, List<NetworkGraphLink>> spanningTreeLinkMap;

    // for unit testing
    private int routingCacheMisses = 0;

    private final NodeIdentifier localNodeId;

    private final Map<NodeIdentifier, NetworkGraphLinkImpl> incomingEdgeMap;

    private final DijkstraShortestPath<NodeIdentifier, NetworkGraphLinkImpl> shortestPathAlgorithm;

    private final Set<NodeIdentifier> reachableNodes;

    public NetworkRoutingInformationImpl(NetworkGraphImpl rawNetworkGraph) {
        DirectedSparseMultigraph<NodeIdentifier, NetworkGraphLinkImpl> rawJungGraph = rawNetworkGraph.getJungGraph();
        this.localNodeId = rawNetworkGraph.getLocalNodeId();
        // Note: unless edge weights are used, UnweightedShortestPath would work as well - misc_ro
        this.shortestPathAlgorithm = new DijkstraShortestPath<NodeIdentifier, NetworkGraphLinkImpl>(rawJungGraph);
        try {
            this.incomingEdgeMap = shortestPathAlgorithm.getIncomingEdgeMap(rawNetworkGraph.getLocalNodeId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(NetworkFormatter.networkGraphToGraphviz(rawNetworkGraph, false), e);
        }
        reachableNodes = Collections.unmodifiableSet(incomingEdgeMap.keySet());
    }

    @Override
    public Set<NodeIdentifier> getReachableNodes() {
        return reachableNodes;
    }

    @Override
    public synchronized NetworkGraphLink getNextLinkTowards(NodeIdentifier targetNodeId) throws NoRouteToNodeException {
        StatsCounter.count("Routing", "Route requests");
        
        if (targetNodeId.equals(localNodeId)) {
            throw new NoRouteToNodeException("Cannot route to Local node", localNodeId);
        }

        if (routingTable == null) {
            StatsCounter.count("Routing", "Routing table calculations");
            
            // initialize basic structures
            routingTable = new HashMap<NodeIdentifier, NetworkGraphLink>();
            incomingEdgesById = new HashMap<NodeIdentifier, NetworkGraphLink>();
            for (NetworkGraphLinkImpl link : incomingEdgeMap.values()) {
                if (link != null) {
                    incomingEdgesById.put(link.getTargetNodeId(), link);
                }
            }
        }

        // individual routing map entries are calculated lazily, as in large networks,
        // only a few nodes will probably be contacted at once (TODO actually track/measure this) - misc_ro
        return determineRoutingTableEntryFor(targetNodeId);
    }

    @Override
    public synchronized List<? extends NetworkGraphLink> getRouteTo(NodeIdentifier destination) {
        if (destination.equals(localNodeId)) {
            throw new IllegalArgumentException("Invalid route request to local node");
        }
        List<? extends NetworkGraphLink> path = shortestPathAlgorithm.getPath(localNodeId, destination);
        if (path.size() != 0) {
            return Collections.unmodifiableList(path);
        } else {
            // empty path = target unreachable
            return null;
        }
    }

    @Override
    public NetworkGraphLink getNextLinkTowards(NetworkGraphNode targetNode) throws NoRouteToNodeException {
        return getNextLinkTowards(targetNode.getNodeId());
    }

    @Override
    public synchronized Set<NetworkGraphLink> getSpanningTreeLinks() {
        // lazy init, as this is rarely called
        if (spanningTreeLinkSet == null) {
            spanningTreeLinkSet = new HashSet<NetworkGraphLink>();
            for (NetworkGraphLinkImpl link : incomingEdgeMap.values()) {
                if (link != null) {
                    spanningTreeLinkSet.add(link);
                }
            }
            spanningTreeLinkSet = Collections.unmodifiableSet(spanningTreeLinkSet);
        }
        return spanningTreeLinkSet;
    }

    @Override
    public synchronized Map<NodeIdentifier, List<NetworkGraphLink>> getSpanningTreeLinkMap() {
        if (spanningTreeLinkMap == null) {
            // lazy init, as this is rarely called
            spanningTreeLinkMap = createSpanningTree();
        }
        // note: not fully guarded against external modification (inner maps are mutable) - misc_ro
        return spanningTreeLinkMap;
    }

    protected int getRoutingCacheMisses() {
        return routingCacheMisses;
    }

    protected void resetCacheMisses() {
        routingCacheMisses = 0;
    }

    private NetworkGraphLink determineRoutingTableEntryFor(NodeIdentifier targetNodeId) throws NoRouteToNodeException {
        NetworkGraphLink result = routingTable.get(targetNodeId);
        // cached entry present?
        if (result != null) {
            return result;
        }
        routingCacheMisses++;

        NetworkGraphLink incomingEdge = incomingEdgesById.get(targetNodeId);
        // consistency check
        if (incomingEdge == null) {
            throw new NoRouteToNodeException("No incoming edge for " + targetNodeId, targetNodeId);
        }
        NodeIdentifier predecessorNodeId = incomingEdge.getSourceNodeId();
        if (predecessorNodeId.equals(localNodeId)) {
            // source node reached; current target node is adjacent and reachable
            result = incomingEdge;
        } else {
            // otherwise, delegate to predecessor
            result = determineRoutingTableEntryFor(predecessorNodeId);
        }
        // add result to cache and return
        routingTable.put(targetNodeId, result);
        return result;
    }

    private Map<NodeIdentifier, List<NetworkGraphLink>> createSpanningTree() {
        AutoCreationMap<NodeIdentifier, List<NetworkGraphLink>> spanningTreeLinks =
            new AutoCreationMap<NodeIdentifier, List<NetworkGraphLink>>() {

                @Override
                protected List<NetworkGraphLink> createNewEntry(NodeIdentifier key) {
                    return new ArrayList<NetworkGraphLink>();
                }
            };

        // build spanning tree from incoming edge map (map inversion)
        for (NetworkGraphLinkImpl link : incomingEdgeMap.values()) {
            if (link != null) {
                spanningTreeLinks.get(link.getSourceNodeId()).add(link);
            }
        }

        Map<NodeIdentifier, List<NetworkGraphLink>> temp = spanningTreeLinks.getImmutableShallowCopy();
        return temp;
    }

}
