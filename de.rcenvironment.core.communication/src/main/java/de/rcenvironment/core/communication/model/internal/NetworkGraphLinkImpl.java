/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.communication.model.internal;

import de.rcenvironment.core.communication.common.NetworkGraphLink;
import de.rcenvironment.core.communication.common.NetworkGraphNode;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Tree representation of a "connection" between two {@link NetworkGraphNode}s. The exact semantics are up to the content provider.
 * 
 * @author Robert Mischke
 */
public final class NetworkGraphLinkImpl implements NetworkGraphLink {

    private final String linkId;

    private final InstanceNodeSessionId source;

    private final InstanceNodeSessionId target;

    public NetworkGraphLinkImpl(String linkId, InstanceNodeSessionId source, InstanceNodeSessionId target) {
        if (linkId == null || source == null || target == null) {
            throw new NullPointerException(StringUtils.format("%s / %s / %s", linkId, source, target));
        }
        this.linkId = linkId;
        this.source = source;
        this.target = target;
    }

    @Override
    public String getLinkId() {
        return linkId;
    }

    @Override
    public InstanceNodeSessionId getSourceNodeId() {
        return source;
    }

    @Override
    public InstanceNodeSessionId getTargetNodeId() {
        return target;
    }

    @Override
    public String toString() {
        return StringUtils.format("%s (%s->%s)", linkId, source, target);
    }

}
