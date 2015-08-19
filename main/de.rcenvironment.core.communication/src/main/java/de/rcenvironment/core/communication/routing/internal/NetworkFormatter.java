/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.model.NetworkResponse;
import de.rcenvironment.core.communication.model.NetworkRoutingInformation;
import de.rcenvironment.core.communication.protocol.MessageMetaData;
import de.rcenvironment.core.utils.incubator.GraphvizUtils;
import de.rcenvironment.core.utils.incubator.GraphvizUtils.DotFileBuilder;

/**
 * Provides tools to generate human readable string representations of routing components (such as {@link TopologyMap}) that are useful for
 * debugging and monitoring.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public final class NetworkFormatter {

    private static final String GRAPHVIZ_STYLE_KEY = "style";

    private static final String GRAPHVIZ_STYLE_VALUE_DASHED = "dashed";

    private static final String GRAPHVIZ_STYLE_VALUE_BOLD = "bold";

    private NetworkFormatter() {
        // no instance
    }

    /**
     * TODO krol_ph: Enter comment!
     * 
     * @param networkGraph The network Graph
     * @return A string representation for debugging/displaying.
     */
    public static String linkList(TopologyMap networkGraph) {
        return linkList(networkGraph.getAllLinks());
    }

    /**
     * Formats a collection of links to a string.
     * 
     * @param linkCollection The collection of links.
     * @return A string representation for debugging/displaying.
     */
    public static String linkList(Collection<TopologyLink> linkCollection) {
        String result = "";
        List<TopologyLink> linkList = new ArrayList<TopologyLink>(linkCollection);
        Collections.sort(linkList);

        for (TopologyLink link : linkList) {
            result += String.format("  %s --[%s]--> %s (Hash=%s)\n",
                link.getSource().getIdString(),
                link.getConnectionId(),
                link.getDestination().getIdString(),
                link.hashCode());
        }
        return result;
    }

    /**
     * Formats a list of nodes to a string.
     * 
     * @param networkGraph The network graph.
     * @return A string representation for debugging/displaying.
     */
    public static String nodeList(TopologyMap networkGraph) {
        String result = "";
        List<TopologyNode> networkNodes = new ArrayList<TopologyNode>(networkGraph.getNodes());
        Collections.sort(networkNodes);

        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        for (TopologyNode networkNode : networkNodes) {
            String nodeIdInfo = networkNode.getNodeIdentifier().getIdString();
            // mark local node
            if (nodeIdInfo.equals(networkGraph.getLocalNodeId().getIdString())) {
                nodeIdInfo += "*";
            }
            result +=
                String.format("  [%9$s] %1$s, --%4$s-> * --%5$s->, '%8$s', Seq=%2$s, Created=%3$tk:%3$tM:%3$tS, Conv=%6$s, Hash=%7$s\n",
                    nodeIdInfo,
                    // TODO quick & dirty; improve
                    networkNode.getSequenceNumber() + " (" + timestampFormat.format(new Date(networkNode.getSequenceNumber()))
                        + ")",
                    networkNode.getCreatedTime(),
                    networkGraph.getPredecessors(networkNode).size(),
                    networkGraph.getSuccessors(networkNode).size(),
                    (networkNode.getLastGraphHashCode() == networkGraph.hashCode()),
                    networkNode.hashCode(),
                    networkNode.getDisplayName(),
                    networkNode.getIsWorkflowHost());
        }
        return result;
    }

    /**
     * TODO krol_ph: Enter comment!
     * 
     * @param networkGraph The network graph.
     * @return A string representation for debugging/displaying.
     */
    public static String graphMetaData(TopologyMap networkGraph) {
        return String.format("  Local Node Id: %s, Nodes: %s, Links: %s, Fully conv.: %s, Hash=%s\n",
            networkGraph.getLocalNodeId().getIdString(),
            networkGraph.getNodeCount(),
            networkGraph.getLinkCount(),
            networkGraph.hasSameTopologyHashesForAllNodes(),
            networkGraph.hashCode());
    }

    /**
     * TODO krol_ph: Enter comment!
     * 
     * @param networkGraph The network graph.
     * @return A string representation for debugging/displaying.
     */
    public static String summary(TopologyMap networkGraph) {
        return String.format("Topology Metadata:\n%sKnown Nodes:\n%sLinks:\n%s",
            NetworkFormatter.graphMetaData(networkGraph),
            NetworkFormatter.nodeList(networkGraph),
            NetworkFormatter.linkList(networkGraph));
    }

    /**
     * Renders the current network topology to a Graphviz "dot" file.
     * 
     * @param topologyMap the {@link TopologyMap} to render
     * @return the "dot" file script content
     */
    public static String topologyToGraphviz(TopologyMap topologyMap) {
        DotFileBuilder builder = GraphvizUtils.createDotFileBuilder("rce_network");
        List<TopologyNode> networkNodes = new ArrayList<TopologyNode>(topologyMap.getNodes());
        Map<String, String> backwardEdgeProperties = new HashMap<String, String>();
        backwardEdgeProperties.put(GRAPHVIZ_STYLE_KEY, GRAPHVIZ_STYLE_VALUE_DASHED);
        for (TopologyNode networkNode : networkNodes) {
            // TODO filter to only include reachable nodes
            String vertexId = networkNode.getNodeIdentifier().getIdString();
            String label = networkNode.getDisplayName();
            builder.addVertex(vertexId, label);
        }
        List<TopologyLink> linkList = new ArrayList<TopologyLink>(topologyMap.getAllLinks());
        for (TopologyLink link : linkList) {
            String edgeId = link.getConnectionId();
            String label = edgeId.substring(0, edgeId.indexOf('-'));
            if (label.endsWith("s")) {
                builder.addEdge(link.getSource().getIdString(), link.getDestination().getIdString(), label);
            } else {
                builder.addEdge(link.getSource().getIdString(), link.getDestination().getIdString(), label, backwardEdgeProperties);
            }
        }
        // mark local node
        String localNodeId = topologyMap.getLocalNodeId().getIdString();
        builder.addVertexProperty(localNodeId, GRAPHVIZ_STYLE_KEY, GRAPHVIZ_STYLE_VALUE_BOLD);
        // generate script
        return builder.getScriptContent();
    }

    /**
     * Formats message to string.
     * 
     * @param messageContent The message content.
     * @param metaData The meta data of the message.
     * @return A string representation for debugging/displaying.
     */
    public static String message(Serializable messageContent, Map<String, String> metaData) {
        MessageMetaData handler = MessageMetaData.wrap(metaData);
        return String.format("Src='%s', Dst='%s', Body='%s', HopC=%d, MsgId='%s', Trace='%s'",
            handler.getSender(),
            handler.getFinalRecipient(),
            messageContent.toString(),
            handler.getHopCount(),
            handler.getMessageId(),
            handler.getTrace());
    }

    /**
     * Formats LSA to string.
     * 
     * @param lsa The links state advertisement.
     * @return A string representation for debugging/displaying.
     */
    public static String lsa(LinkStateAdvertisement lsa) {
        return String.format("owner=%s, links=%s, seq=%s, type=%s, hash=%s\n%s",
            lsa.getOwner(),
            lsa.getLinks().size(),
            lsa.getSequenceNumber(),
            lsa.getReason(),
            lsa.getGraphHashCode(),
            linkList(lsa.getLinks()));
    }

    /**
     * Formats LSA cache to string.
     * 
     * @param lsaCache The LSA cache.
     * @return A string representation for debugging/displaying.
     */
    public static String lsaCache(LinkStateAdvertisementBatch lsaCache) {
        String result = String.format("size=%s\n", lsaCache.size());
        for (LinkStateAdvertisement lsa : lsaCache.values()) {
            result += lsa(lsa) + "\n";
        }
        return result;
    }

    /**
     * Formats a network rout to string.
     * 
     * @param networkRoute The network route.
     * @return A string representation for debugging/displaying.
     */
    public static String networkRoute(NetworkRoute networkRoute) {
        String result =
            String.format("length: %s, %s, time: %s ms", networkRoute.getPath().size(), networkRoute.getSource().getIdString(),
                networkRoute.getComputationalEffort());
        for (TopologyLink link : networkRoute.getPath()) {
            result += String.format(" --> %s", link.getDestination().getIdString());
        }
        return result;
    }

    /**
     * Formats network statistics to string.
     * 
     * @param networkStats The network statistics.
     * @return A string representation for debugging/displaying.
     */
    public static String networkStats(NetworkStats networkStats) {
        return String.format(
            "\nSuccessful communications: %s\n"
                + "Failed communications: %s\n\n"
                + "LSAs send:     %s\n"
                + "LSAs received: %s\n"
                + "LSAs rejected: %s\n\n"
                + "Max received hop count:     %s\n"
                + "Max time to live:           %s\n"
                + "Number of computed routes:  %s\n\n"
                + "Average hop count of send LSAs:     %s\n"
                + "Average hop count of received LSAs: %s\n"
                + "Average hop count of rejected LSAs: %s\n",

            networkStats.getSuccessfulCommunications(),
            networkStats.getFailedCommunications(),

            networkStats.getSentLSAs(),
            networkStats.getReceivedLSAs(),
            networkStats.getRejectedLSAs(),

            networkStats.getMaxReceivedHopCount(),
            networkStats.getMaxTimeToLive(),
            networkStats.getShortestPathComputations(),

            networkStats.averageHopCountOfSentLSAs(),
            networkStats.averageHopCountOfReceivedLSAs(),
            networkStats.averageHopCountOfRejectedLSAs());
    }

    /**
     * Formats network response to string.
     * 
     * @param networkResponse The network response.
     * @return A string representation for debugging/displaying.
     */
    public static String networkResponseToString(NetworkResponse networkResponse) {
        return String.format("id=%s, succ=%s, code=%s, header=%s",
            networkResponse.getRequestId(),
            networkResponse.isSuccess(),
            networkResponse.getResultCode(),
            networkResponse.accessRawMetaData().toString());
    }

    /**
     * Generates a human-readable representation of a {@link TopologyMap}'s contents.
     * 
     * @param topologyMap the {@link TopologyMap} to render
     * @param extendedInfo whether extended (ie, developer) information should be added
     * @return the formatted, multi-line string
     */
    public static String formatTopologyMap(TopologyMap topologyMap, boolean extendedInfo) {
        return String.format("Nodes:\n%sConnections:\n%s",
            NetworkFormatter.formatTopologyNodes(topologyMap, extendedInfo),
            NetworkFormatter.formatTopologyLinks(topologyMap, extendedInfo));
    }

    /**
     * Renders the current network topology to a simple console-ready "network information" output.
     * 
     * @param networkGraph the {@link TopologyMap} to render
     * @return the information text.
     */
    public static String networkGraphToConsoleInfo(NetworkGraph networkGraph) {
        StringBuilder buffer = new StringBuilder();
        List<NodeIdentifier> networkNodes = new ArrayList<NodeIdentifier>(networkGraph.getNodeIds());
        buffer.append("Reachable network nodes (" + networkNodes.size() + " total):\n");
        for (NodeIdentifier nodeId : networkNodes) {
            buffer.append(String.format("  %s [%s]\n", nodeId.getAssociatedDisplayName(), nodeId.getIdString()));
        }
        buffer.append("Message channels (" + networkGraph.getLinkCount() + " total):\n");
        for (NetworkGraphLink link : networkGraph.getLinks()) {
            buffer.append(String.format("  %s--[%s]->[%s]\n", link.getSourceNodeId(), link.getLinkId(), link.getTargetNodeId()));
        }
        return buffer.toString();
    }

    /**
     * Renders the current network topology to a Graphviz "dot" file.
     * 
     * @param networkGraph the {@link TopologyMap} to render
     * @param markSpanningTree TODO
     * @return the "dot" file script content
     */
    public static String networkGraphToGraphviz(NetworkGraph networkGraph, boolean markSpanningTree) {
        DotFileBuilder builder = GraphvizUtils.createDotFileBuilder("rce_network");
        List<NodeIdentifier> networkNodes = new ArrayList<NodeIdentifier>(networkGraph.getNodeIds());

        // prepare spanning tree data if requested, or set an empty placeholder
        Set<NetworkGraphLink> spanningTreeLinks;
        if (markSpanningTree) {
            NetworkRoutingInformation routingInformation = networkGraph.getRoutingInformation();
            if (routingInformation == null) {
                throw new IllegalArgumentException("Spanning tree requested for a graph without routing information");
            }
            spanningTreeLinks = routingInformation.getSpanningTreeLinks();
        } else {
            // placeholder
            spanningTreeLinks = new HashSet<NetworkGraphLink>();
        }

        // add vertices
        for (NodeIdentifier nodeId : networkNodes) {
            String vertexId = nodeId.getIdString();
            String label = nodeId.getAssociatedDisplayName(); // TODO improve
            builder.addVertex(vertexId, label);
        }

        // add edges
        List<NetworkGraphLink> linkList = new ArrayList<NetworkGraphLink>(networkGraph.getLinks());
        // TODO move style handling into GraphViz builder? (requires adding edge ids) - misc_ro
        StringBuilder styleBuilder = new StringBuilder();
        for (NetworkGraphLink link : linkList) {
            String edgeId = link.getLinkId();
            String label = edgeId.substring(0, edgeId.indexOf('-'));
            Map<String, String> edgeProperties = new HashMap<String, String>();
            if (label.endsWith("r")) {
                styleBuilder.append(GRAPHVIZ_STYLE_VALUE_DASHED);
            }
            if (spanningTreeLinks.contains(link)) {
                if (styleBuilder.length() != 0) {
                    styleBuilder.append(",");
                }
                styleBuilder.append(GRAPHVIZ_STYLE_VALUE_BOLD);
            }
            edgeProperties.put(GRAPHVIZ_STYLE_KEY, styleBuilder.toString());
            styleBuilder.setLength(0);
            builder.addEdge(link.getSourceNodeId().getIdString(), link.getTargetNodeId().getIdString(), label,
                edgeProperties);
        }

        // mark local node
        String localNodeId = networkGraph.getLocalNodeId().getIdString();
        builder.addVertexProperty(localNodeId, GRAPHVIZ_STYLE_KEY, GRAPHVIZ_STYLE_VALUE_BOLD);

        // generate script
        return builder.getScriptContent();
    }

    private static String formatTopologyNodes(TopologyMap topologyMap, boolean extendedInfo) {
        NodeIdentifier localNodeId = topologyMap.getLocalNodeId();
        Set<NodeIdentifier> reachableNodes = topologyMap.getIdsOfReachableNodes(false);
        List<TopologyNode> topologyNodes = new ArrayList<TopologyNode>(topologyMap.getNodes());

        StringBuilder result = new StringBuilder();

        Collections.sort(topologyNodes);
        for (TopologyNode topologyNode : topologyNodes) {
            // do not show unreachable nodes (at least, not by default)
            NodeIdentifier nodeId = topologyNode.getNodeIdentifier();
            if (!reachableNodes.contains(nodeId)) {
                continue;
            }
            // mark local node
            String markers = "";
            if (nodeId.equals(localNodeId)) {
                markers += " <self>";
            }
            result.append(
                String.format("  %s%s\n",
                    nodeId,
                    markers));
        }
        return result.toString();
    }

    private static String formatTopologyLinks(TopologyMap topologyMap, boolean extendedInfo) {
        StringBuilder result = new StringBuilder();
        List<TopologyLink> linkList = new ArrayList<TopologyLink>(topologyMap.getAllLinks());
        Collections.sort(linkList);

        for (TopologyLink link : linkList) {
            result.append(String.format("  %s --[%s]--> %s\n",
                link.getSource(),
                link.getConnectionId(),
                link.getDestination()));
        }
        return result.toString();
    }

}
