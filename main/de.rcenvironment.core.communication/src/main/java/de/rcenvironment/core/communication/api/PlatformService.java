/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Configuration management service for the local platform instance.
 * 
 * TODO merge into {@link CommunicationService}? - misc_ro
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface PlatformService {

    /**
     * Returns information about this platform's identity, including a persistent unique identifier,
     * and optionally, a public key and an end-user display name for this platform.
     * 
     * @return an immutable identity information object
     */
    // disabled for 3.0.0 migration - misc_ro, June 2013
    // NodeIdentityInformation getIdentityInformation();

    /**
     * Returns the identifier of the local RCE platform.
     * 
     * @return the identifier of the local RCE platform.
     */
    NodeIdentifier getLocalNodeId();

    /**
     * Checks if the specified {@link NodeIdentifier} represents the local node.
     * 
     * @param nodeId the {@link NodeIdentifier} to verify
     * @return true if the given {@link NodeIdentifier} matches the local node's id
     */
    boolean isLocalNode(NodeIdentifier nodeId);
}
