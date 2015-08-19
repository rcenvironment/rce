/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.spi;

import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Default {@link NetworkTopologyChangeListener} implementation to allow listeners to implement only the methods they are actually
 * interested in.
 * 
 * @author Robert Mischke
 */
public class NetworkTopologyChangeListenerAdapter implements NetworkTopologyChangeListener {

    @Override
    public void onNetworkTopologyChanged() {}

    @Override
    public void onReachableNetworkChanged(NetworkGraph networkGraph) {}

    @Override
    public void onReachableNodesChanged(Set<NodeIdentifier> reachableNodes, Set<NodeIdentifier> addedNodes,
        Set<NodeIdentifier> removedNodes) {}

}
