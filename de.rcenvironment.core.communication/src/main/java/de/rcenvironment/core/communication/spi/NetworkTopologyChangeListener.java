/*
 * Copyright 2006-2021 DLR, Germany
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
 * Listener interface for topology changes. The main events triggering topology changes are nodes entering or leaving the network.
 * Additionally, existing nodes may create new connections, or existing connections may disappear.
 * 
 * Typically, new listeners will receive a custom {@link #onReachableNodesChanged()} callback on subscription that allows them to initialize
 * to the current observed state. From the listener's perspective, the call will contain parameters as if the last observed set of nodes was
 * empty, and all current nodes just became available.
 * 
 * @author Robert Mischke
 */
public interface NetworkTopologyChangeListener {

    /**
     * Signals that the known network topology has changed.
     */
    void onNetworkTopologyChanged();

    /**
     * Signals that the topology of the reachable network has changed.
     * 
     * @param networkGraph the new network graph representation
     */
    void onReachableNetworkChanged(NetworkGraph networkGraph);

    /**
     * Reports a change to the set of reachable nodes. As a convenience, the difference to the previous state is provided as well.
     * 
     * @param reachableNodes the new set of reachable nodes
     * @param addedNodes nodes that have been added by this change
     * @param removedNodes nodes that have been removed by this change
     */
    void onReachableNodesChanged(Set<InstanceNodeSessionId> reachableNodes, Set<InstanceNodeSessionId> addedNodes,
        Set<InstanceNodeSessionId> removedNodes);
}
