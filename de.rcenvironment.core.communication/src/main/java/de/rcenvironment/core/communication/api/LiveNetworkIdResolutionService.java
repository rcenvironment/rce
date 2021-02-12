/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.CommonIdBase;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * A service for resolving received or stored node/network ids of any type to specific ids in the currently known and reachable network.
 * 
 * @author Robert Mischke
 */
public interface LiveNetworkIdResolutionService {

    /**
     * Returns the most appropriate {@link InstanceNodeSessionId} for the given string, which can be either an instance id (as, for example,
     * returned by {@link CommonIdBase#getInstanceNodeIdString()}), or an instance session id (as, for example, returned by
     * {@link InstanceNodeSessionId#getInstanceNodeSessionIdString()}.
     * 
     * @param input the string to resolve
     * @return the resolved {@link InstanceNodeSessionId}
     * @throws IdentifierException if no appropriate match exists
     */
    InstanceNodeSessionId resolveInstanceNodeIdStringToInstanceNodeSessionId(String input) throws IdentifierException;

    /**
     * Resolves/converts the given id to the best-matching {@link LogicalNodeSessionId} available. If no match exists, an
     * {@link IdentifierException} is thrown.
     * 
     * @param id the id to resolve
     * @return the {@link LogicalNodeSessionId} on success
     * @throws IdentifierException if no appropriate match exists
     */
    LogicalNodeSessionId resolveToLogicalNodeSessionId(ResolvableNodeId id) throws IdentifierException;
}
