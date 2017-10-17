/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

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
     * @return default storage node of the workflow/component executed
     */
    LogicalNodeId getDefaultStorageNodeId();

    /**
     * @return the current {@link ServiceCallContext}; can be used to determine the caller that triggered the current method's invocation,
     *         and the {@link LogicalNodeSessionId} this component was invoked under
     */
    ServiceCallContext getServiceCallContext();
}
