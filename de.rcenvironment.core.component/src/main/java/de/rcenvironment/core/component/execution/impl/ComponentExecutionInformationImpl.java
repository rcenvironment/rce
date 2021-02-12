/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.impl;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;

/**
 * Implementation of {@link ComponentExecutionInformation}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ComponentExecutionInformationImpl implements ComponentExecutionInformation {

    private static final long serialVersionUID = -35637831085899098L;

    private String identifier;

    private String instanceName;

    private LogicalNodeId nodeId;

    private String componentIdentifier;

    private NetworkDestination storageNetworkDestination;

    private String workflowInstanceName;

    private String workflowExecutionIdentifier;

    private LogicalNodeId workflowNodeId;

    public ComponentExecutionInformationImpl() {}

    public ComponentExecutionInformationImpl(ComponentExecutionContext compExeCtx) {
        identifier = compExeCtx.getExecutionIdentifier();
        instanceName = compExeCtx.getInstanceName();
        nodeId = compExeCtx.getNodeId();
        componentIdentifier = compExeCtx.getComponentDescription().getIdentifier();
        storageNetworkDestination = compExeCtx.getStorageNetworkDestination();
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
    public LogicalNodeId getNodeId() {
        return nodeId;
    }

    @Override
    public String getComponentIdentifier() {
        return componentIdentifier;
    }

    public void setNodeId(LogicalNodeId nodeId) {
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
    public NetworkDestination getStorageNetworkDestination() {
        return storageNetworkDestination;
    }

    public void setStorageNetworkDestination(NetworkDestination networkDestination) {
        this.storageNetworkDestination = networkDestination;
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
    public LogicalNodeId getWorkflowNodeId() {
        return workflowNodeId;
    }

    public void setWorkflowNodeId(LogicalNodeId workflowNodeId) {
        this.workflowNodeId = workflowNodeId;
    }

}
