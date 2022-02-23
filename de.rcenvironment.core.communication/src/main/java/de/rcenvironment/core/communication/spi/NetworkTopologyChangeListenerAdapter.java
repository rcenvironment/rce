/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.spi;

import java.util.Set;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

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
    public void onReachableNodesChanged(Set<InstanceNodeSessionId> reachableNodes, Set<InstanceNodeSessionId> addedNodes,
        Set<InstanceNodeSessionId> removedNodes) {}

}
