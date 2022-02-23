/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing;

import de.rcenvironment.core.communication.common.NetworkGraph;
import de.rcenvironment.core.communication.routing.internal.LinkStateRoutingProtocolManager;

/**
 * A service that provides message routing operations.
 * 
 * TODO 3.0.0: rework this interface; low cohesion
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public interface NetworkRoutingService {

    /**
     * @return the current unfiltered ("raw") network/topology graph
     */
    NetworkGraph getRawNetworkGraph();

    /**
     * @return the current network/topology graph, filtered for reachable nodes
     */
    NetworkGraph getReachableNetworkGraph();

    /**
     * Returns a human-readable summary of the current network state.
     * 
     * @param type the type of formatting to use; currently, "info" and "graphviz" are supported
     * 
     * @return the network summary text representation
     */
    String getFormattedNetworkInformation(String type);

    /**
     * Makes the {@link LinkStateRoutingProtocolManager} available to other services.
     * 
     * TODO refactor?
     * 
     * @return the protocol manager
     */
    LinkStateRoutingProtocolManager getProtocolManager();

}
