/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.session.api;

import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;

/**
 * Utility class for handling destination ids. These methods are meant to be used on the "tool publisher side".
 * Currently, we use the node id for the internal network as part of the destination id. 
 * When this is changed later, this class is the place to make these changes.
 *
 * @author Brigitte Boden
 */
public final class DestinationIdUtils {
    
    private DestinationIdUtils() {}
    
    /**
     *  Currently, we use the node id for the internal network as part of the destination id. 
     * When this is changed later, this is the place to make these changes.
     * 
     * @param nodeId the node id in the internal RCE network
     * @return the internal destination id (without destination id prefix)
     */
    public static String getInternalDestinationIdForLogicalNodeId(String nodeId) {
        return nodeId;
    }
    
    /**
     * Returns the node id (in the internal RCE network) for a given qualifiedDestinationId and destinationIdPrefix.
     * 
     * @param qualifiedDestinationId the qualified destination id
     * @return the logical node id in the internal network
     */
    public static String getNodeIdFromQualifiedDestinationId(String qualifiedDestinationId) {
        return qualifiedDestinationId.substring(UplinkProtocolConstants.DESTINATION_ID_PREFIX_LENGTH);
    }
    
    /**
     * Creates a qualified destination id for a given destinationIdPrefix and a given internalDestinationId.
     * 
     * @param destinationIdPrefix the prefix
     * @param internalDestinationId the internal id
     * @return the qualified destination id
     */
    public static String getQualifiedDestinationId(String destinationIdPrefix, String internalDestinationId) {
        return destinationIdPrefix + internalDestinationId;
    }
}
