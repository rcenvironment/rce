/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;

/**
 * Provides information about the execution of a workflow or component.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public interface ExecutionContext extends Serializable {

    /**
     * @return identifier of the workflow/component executed
     */
    // TODO deprecate this once a type-safe id object is available; consider moving this into subinterfaces for type safety
    String getExecutionIdentifier();

    /**
     * @return name of the workflow/component executed
     */
    String getInstanceName();

    /**
     * @return host node of the workflow/component executed
     */
    LogicalNodeId getNodeId();

    /**
     * @return the storage node location of the workflow/component being executed; intended for log/metadata generation, not for performing
     *         remote calls; use {@link #getStorageNetworkDestination()} for this instead!
     */
    LogicalNodeId getStorageNodeId();

    /**
     * @return the {@link NetworkDestination} to use for data management operations
     */
    NetworkDestination getStorageNetworkDestination();

    /**
     * @return the current {@link ServiceCallContext}; can be used to determine the caller that triggered the current method's invocation,
     *         and the {@link LogicalNodeSessionId} this component was invoked under
     */
    ServiceCallContext getServiceCallContext();
}
