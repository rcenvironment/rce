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

import de.rcenvironment.core.communication.common.NodeIdentifier;
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
    
    private NodeIdentifier nodeIdentifierStartedExecution;
    
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
    public NodeIdentifier getNodeId() {
        return workflowDescription.getControllerNode();
    }

    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
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
    public NodeIdentifier getNodeIdStartedExecution() {
        return nodeIdentifierStartedExecution;
    }

    @Override
    public String getAdditionalInformationProvidedAtStart() {
        return additionalInformation;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
    
    public void setNodeIdentifierStartedExecution(NodeIdentifier nodeIdentifier) {
        this.nodeIdentifierStartedExecution = nodeIdentifier;
    }

    public void setAdditionalInformationProvidedAtStart(String additionalInformationProvidedAtStart) {
        this.additionalInformation = additionalInformationProvidedAtStart;
    }
}
