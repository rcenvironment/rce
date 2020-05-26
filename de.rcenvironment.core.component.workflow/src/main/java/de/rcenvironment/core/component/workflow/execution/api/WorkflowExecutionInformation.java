/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;

/**
 * Workflow specific extension of {@link ExecutionInformation}.
 * 
 * Note: Extended on demand. I missed from time to time a review which information must be provided to whom and which must not. --seid_do
 * 
 * @author Doreen Seider
 * 
 */
public interface WorkflowExecutionInformation extends ExecutionInformation, Comparable<WorkflowExecutionInformation> {

    /**
     * @return time the execution was started
     */
    long getStartTime();

    /**
     * @return the underlying {@link WorkflowDescription}
     */
    WorkflowDescription getWorkflowDescription();

    /**
     * @return the {@link ExecutionHandle} that identifies this workflow uniquely
     */
    WorkflowExecutionHandle getWorkflowExecutionHandle();

    /**
     * @param identifier execution identifier of the component instance to get information for
     * @return {@link ComponentExecutionInformation} for the component instance
     */
    ComponentExecutionInformation getComponentExecutionInformation(WorkflowNodeIdentifier identifier);

    /**
     * @return {@link InstanceNodeSessionId} of the instance the execution was started from
     */
    LogicalNodeId getNodeIdStartedExecution();

    /**
     * @return additional information optionally provided at workflow start
     */
    String getAdditionalInformationProvidedAtStart();

    /**
     * Gets current workflow state. The {@link WorkflowState} might be out-dated at the time this method is called. It is snapshot at the
     * time, the {@link WorkflowExecutionInformation} instance was requested.
     * 
     * @return {@link WorkflowState} (might be out-dated)
     */
    WorkflowState getWorkflowState();

    /**
     * @return identifier the workflow is stored under in the data management, <code>null</code> if no data management entry exists for the
     *         workflow (yet)
     */
    Long getWorkflowDataManagementId();

    /**
     * @return Collection of {@link ComponentExecutionInformation} for all component instances in the current workflow
     */

    Collection<ComponentExecutionInformation> getComponentExecutionInformations();

}
