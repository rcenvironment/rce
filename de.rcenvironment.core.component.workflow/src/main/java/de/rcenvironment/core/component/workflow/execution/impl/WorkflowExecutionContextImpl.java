/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;

/**
 * Implementation of {@link WorkflowExecutionContext}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionContextImpl implements WorkflowExecutionContext {

    private static final long serialVersionUID = 238066231055021678L;

    private String executionIdentifier;

    private String instanceName;

    private WorkflowDescription workflowDescription;

    private WorkflowExecutionHandle workflowHandle;

    private Map<WorkflowNodeIdentifier, ComponentExecutionIdentifier> componentExecutionIdentifiers;

    private LogicalNodeId nodeIdentifierStartedExecution;

    private String additionalInformation;

    private LogicalNodeId storageNetworkDestination;

    public WorkflowExecutionContextImpl(String executionIdentifier, WorkflowDescription workflowDescription) {
        this.executionIdentifier = executionIdentifier;
        this.workflowDescription = workflowDescription;
        this.workflowHandle = new ExecutionHandleImpl(executionIdentifier, workflowDescription.getControllerNode());
        componentExecutionIdentifiers = new HashMap<>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            ComponentExecutionIdentifier compExeId = new ComponentExecutionIdentifier(UUID.randomUUID().toString());
            componentExecutionIdentifiers.put(wfNode.getIdentifierAsObject(), compExeId);
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
    public WorkflowExecutionHandle getWorkflowExecutionHandle() {
        return workflowHandle;
    }

    @Override
    public LogicalNodeId getStorageNodeId() {
        return workflowDescription.getControllerNode();
    }

    @Override
    public NetworkDestination getStorageNetworkDestination() {
        // important: this is only correct as long as this is used on the workflow node itself and it is also the storage node!
        return workflowDescription.getControllerNode();
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    @Override
    public ComponentExecutionIdentifier getCompExeIdByWfNode(WorkflowNode wfNode) {
        return componentExecutionIdentifiers.get(wfNode.getIdentifierAsObject());
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
