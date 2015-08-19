/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.nodeproperties;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.messaging.NetworkRequestHandlerMap;
import de.rcenvironment.core.communication.nodeproperties.spi.RawNodePropertiesChangeListener;

/**
 * A service for managing distributed node properties. Each node holds a key-value map that can be filled by other local services. This data
 * is then synchronized to all reachable nodes, where other services can access it.
 * 
 * @author Robert Mischke
 */
public interface NodePropertiesService {

    /**
     * Adds or updates a single local key-value property. Calling this method may trigger distribution of this datum to remote nodes; when
     * adding more than one entries, using {@link #addOrUpdateLocalNodeProperties(Map)} is more efficient.
     * 
     * @param key the key
     * @param value the value
     */
    void addOrUpdateLocalNodeProperty(String key, String value);

    /**
     * Adds and/or updates a set of local key-value properties. Calling this method may trigger distribution of these entries to remote
     * nodes.
     * 
     * @param data the map of entries to add and/or update
     */
    void addOrUpdateLocalNodeProperties(Map<String, String> data);

    /**
     * Returns the full property map for a single node. Modifications to the map do not cause any side effects.
     * 
     * @param nodeId the id of the target node
     * @return the property map
     */
    Map<String, String> getNodeProperties(NodeIdentifier nodeId);

    /**
     * Returns the full property map for all node that occurred in property updates.
     * 
     * @return the map of property maps as returned by {@link #getNodeProperties(NodeIdentifier)}
     */
    Map<NodeIdentifier, Map<String, String>> getAllNodeProperties();

    /**
     * Returns the full property map for the given set of nodes.
     * 
     * @param nodeIds the ids of the relevant nodes
     * @return the map of property maps as returned by {@link #getNodeProperties(NodeIdentifier)}
     */
    Map<NodeIdentifier, Map<String, String>> getAllNodeProperties(Collection<NodeIdentifier> nodeIds);

    /**
     * Adds a listener for local or remote node property changes.
     * 
     * @param listener the new listener
     */
    // TODO add filtering options if necessary
    void addRawNodePropertiesChangeListener(RawNodePropertiesChangeListener listener);

    /**
     * Removes a listener for local or remote node property changes.
     * 
     * @param listener the listener to remove
     */
    void removeRawNodePropertiesChangeListener(RawNodePropertiesChangeListener listener);

    /**
     * @return the {@link NetworkRequestHandler}s this service needs to register
     */
    NetworkRequestHandlerMap getNetworkRequestHandlers();

}
