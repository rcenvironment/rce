/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.Date;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Represents a RCE instance in the network. More abstract a {@link TopologyNode} is a node in a graph that is encapsulated in
 * {@link TopologyMap}.
 * 
 * @author Phillip Kroll
 * @author Robert Mischke (changed sequenceNumber to timestamp)
 */
public final class TopologyNode implements Comparable<TopologyNode>, Cloneable, Serializable {

    private static final int INITIAL_SEQUENCE_NUMBER = -1;

    private static final long serialVersionUID = -2179209415622826744L;

    private final Date createdTime = new Date();

    private final InstanceNodeSessionId nodeId;

    private long lastSequenceNumber = INITIAL_SEQUENCE_NUMBER;

    private int lastGraphHashCode = 0;

    private boolean routing;

    private String displayName;

    private boolean isWorkflowHost;

    /**
     * The constructor.
     * 
     * @param nodeId
     */
    public TopologyNode(InstanceNodeSessionId nodeId) {
        this.nodeId = nodeId;
        this.routing = true;
    }

    public TopologyNode(InstanceNodeSessionId nodeId, boolean routing) {
        this.nodeId = nodeId;
        this.routing = routing;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Deprecated
    public boolean getIsWorkflowHost() {
        return isWorkflowHost;
    }

    @Deprecated
    public void setIsWorkflowHost(boolean isWorkflowHost) {
        this.isWorkflowHost = isWorkflowHost;
    }

    @Override
    public TopologyNode clone() {
        return new TopologyNode(getNodeIdentifier());
    }

    /**
     * @return Returns the added.
     */
    public Date getCreatedTime() {
        return createdTime;
    }

    /**
     * @return Returns the lastSequenceNumber.
     */
    public synchronized long getSequenceNumber() {
        return lastSequenceNumber;
    }

    /**
     * @return Returns the node identifier.
     */
    public InstanceNodeSessionId getNodeIdentifier() {
        return nodeId;
    }

    /**
     * @param lastSequenceNumber The lastSequenceNumber to set.
     */
    public synchronized void setLastSequenceNumber(long lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    /**
     * @return The incremented sequence number.
     */
    public synchronized long invalidateSequenceNumber() {
        // note that this timestamp-based approach relies on the local clock not jumping back to an
        // earlier time for correct distributed behavior
        long newValue = System.currentTimeMillis();
        if (newValue <= lastSequenceNumber) {
            // ensure an increase even if the timestamp did not change since the last update
            newValue = lastSequenceNumber + 1;
        }
        lastSequenceNumber = newValue;
        return lastSequenceNumber;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getNodeIdentifier().toString() + "(" + getSequenceNumber() + ")";
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TopologyNode networkNode) {
        return ((Integer) hashCode()).compareTo(networkNode.hashCode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getNodeIdentifier().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TopologyNode) {
            return obj.hashCode() == hashCode();
        } else {
            return false;
        }
    }

    /**
     * @return Returns the lastGraphChecksum.
     */
    public int getLastGraphHashCode() {
        return lastGraphHashCode;
    }

    /**
     * @param lastGraphHashCode The lastGraphHashCode to set.
     */
    public void setLastGraphHashCode(int lastGraphHashCode) {
        this.lastGraphHashCode = lastGraphHashCode;
    }

    /**
     * Is a node that performs routing.
     * 
     * @return Returns the routing.
     */
    public boolean isRouting() {
        return routing;
    }

    /**
     * Set isRouting.
     * 
     * @param routing The routing to set.
     */
    public void setRouting(boolean routing) {
        this.routing = routing;
    }
}
