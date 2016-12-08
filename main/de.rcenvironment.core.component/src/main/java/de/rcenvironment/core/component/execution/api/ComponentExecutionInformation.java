/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
