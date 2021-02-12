/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * A service for managing the logical nodes representing destinations reachable by uplink connections.
 *
 * @author Brigitte Boden
 */
public interface UplinkLogicalNodeMappingService {

    /**
     * Gets the id of the logical node which represents the given destination.
     * 
     * @param destinationId the qualified destination id
     * @param announcedDisplayName TODO (p1) 11.0: review display name handling
     * @return the logical node id for the associated logical node, or null if no node exists yet for the given parameters,
     */
    LogicalNodeId getLocalLogicalNodeIdForDestinationIdAndUpdateName(String destinationId, String announcedDisplayName);

    /**
     * Gets the id of the logical node which represents the given destination. If no node exists yet for the given parameters, a new node is
     * created.
     * 
     * @param destinationId the qualified destination id
     * @param announcedDisplayName the display name announced by the publisher
     * @return the logical node id for the associated logical node.
     */
    LogicalNodeId createOrGetLocalLogicalNodeIdForDestinationId(String destinationId, String announcedDisplayName);

    /**
     * Gets the qualified destination id for a given logicalNodeId.
     * 
     * @param logicalNodeId the logical node id.
     * @return The id of the associated logical node, or null, if no mappped logical node with this id exists.
     */
    String getDestinationIdForLogicalNodeId(LogicalNodeId logicalNodeId);

    /**
     * Gets the qualified destination id for a given logicalNodeId string.
     * 
     * @param logicalNodeId the logical node id.
     * @return The id of the associated logical node, or null, if no mappped logical node with this id exists.
     * 
     */
    String getDestinationIdForLogicalNodeId(String logicalNodeId);

}
