/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties.spi;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.nodeproperties.NodeProperty;

/**
 * A listener for changes to the known set of distributed node properties.
 * 
 * @author Robert Mischke
 */
public interface NodePropertiesChangeListener {

    /**
     * Reports that there has been a change to connected/reachable set of {@link NodeProperty}s. This also includes changes where no
     * properties were changed, but nodes became reachable or unreachable ("connected"/"disconnected") by network topology changes.
     * 
     * Note that for performance reasons, the complete set of known node properties is not sent along with every change event. The rationale
     * is that most listeners will only track a very specific set of properties anyway, so the complete set is rarely needed, and if it is
     * needed, it is trivial to reconstruct.
     * 
     * @param addedProperties all new {@link NodeProperty}s, either because they were newly added/published by a node, or because the node
     *        declaring them became reachable
     * @param updatedProperties all new {@link NodeProperty}s that already existed, but had their value changed to a non-null value;
     *        {@link NodeProperty}s that have changed their value to "null" are treated as "removed" (see next parameter)
     * @param removedProperties all removed {@link NodeProperty}s, either because they were removed/unpublished by their node (by updating
     *        them with a "null" value), or because the node declaring them became unreachable
     */
    void onReachableNodePropertiesChanged(Collection<? extends NodeProperty>
        addedProperties, Collection<? extends NodeProperty> updatedProperties,
        Collection<? extends NodeProperty> removedProperties);

    /**
     * Convenience callback for listeners that need to keep track of all reachable nodes' full properties. Each update sends one or more new
     * property maps; each map itself is immutable, so it is safe to store it in listeners without cloning. When a node becomes unreachable,
     * its map may either be replaced by an empty map, or null. (TODO specify if needed)
     * 
     * @param updatedPropertyMaps the map of property maps that have changed
     */
    void onNodePropertyMapsOfNodesChanged(Map<InstanceNodeSessionId, Map<String, String>> updatedPropertyMaps);

    /**
     * Reports that one or more nodes that previously declared {@link NodeProperty}s have become unreachable/disconnected.
     * 
     * @param disconnectedProperties the last known {@link NodeProperty}s of the now-disconnected node(s)
     */
    // TODO reactivate once there is a use case
    // void onNodePropertiesDisconnected(Collection<? extends NodeProperty> disconnectedProperties);

    /**
     * Reports that a one or more nodes that previously declared {@link NodeProperty}s and had become unreachable/disconnected are now
     * available again.
     * 
     * Note that there is no guarantee that these properties are still up-to-date; listen for
     * {@link #onNodePropertiesAddedOrModified(Collection)} callbacks to detect related changes.
     * 
     * @param reconnectedProperties the last known {@link NodeProperty}s of the reconnected node(s)
     */
    // TODO reactivate once there is a use case
    // void onNodePropertiesReconnected(Collection<? extends NodeProperty> reconnectedProperties);
}
