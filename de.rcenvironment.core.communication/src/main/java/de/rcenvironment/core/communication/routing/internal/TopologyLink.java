/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.Date;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents a logical link between two communicating RCE instances in a network. It is similar to a {@link MessageChannel}, but can
 * connect arbitrary nodes (while a {@link MessageChannel} always starts at the local node). It is used as a directed edge in the graph
 * representing the known network topology (see {@link TopologyMap}).
 * 
 * @author Phillip Kroll
 * @author Robert Mischke
 */
public final class TopologyLink implements Comparable<TopologyLink>, Cloneable, Serializable {

    private static final long serialVersionUID = -5377050931490338202L;

    private final Date creationTime = new Date();

    private final InstanceNodeSessionId source;

    private final InstanceNodeSessionId destination;

    private int weight;

    private int reliability;

    private String connectionId;

    private final String linkIdentity;

    public TopologyLink(InstanceNodeSessionId source, InstanceNodeSessionId destination, String connectionId) {
        this.source = source;
        this.destination = destination;
        // generate a link id that should be globally unique
        this.linkIdentity = source.getInstanceNodeSessionIdString() + destination.getInstanceNodeSessionIdString() + connectionId;
        this.connectionId = connectionId;
        this.reliability = 0;
        this.weight = 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return StringUtils.format("%s --[ConId=%3$s]--> %s, (Hash=%s)", getSource(), getDestination(), getConnectionId(), hashCode());
    }

    @Override
    public TopologyLink clone() {
        // TODO check - is this actually needed anymore? couldn't the class just be made immutable? - misc_ro
        return new TopologyLink(getSource(), getDestination(), connectionId);
    }

    /**
     * @return Returns the creationTime.
     */
    public Date getCreationTime() {
        return creationTime;
    }

    public String getConnectionId() {
        return connectionId;
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
     * @return Returns the weight.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight The weight to set.
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * @return Returns the reliability.
     */
    public int getReliability() {
        return reliability;
    }

    /**
     * Increase reliability.
     * 
     */
    public void incReliability() {
        reliability = reliability + 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TopologyLink otherLink) {
        return linkIdentity.compareTo(otherLink.linkIdentity);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return linkIdentity.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TopologyLink) {
            return ((TopologyLink) obj).linkIdentity.equals(linkIdentity);
        } else {
            return false;
        }
    }

}
