/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import de.rcenvironment.core.communication.transport.spi.MessageChannel;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Represents a "link" in the Link State Routing terminology. Internally, this is equivalent to an
 * established outgoing {@link MessageChannel}. This class is immutable.
 * 
 * @author Robert Mischke
 */
public final class Link {

    private final String linkId;

    private final String nodeId; // an InstanceNodeSessionId

    /**
     * @param linkId the value of {@link MessageChannel#getChannelId()}; named "link id" to match
     *        the LSA concept
     * @param nodeIdString the {@link InstanceNodeSessionId} string of the destination node
     */
    public Link(String linkId, String nodeIdString) {
        this.linkId = linkId;
        this.nodeId = nodeIdString;
    }

    /**
     * @return the value of {@link MessageChannel#getChannelId()}; named "link id" to match the LSA
     *         concept
     */
    public String getLinkId() {
        return linkId;
    }

    public String getNodeIdString() {
        return nodeId;
    }

    @Override
    public String toString() {
        return StringUtils.format("Link(%s)->%s", linkId, nodeId);
    }
}
