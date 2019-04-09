/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NetworkDestination;

/**
 * Synchronized and type-safe container for {@link NetworkDestination}s that should be used for contacting specific workflow component
 * controllers.
 *
 * @author Robert Mischke
 */
public class ComponentControllerRoutingMap {

    private final Map<String, NetworkDestination> componentControllerDestinationsByCompExecId = new HashMap<>();

    /**
     * Retrieves a {@link NetworkDestination}.
     * 
     * Note: Once the calling code is properly migrated to use {@link ComponentExecutionIdentifier} objects, this method could be changed to
     * use these instead of id strings.
     * 
     * @param compExecId the id string of the {@link ComponentExecutionIdentifier}'s to look up
     * @return the {@link NetworkDestination}, or null if none was found
     */
    public synchronized NetworkDestination getNetworkDestinationForComponentController(String compExecId) {
        return componentControllerDestinationsByCompExecId.get(compExecId);
    }

    /**
     * Sets a {@link NetworkDestination}.
     * 
     * Note: Once the calling code is properly migrated to use {@link ComponentExecutionIdentifier} objects, this method could be changed to
     * use these instead of id strings.
     * 
     * @param compExecId the id string of the {@link ComponentExecutionIdentifier}'s to set the {@link NetworkDestination} for
     * @param dest the {@link NetworkDestination} to set
     */
    public synchronized void setNetworkDestinationForComponentController(String compExecId, NetworkDestination dest) {
        componentControllerDestinationsByCompExecId.put(compExecId, dest);
    }
}
