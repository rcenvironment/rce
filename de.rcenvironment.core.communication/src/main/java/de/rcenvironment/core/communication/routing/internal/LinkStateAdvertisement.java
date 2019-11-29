/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Encapsulates information about a network node and its direct neighborhood. This is called a link state advertisement and is supposed to
 * be passed across the network.
 * 
 * @see http://de.wikipedia.org/wiki/Link_State_Advertisement
 * @author Phillip Kroll
 */
public final class LinkStateAdvertisement implements Serializable, Cloneable {

    /**
     * Startup LSA string.
     */
    public static final String REASON_STARTUP = "startup";

    /**
     * Shutdown LSA string.
     */
    public static final String REASON_SHUTDOWN = "shutdown";

    /**
     * Update LSA string.
     */
    public static final String REASON_UPDATE = "update";

    private static final long serialVersionUID = -6872824009925812913L;

    private final Collection<TopologyLink> links;

    private final InstanceNodeSessionId owner;

    // TODO temporarily transported on top of the LSA infrastructure
    private String displayName;

    // TODO temporarily transported on top of the LSA infrastructure
    private boolean isWorkflowHost;

    private final long sequenceNumber;

    private final int graphHashCode;

    private final boolean routing;

    private final String reason;

    /**
     * The constructor.
     */
    public LinkStateAdvertisement(InstanceNodeSessionId theOwner, String displayName, boolean isWorkflowHost, long sequenceNumber,
        int graphChecksum,
        boolean routing, String reason, Collection<TopologyLink> links) {
        this.owner = theOwner;
        this.displayName = displayName;
        this.isWorkflowHost = isWorkflowHost;
        this.sequenceNumber = sequenceNumber;
        this.links = new HashSet<TopologyLink>(links);
        this.graphHashCode = graphChecksum;
        this.routing = routing;
        this.reason = reason;
    }

    /**
     * Create an update LSA.
     * 
     * @param theOwner The owner
     * @param sequenceNumber The sequence number
     * @param graphChecksum The graph checksum
     * @param routing The routing
     * @param links The links
     * @param displayName The display name
     * @param isWorkflowHost Is workflow host
     * @return The update LSA.
     */
    public static LinkStateAdvertisement createUpdateLsa(InstanceNodeSessionId theOwner, String displayName, boolean isWorkflowHost,
        long sequenceNumber,
        int graphChecksum, boolean routing, Collection<TopologyLink> links) {
        return new LinkStateAdvertisement(theOwner, displayName, isWorkflowHost, sequenceNumber, graphChecksum, routing, REASON_UPDATE,
            links);
    }

    /**
     * Create startup LSA.
     * 
     * @param theOwner The owner
     * @param routing The routing
     * @param links The links
     * @param displayName The display name
     * @param isWorkflowHost Is workflow host
     * @param sequenceNumber The sequence number
     * @return The startup LSA
     */
    public static LinkStateAdvertisement createStartUpLsa(InstanceNodeSessionId theOwner, String displayName, boolean isWorkflowHost,
        long sequenceNumber, boolean routing,
        Collection<TopologyLink> links) {
        return new LinkStateAdvertisement(theOwner, displayName, isWorkflowHost, 1, 0, routing, REASON_STARTUP, links);
    }

    /**
     * Create shut down LSA.
     * 
     * @param theOwner The owner
     * @param sequenceNumber The sequnce number
     * @param displayName The display name.
     * @param isWorkflowHost Is worfklow host.
     * @return The shut down LSA.
     */
    public static LinkStateAdvertisement createShutDownLsa(InstanceNodeSessionId theOwner, String displayName, boolean isWorkflowHost,
        long sequenceNumber) {
        return new LinkStateAdvertisement(theOwner, displayName, isWorkflowHost, sequenceNumber, 0, false, REASON_SHUTDOWN,
            new ArrayList<TopologyLink>());
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public LinkStateAdvertisement clone() {
        List<TopologyLink> copiedLinks = new ArrayList<TopologyLink>();
        for (TopologyLink link : this.links) {
            copiedLinks.add(link.clone());
        }
        LinkStateAdvertisement clone = new LinkStateAdvertisement(
            owner, displayName, isWorkflowHost, sequenceNumber, graphHashCode, routing, reason, copiedLinks);
        return clone;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String result = StringUtils.format("LinkStateAdvertisement(%s): %s [", getSequenceNumber(), getOwner());
        for (TopologyLink link : links) {
            result = StringUtils.format("%s, %s", result, link);
        }
        return StringUtils.format("%s]", result);
    }

    /**
     * @return Returns the networkNodes.
     */
    public Collection<TopologyLink> getLinks() {
        return links;
    }

    /**
     * @return Returns the owner.
     */
    public InstanceNodeSessionId getOwner() {
        return owner;
    }

    /**
     * @return Returns the displayName.
     */
    public String getDisplayName() {
        return displayName;
    }

    public boolean getIsWorkflowHost() {
        return isWorkflowHost;
    }

    /**
     * @return Returns the sequenceNumber.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * @return Returns the graphHashCode.
     */
    public int getGraphHashCode() {
        return graphHashCode;
    }

    /**
     * @return Returns the routing.
     */
    public boolean isRouting() {
        return routing;
    }

    /**
     * @return Returns the reason.
     */
    public String getReason() {
        return reason;
    }
}
