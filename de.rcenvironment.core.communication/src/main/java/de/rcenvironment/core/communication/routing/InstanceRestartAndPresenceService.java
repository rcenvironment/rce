/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Defines methods for detecting instance/node restarts and presence.
 *
 * @author Robert Mischke
 */
public interface InstanceRestartAndPresenceService {

    /**
     * Determines the state of a given {@link InstanceNodeSessionId}; see {@link InstanceSessionNetworkStatus.State} for possible values.
     * 
     * @param lookupId the id to query
     * @return a wrapper object representing the {@link InstanceSessionNetworkStatus.State} with additional id information
     */
    InstanceSessionNetworkStatus queryInstanceSessionNetworkStatus(InstanceNodeSessionId lookupId);

}
