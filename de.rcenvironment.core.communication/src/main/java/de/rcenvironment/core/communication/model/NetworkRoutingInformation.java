/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.routing.internal.v2.NoRouteToNodeException;

/**
 * Provides routing information of a {@link NetworkGraph}. This includes single-query access to a routing table, and information about the
 * routing spanning tree.
 * 
 * @author Robert Mischke
 */
public interface NetworkRoutingInformation {

    /**
     * @return the set of all nodes that are reachable from the local node of the source graph
     */
    Set<InstanceNodeSessionId> getReachableNodes();

    /**
     * @param targetNodeId the id of the network node to contact
     * @return the first link of the shortes path to the target node
     * @throws NoRouteToNodeException if the target node is either the local node, or an unreachable node
     */
    NetworkGraphLink getNextLinkTowards(InstanceNodeSessionId targetNodeId) throws NoRouteToNodeException;

    /**
     * @param targetNode the network node to contact
     * @return the first link of the shortes path to the target node
     * @throws NoRouteToNodeException if the target node is either the local node, or an unreachable node
     */
    NetworkGraphLink getNextLinkTowards(NetworkGraphNode targetNode) throws NoRouteToNodeException;

    /**
     * @param destination the target node's id
     * @return a full path to the target node; used for unit testing
     */
    List<? extends NetworkGraphLink> getRouteTo(InstanceNodeSessionId destination);

    /**
     * @return the set of links in the routing spanning tree
     */
    Set<NetworkGraphLink> getSpanningTreeLinks();

    /**
     * @return the set of outgoing links for each node in the routing spanning tree
     */
    Map<InstanceNodeSessionId, List<NetworkGraphLink>> getSpanningTreeLinkMap();

}
