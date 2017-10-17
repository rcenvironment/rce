/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Implementation of {@link WorkflowExecutionContext}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionContextImpl implements WorkflowExecutionContext {

    private static final long serialVersionUID = 238066231055021678L;

    private String executionIdentifier;
    
    private String instanceName;
    
    private WorkflowDescription workflowDescription;
    
    private Map<String, String> componentExecutionIdentifiers;
    
    private LogicalNodeId nodeIdentifierStartedExecution;
    
    private String additionalInformation;
    
    public WorkflowExecutionContextImpl(String executionIdentifier, WorkflowDescription workflowDescription) {
        this.executionIdentifier = executionIdentifier;
        this.workflowDescription = workflowDescription;
        componentExecutionIdentifiers = new HashMap<>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            componentExecutionIdentifiers.put(wfNode.getIdentifier(), UUID.randomUUID().toString());
        }
    }
    
    @Override
    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public LogicalNodeId getNodeId() {
        return workflowDescription.getControllerNode();
    }

    @Override
    public LogicalNodeId getDefaultStorageNodeId() {
        return getNodeId();
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    @Override
    public String getCompExeIdByWfNodeId(String wfNodeId) {
        return componentExecutionIdentifiers.get(wfNodeId);
    }

    @Override
    public LogicalNodeId getNodeIdStartedExecution() {
        return nodeIdentifierStartedExecution;
    }

    @Override
    public String getAdditionalInformationProvidedAtStart() {
        return additionalInformation;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    
    public void setNodeIdentifierStartedExecution(LogicalNodeId nodeIdentifier) {
        this.nodeIdentifierStartedExecution = nodeIdentifier;
    }

    public void setAdditionalInformationProvidedAtStart(String additionalInformationProvidedAtStart) {
        this.additionalInformation = additionalInformationProvidedAtStart;
    }

    @Override
    public ServiceCallContext getServiceCallContext() {
        return null; // implement this once needed
    }
}
