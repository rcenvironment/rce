/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;

/**
 * Component specific extension of {@link ExecutionInformation}.
 * 
 * @author Doreen Seider
 */
public interface ComponentExecutionInformation extends ExecutionInformation {

    /**
     * @return identifier of the component
     */
    String getComponentIdentifier();

    /**
     * @return name of workflow instance
     */
    String getWorkflowInstanceName();

    /**
     * @return execution identifier of workflow instance
     */
    String getWorkflowExecutionIdentifier();

    /**
     * @return get {@link InstanceNodeSessionId} of workflow instance
     */
    LogicalNodeId getWorkflowNodeId();
}
