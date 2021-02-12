/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.InstanceNodeId;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * Configuration management service for the local platform instance.
 * 
 * TODO (p2) 10.1+ rename/refactor
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface PlatformService {

    /**
     * Returns information about this platform's identity, including a persistent unique identifier, and optionally, a public key and an
     * end-user display name for this platform.
     * 
     * @return an immutable identity information object
     */
    // disabled for 3.0.0 migration - misc_ro, June 2013
    // NodeIdentityInformation getIdentityInformation();

    /**
     * @return the persistent {@link InstanceNodeId} of the local instance
     */
    InstanceNodeId getLocalInstanceNodeId();

    /**
     * @return the current {@link InstanceNodeSessionId} of the local instance
     */
    InstanceNodeSessionId getLocalInstanceNodeSessionId();

    /**
     * @return the default {@link LogicalNodeId} for the local instance
     */
    LogicalNodeId getLocalDefaultLogicalNodeId();

    /**
     * @return the default {@link LogicalNodeSessionId} for the local instance
     */
    LogicalNodeSessionId getLocalDefaultLogicalNodeSessionId();

    /**
     * Checks if the specified {@link InstanceNodeSessionId} represents the local node.
     * 
     * TODO >=8.0.0 - this is potentially unsafe in case of instance id collisions; check if this can be an actual problem
     * 
     * @param nodeId the {@link InstanceNodeSessionId} to verify
     * @return true if the given {@link InstanceNodeSessionId} matches the local node's id
     */
    boolean matchesLocalInstance(ResolvableNodeId nodeId);

}
