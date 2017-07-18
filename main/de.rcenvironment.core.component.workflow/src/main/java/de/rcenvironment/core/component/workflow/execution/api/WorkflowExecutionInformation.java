/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.api.ExecutionInformation;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;


/**
 * Workflow specific extension of {@link ExecutionInformation}.
 * 
 * @author Doreen Seider
 * 
 * Note: Extended on demand. I missed from time to time a review which information must be provided to whom and which must not. --seid_do
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
     * @param identifier execution identifier of the component instance to get information for
     * @return {@link ComponentExecutionInformation} for the component instance
     */
    ComponentExecutionInformation getComponentExecutionInformation(String identifier);
    
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
    
}
