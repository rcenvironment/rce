/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Represents the local knowledge about published {@link LinkState}s of other nodes.
 * 
 * Note that this class is immutable.
 * 
 * @author Robert Mischke
 */
@Deprecated
// TODO unless a new use for this class is found, delete it for 4.0
public final class LinkStateKnowledge {

    private final Map<InstanceNodeSessionId, LinkState> linkStates;

    public LinkStateKnowledge() {
        linkStates = Collections.unmodifiableMap(new HashMap<InstanceNodeSessionId, LinkState>());
    }

    public LinkStateKnowledge(Map<InstanceNodeSessionId, LinkState> newLinkStates) {
        linkStates = Collections.unmodifiableMap(new HashMap<InstanceNodeSessionId, LinkState>(newLinkStates));
    }

    public Map<InstanceNodeSessionId, LinkState> getLinkStates() {
        return linkStates;
    }

}
