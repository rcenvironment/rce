/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * This class represents an entry in the routing table (see {@link RoutingTable}).
 * 
 * @author Phillip Kroll
 */
public class NetworkRoute implements Serializable {

    private static final long serialVersionUID = 1477187382233968105L;

    private final InstanceNodeSessionId source;

    private final InstanceNodeSessionId destination;

    private final List<TopologyLink> path;

    private final List<InstanceNodeSessionId> nodes;

    private final long computationalEffort;

    public NetworkRoute(InstanceNodeSessionId source, InstanceNodeSessionId destination, List<TopologyLink> path,
        List<InstanceNodeSessionId> nodes, long computationalEffort) {
        this.source = source;
        this.destination = destination;
        this.path = path;
        this.nodes = nodes;
        this.computationalEffort = computationalEffort;
    }

    /**
     * @return The network link to use in order to reach the next node on the route.
     */
    public TopologyLink getFirstLink() {
        if (path.size() <= 0) {
            return null;
        } else {
            return path.get(0);
        }
    }

    /**
     * @return The node that is the first to be passed along the route.
     */
    public InstanceNodeSessionId getNextNode() {
        return nodes.get(0);
    }

    /**
     * @return Whether there is a path at all.
     */
    public boolean validate() {
        return (path.size() > 0 && nodes.size() > 0 && !source.equals(destination));
    }

    /**
     * @return Returns the route length.
     */
    public int getLength() {
        return path.size();
    }

    /**
     * @return Returns the source.
     */
    public InstanceNodeSessionId getSource() {
        return source;
    }

    /**
     * @return Returns the destination.
     */
    public InstanceNodeSessionId getDestination() {
        return destination;
    }

    /**
     * @return Returns the path.
     */
    public List<TopologyLink> getPath() {
        return path;
    }

    /**
     * @return Returns the nodes.
     */
    public List<InstanceNodeSessionId> getNodes() {
        return nodes;
    }

    /**
     * TODO krol_ph: Comment!
     * @return Returns the computationalEffort.
     */
    public long getComputationalEffort() {
        return computationalEffort;
    }

}
