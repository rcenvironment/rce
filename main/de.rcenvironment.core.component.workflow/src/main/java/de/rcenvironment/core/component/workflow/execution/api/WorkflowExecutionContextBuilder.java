/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.api;

import java.util.UUID;

import de.rcenvironment.core.communication.common.NodeIdentifier;
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
     * @param nodeIdentifier {@link NodeIdentifier} of node the workflow was started
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setNodeIdentifierStartedExecution(NodeIdentifier nodeIdentifier) {
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
