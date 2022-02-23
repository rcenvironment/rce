/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;

/**
 * Local service for dispatching {@link EndpointDatum}s.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface EndpointDatumDispatchService {

    /**
     * Dispatches {@link EndpointDatum}s asynchronously but ordered.
     * 
     * @param endpointDatum {@link EndpointDatum} to dispatch
     */
    void dispatchEndpointDatum(EndpointDatum endpointDatum);

    /**
     * If two component controllers that are connected within a workflow are not network visible to each other, any {@link EndpointDatum} is
     * sent to the workflow controller instead. This map is registered on workflow execution creation to provide connections from the
     * workflow controller to the actual {@link EndpointDatum} destinations.
     * 
     * @param workflowExecutionId the id of the workflow execution to register the mapping for
     * @param destinationMap the map of {@link NetworkDestination}s to use for any component controller id that should receive the
     *        {@link EndpointDatum}
     */
    void registerComponentControllerForwardingMap(String workflowExecutionId, ComponentControllerRoutingMap destinationMap);

    /**
     * Unregisters a map previously set using {@link #registerComponentControllerForwardingMap()}; typically called during workflow
     * disposal.
     * 
     * @param workflowExecutionId the id of the workflow being disposed
     */
    void unregisterComponentControllerForwardingMap(String workflowExecutionId);
}
