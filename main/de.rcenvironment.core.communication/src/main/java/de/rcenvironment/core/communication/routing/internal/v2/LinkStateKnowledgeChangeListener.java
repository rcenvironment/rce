/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Listener for {@link LinkState} knowledge changes.
 * 
 * @author Robert Mischke
 */
public interface LinkStateKnowledgeChangeListener {

    /**
     * Called when the accumulated knowledge of {@link LinkState}s is modified. The map and the contained {@link LinkState} objects are
     * immutable, so all access is thread-safe.
     * 
     * This event is also fired for each new listener on subscription.
     * 
     * @param knowledge the map of all known {@link LinkState}s
     */
    void onLinkStateKnowledgeChanged(Map<NodeIdentifier, LinkState> knowledge);

    /**
     * Called when {@link LinkState}s are added or updated. The map and the contained {@link LinkState} objects are immutable, so all access
     * is thread-safe.
     * 
     * @param delta the map of added or updated {@link LinkState}s
     */
    void onLinkStatesUpdated(Map<NodeIdentifier, LinkState> delta);

    /**
     * Reports that the local node's {@link LinkState} has been updated.
     * 
     * @param linkState the new local {@link LinkState}
     */
    void onLocalLinkStateUpdated(LinkState linkState);
}
