/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.ExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Workflow-specific {@link ExecutionContext}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionContext extends ExecutionContext {

    /**
     * @return {@link WorkflowDescription} of the workflow executed
     */
    WorkflowDescription getWorkflowDescription();

    /**
     * @param wfNode workflow node of the component within the {@link WorkflowDescription}
     * @return execution identifier of the component with the given workflow node within the {@link WorkflowDescription}
     */
    ComponentExecutionIdentifier getCompExeIdByWfNode(WorkflowNode wfNode);

    /**
     * @return {@link InstanceNodeSessionId} of the instance the execution was started from
     */
    LogicalNodeId getNodeIdStartedExecution();

    /**
     * @return additional information optionally provided at workflow start
     */
    String getAdditionalInformationProvidedAtStart();

    /**
     * @return the {@link WorkflowExecutionHandle} object that references this workflow uniquely
     */
    WorkflowExecutionHandle getWorkflowExecutionHandle();

}
