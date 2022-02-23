/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphWithProperties;
import de.rcenvironment.core.communication.model.NetworkRoutingInformation;
import edu.uci.ics.jung.algorithms.filters.FilterUtils;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

/**
 * A model representing a condensed, disconnected snapshot of the current network graph. Changes to the actual live network state will not
 * automatically affect this model.
 * 
 * @author Robert Mischke
 */
public class NetworkGraphImpl implements NetworkGraphWithProperties {

    private final InstanceNodeSessionId localNodeId;

    private final Map<InstanceNodeSessionId, NetworkGraphNodeImpl> nodeMap = new HashMap<InstanceNodeSessionId, NetworkGraphNodeImpl>();

    // TODO review: possible to reduce synchronization? - misc_ro
    private final DirectedSparseMultigraph<InstanceNodeSessionId, NetworkGraphLinkImpl> jungGraph;

    private NetworkRoutingInformation routingInformation;

    private String compactRepresentation;

    public NetworkGraphImpl(InstanceNodeSessionId localNodeId) {
        if (localNodeId == null) {
            throw new NullPointerException();
        }
        this.localNodeId = localNodeId;
        this.jungGraph = new DirectedSparseMultigraph<InstanceNodeSessionId, NetworkGraphLinkImpl>();
        addNode(localNodeId);
    }

    /**
     * Creates an induced subgraph from the given source graph, and a subset of vertices from that graph.
     * 
     * Note that attached node properties are not transfered at this time, as this is usually performed before properties are attached.
     * 
     * @param sourceGraph the source graph
     * @param subgraphVertices
     */
    private NetworkGraphImpl(NetworkGraphImpl sourceGraph, Set<InstanceNodeSessionId> subgraphVertices) {
        this.localNodeId = sourceGraph.localNodeId;
        this.jungGraph = FilterUtils.createInducedSubgraph(subgraphVertices, sourceGraph.jungGraph);
        if (!jungGraph.containsVertex(localNodeId)) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a {@link NetworkGraphWithProperties} from the given source graph by attaching the given node properties.
     * 
     * @param sourceGraph the source graph
     * @param nodeProperties the map of node properties to attach
     */
    private NetworkGraphImpl(NetworkGraphImpl sourceGraph, Map<InstanceNodeSessionId, Map<String, String>> nodeProperties) {
        this.localNodeId = sourceGraph.localNodeId;
        // note: the graph object is shared!
        // TODO add "prevent modification" flag to graphs?
        this.jungGraph = sourceGraph.jungGraph;
        for (InstanceNodeSessionId nodeId : getNodeIds()) {
            NetworkGraphNodeImpl node = new NetworkGraphNodeImpl(nodeId, nodeProperties.get(nodeId));
            if (nodeId.equals(localNodeId)) {
                node.setIsLocalNode(true);
            }
            nodeMap.put(nodeId, node);
        }
    }

    @Override
    public int getNodeCount() {
        synchronized (jungGraph) {
            return jungGraph.getVertexCount();
        }
    }

    @Override
    public Set<InstanceNodeSessionId> getNodeIds() {
        // TODO cache if used frequently
        synchronized (jungGraph) {
            return new HashSet<InstanceNodeSessionId>(jungGraph.getVertices());
        }
    }

    @Override
    public Collection<NetworkGraphNodeImpl> getNodes() {
        return Collections.unmodifiableCollection(nodeMap.values());
    }

    @Override
    public NetworkGraphNodeImpl getNodeById(InstanceNodeSessionId nodeId) {
        return nodeMap.get(nodeId);
    }

    @Override
    public int getLinkCount() {
        synchronized (jungGraph) {
            return jungGraph.getEdgeCount();
        }
    }

    @Override
    public Collection<? extends NetworkGraphLink> getLinks() {
        synchronized (jungGraph) {
            return new HashSet<NetworkGraphLink>(jungGraph.getEdges());
        }
    }

    @Override
    public boolean containsLinkBetween(InstanceNodeSessionId sourceNodeId, InstanceNodeSessionId targetNodeId) {
        Collection<NetworkGraphLinkImpl> outEdges;
        synchronized (jungGraph) {
            outEdges = jungGraph.getOutEdges(sourceNodeId);
        }
        for (NetworkGraphLinkImpl edge : outEdges) {
            if (edge.getTargetNodeId().equals(targetNodeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an induced subgraph that consists only of nodes that are reachable from the local node.
     * 
     * @return the reachable subgraph
     */
    public NetworkGraphImpl reduceToReachableGraph() {
        generateRoutingInformation();

        NetworkGraphImpl reachableGraph = new NetworkGraphImpl(this, routingInformation.getReachableNodes());

        // valid for raw graph as well, so attach it there, too
        reachableGraph.setRoutingInformation(routingInformation);

        return reachableGraph;
    }

    @Override
    public NetworkGraphWithProperties attachNodeProperties(Map<InstanceNodeSessionId, Map<String, String>> nodeProperties) {
        return new NetworkGraphImpl(this, nodeProperties);
    }

    /**
     * Computes and returns the {@link NetworkRoutingInformation} for this graph.
     * 
     * @return the {@link NetworkRoutingInformation}
     */
    public NetworkRoutingInformation generateRoutingInformation() {
        if (routingInformation != null) {
            throw new IllegalStateException("Not expected to be called twice");
        }
        routingInformation = new NetworkRoutingInformationImpl(this);
        return routingInformation;
    }

    @Override
    public NetworkRoutingInformation getRoutingInformation() {
        return routingInformation;
    }

    @Override
    public synchronized String getCompactRepresentation() {
        if (compactRepresentation == null) {
            compactRepresentation = generateCompactRepresentation();
        }
        return compactRepresentation;
    }

    private String generateCompactRepresentation() {
        StringBuilder buffer = new StringBuilder();
        // sort nodes by node id
        Set<InstanceNodeSessionId> sortedNodes = new TreeSet<InstanceNodeSessionId>(new Comparator<InstanceNodeSessionId>() {

            @Override
            public int compare(InstanceNodeSessionId o1, InstanceNodeSessionId o2) {
                return o1.getInstanceNodeSessionIdString().compareTo(o2.getInstanceNodeSessionIdString());
            }
        });
        Collection<InstanceNodeSessionId> unsortedNodes = getNodeIds();
        sortedNodes.addAll(unsortedNodes);
        // detect collisions
        if (unsortedNodes.size() != sortedNodes.size()) {
            throw new IllegalStateException();
        }
        buffer.append("Nodes: ");
        for (InstanceNodeSessionId node : sortedNodes) {
            buffer.append(node.getInstanceNodeSessionIdString());
            buffer.append(" ");
        }
        // sort links by node id
        Set<NetworkGraphLink> sortedLinks = new TreeSet<NetworkGraphLink>(new Comparator<NetworkGraphLink>() {

            @Override
            public int compare(NetworkGraphLink o1, NetworkGraphLink o2) {
                return o1.getLinkId().compareTo(o2.getLinkId());
            }
        });
        Collection<? extends NetworkGraphLink> unsortedLinks = getLinks();
        sortedLinks.addAll(unsortedLinks);
        // detect collisions
        if (unsortedLinks.size() != sortedLinks.size()) {
            throw new IllegalStateException();
        }
        buffer.append("Links: ");
        for (NetworkGraphLink link : sortedLinks) {
            buffer.append(link.getLinkId());
            buffer.append(":");
            buffer.append(link.getSourceNodeId().getInstanceNodeSessionIdString());
            buffer.append(">");
            buffer.append(link.getTargetNodeId().getInstanceNodeSessionIdString());
            buffer.append(" ");
        }
        buffer.append("\n");

        String c = buffer.toString();
        return c;
    }

    @Override
    public InstanceNodeSessionId getLocalNodeId() {
        return localNodeId;
    }

    /**
     * @param nodeId the graph node/vertex to add
     */
    public void addNode(InstanceNodeSessionId nodeId) {
        // attach node properties if already present?
        synchronized (jungGraph) {
            if (jungGraph.containsVertex(nodeId)) {
                if (!nodeId.equals(localNodeId)) {
                    LogFactory.getLog(getClass()).warn("Existing node added again: " + nodeId);
                }
                return;
            }
            jungGraph.addVertex(nodeId);
        }
    }

    /**
     * @param linkId the id of the new link
     * @param source the source node's id
     * @param target the target node's id
     */
    public void addLink(String linkId, InstanceNodeSessionId source, InstanceNodeSessionId target) {
        addLink(new NetworkGraphLinkImpl(linkId, source, target));
    }

    /**
     * @param link the graph link/edge to add
     */
    public void addLink(NetworkGraphLinkImpl link) {
        synchronized (jungGraph) {
            jungGraph.addEdge(link, link.getSourceNodeId(), link.getTargetNodeId());
        }
    }

    protected DirectedSparseMultigraph<InstanceNodeSessionId, NetworkGraphLinkImpl> getJungGraph() {
        synchronized (jungGraph) {
            return jungGraph;
        }
    }

    private void setRoutingInformation(NetworkRoutingInformation routingInformation) {
        this.routingInformation = routingInformation;
    }

}
