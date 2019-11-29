/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.UUID;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * Creates {@link WorkflowExecutionContext} objects.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionContextBuilder {

    private WorkflowExecutionContextImpl wfExeCtx;
    
    public WorkflowExecutionContextBuilder(WorkflowDescription workflowDescription) {
        wfExeCtx = new WorkflowExecutionContextImpl(UUID.randomUUID().toString(), workflowDescription);
    }
    
    /**
     * @param instanceName name of the workflow executed
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setInstanceName(String instanceName) {
        wfExeCtx.setInstanceName(instanceName);
        return this;
    }
    
    /**
     * @param nodeIdentifier {@link InstanceNodeSessionId} of node the workflow was started
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setNodeIdentifierStartedExecution(LogicalNodeId nodeIdentifier) {
        wfExeCtx.setNodeIdentifierStartedExecution(nodeIdentifier);
        return this;
    }
    
    /**
     * @param additionalInformation additional information provided at start
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setAdditionalInformationProvidedAtStart(String additionalInformation) {
        wfExeCtx.setAdditionalInformationProvidedAtStart(additionalInformation);
        return this;
    }
    
    
    /**
     * @return {@link WorkflowExecutionContext} instance
     */
    public WorkflowExecutionContext build() {
        return wfExeCtx;
    }
    
}
