/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.internal;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Tree representation of a "connection" between two {@link NetworkGraphNode}s. The exact semantics are up to the content provider.
 * 
 * @author Robert Mischke
 */
public final class NetworkGraphLinkImpl implements NetworkGraphLink {

    private final String linkId;

    private final NodeIdentifier source;

    private final NodeIdentifier target;

    public NetworkGraphLinkImpl(String linkId, NodeIdentifier source, NodeIdentifier target) {
        if (linkId == null || source == null || target == null) {
            throw new NullPointerException(String.format("%s / %s / %s", linkId, source, target));
        }
        this.linkId = linkId;
        this.source = source;
        this.target = target;
    }

    public String getLinkId() {
        return linkId;
    }

    @Override
    public NodeIdentifier getSourceNodeId() {
        return source;
    }

    @Override
    public NodeIdentifier getTargetNodeId() {
        return target;
    }

    @Override
    public String toString() {
        return String.format("%s (%s->%s)", linkId, source, target);
    }

}
