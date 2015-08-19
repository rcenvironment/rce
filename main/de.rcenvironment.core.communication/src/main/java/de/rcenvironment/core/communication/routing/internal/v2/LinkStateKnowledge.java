/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;

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

    private final Map<NodeIdentifier, LinkState> linkStates;

    public LinkStateKnowledge() {
        linkStates = Collections.unmodifiableMap(new HashMap<NodeIdentifier, LinkState>());
    }

    public LinkStateKnowledge(Map<NodeIdentifier, LinkState> newLinkStates) {
        linkStates = Collections.unmodifiableMap(new HashMap<NodeIdentifier, LinkState>(newLinkStates));
    }

    public Map<NodeIdentifier, LinkState> getLinkStates() {
        return linkStates;
    }

}
