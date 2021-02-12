/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import java.util.Collection;

/**
 * 
 * 
 * @author Robert Mischke
 */
public interface NetworkGraphWithProperties extends NetworkGraph {

    /**
     * @return all nodes (the vertices) of the network graph, in no particular order
     */
    Collection<? extends NetworkGraphNode> getNodes();

    /**
     * @param nodeId the nodeId of the topology node
     * @return the graph node representing the matching topology node
     */
    NetworkGraphNode getNodeById(InstanceNodeSessionId nodeId);

}
