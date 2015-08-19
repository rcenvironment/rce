/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.impl;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;

/**
 * Implementation of {@link ComponentExecutionInformation}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionInformationImpl implements ComponentExecutionInformation {

    private static final long serialVersionUID = -35637831085899098L;

    private String identifier;
    
    private String instanceName;
    
    private NodeIdentifier nodeId;
    
    private String componentIdentifier;
    
    private NodeIdentifier defaultStorageNodeId;
    
    private String workflowInstanceName;
    
    private String workflowExecutionIdentifier;
    
    private NodeIdentifier workflowNodeId;
    
    public ComponentExecutionInformationImpl() {}
    
    public ComponentExecutionInformationImpl(ComponentExecutionContext compExeCtx) {
        identifier = compExeCtx.getExecutionIdentifier();
        instanceName = compExeCtx.getInstanceName();
        nodeId = compExeCtx.getNodeId();
        componentIdentifier = compExeCtx.getComponentDescription().getIdentifier();
        defaultStorageNodeId = compExeCtx.getDefaultStorageNodeId();
        workflowInstanceName = compExeCtx.getWorkflowInstanceName();
        workflowExecutionIdentifier = compExeCtx.getWorkflowExecutionIdentifier();
        workflowNodeId = compExeCtx.getWorkflowNodeId();
    }
    
    @Override
    public String getExecutionIdentifier() {
        return identifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public NodeIdentifier getNodeId() {
        return nodeId;
    }
    
    @Override
    public String getComponentIdentifier() {
        return componentIdentifier;
    }
    
    public void setNodeId(NodeIdentifier nodeId) {
        this.nodeId = nodeId;
    }
    
    public void setIdentifier(String executionIdentifier) {
        this.identifier = executionIdentifier;
    }
    
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setComponentIdentifier(String componentIdentifier) {
        this.componentIdentifier = componentIdentifier;
    }

    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
        return defaultStorageNodeId;
    }
    
    public void setDefaultStorageNodeId(NodeIdentifier defaultStorageNodeId) {
        this.defaultStorageNodeId = defaultStorageNodeId;
    }

    @Override
    public String getWorkflowInstanceName() {
        return workflowInstanceName;
    }

    public void setWorkflowInstanceName(String workflowInstanceName) {
        this.workflowInstanceName = workflowInstanceName;
    }
    
    @Override
    public String getWorkflowExecutionIdentifier() {
        return workflowExecutionIdentifier;
    }
    
    public void setWorkflowExecutionIdentifier(String workflowExecutionIdentifier) {
        this.workflowExecutionIdentifier = workflowExecutionIdentifier;
    }

    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(NodeIdentifier workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }
    
}
