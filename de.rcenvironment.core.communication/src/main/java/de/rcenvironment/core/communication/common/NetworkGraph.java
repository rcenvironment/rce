/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.communication.model.NetworkRoutingInformation;

/**
 * A model representing a disconnected snapshot of the current network graph. Changes to the actual, live network state will not affect this
 * model.
 * 
 * @author Robert Mischke
 */
public interface NetworkGraph {

    /**
     * @return the id of the local node
     */
    InstanceNodeSessionId getLocalNodeId();

    /**
     * @return the number of nodes/vertices in this graph
     */
    int getNodeCount();

    /**
     * @return all {@link InstanceNodeSessionId}s of the nodes (the vertices) of the network graph
     */
    Set<InstanceNodeSessionId> getNodeIds();

    /**
     * @return the number of links/edges in this graph
     */
    int getLinkCount();

    /**
     * @return all links (the edges) of the network graph, in no particular order
     */
    Collection<? extends NetworkGraphLink> getLinks();

    /**
     * @param sourceNodeId the source node's id
     * @param targetNodeId the target node's id
     * @return true if there is a direct link from "source" to "target"
     */
    boolean containsLinkBetween(InstanceNodeSessionId sourceNodeId, InstanceNodeSessionId targetNodeId);

    /**
     * Creates a {@link NetworkGraphWithProperties} from this {@link NetworkGraph} by providing the node properties to attach.
     * 
     * TODO add (im)mutability information
     * 
     * @param nodeProperties the node properies to attach
     * @return the new graph
     */
    NetworkGraphWithProperties attachNodeProperties(Map<InstanceNodeSessionId, Map<String, String>> nodeProperties);

    /**
     * @return the (optional) routing information object, if available
     */
    NetworkRoutingInformation getRoutingInformation();

    /**
     * @return a compact string representation of this graph; intended for debug output, or equality checks in tests
     */
    String getCompactRepresentation();

}
