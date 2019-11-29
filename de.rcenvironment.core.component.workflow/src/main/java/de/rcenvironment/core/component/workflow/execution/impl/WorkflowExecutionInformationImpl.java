/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;

/**
 * Implementation of {@link WorkflowExecutionInformation}.
 * 
 * @author Doreen Seider
 * @author Brigitte Boden
 */
public class WorkflowExecutionInformationImpl extends ComponentExecutionInformationImpl implements WorkflowExecutionInformation {

    private static final long serialVersionUID = 8037878257248368500L;

    private long instantiationTime;

    private WorkflowDescription workflowDescription;

    private Map<WorkflowNodeIdentifier, ComponentExecutionInformation> componentExecutionInformations = new HashMap<>();

    private LogicalNodeId nodeIdentifierStartedExecution;

    private String additionalInformation;

    private WorkflowState workflowState;

    private WorkflowExecutionHandle workflowExecutionHandle;

    private Long workflowDataManagementId = null;

    public WorkflowExecutionInformationImpl(WorkflowExecutionContext wfExeCtx) {
        setInstanceName(wfExeCtx.getInstanceName());
        setNodeId(wfExeCtx.getNodeId());
        setStorageNetworkDestination(wfExeCtx.getStorageNetworkDestination());
        setWorkflowDescription(wfExeCtx.getWorkflowDescription());
        setWorkflowExecutionContext(wfExeCtx);
        this.workflowExecutionHandle = wfExeCtx.getWorkflowExecutionHandle();
        this.instantiationTime = new Date().getTime();
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    @Override
    public WorkflowExecutionHandle getWorkflowExecutionHandle() {
        return workflowExecutionHandle;
    }

    @Override
    public long getStartTime() {
        return instantiationTime;
    }

    @Override
    public ComponentExecutionInformation getComponentExecutionInformation(WorkflowNodeIdentifier wfNodeId) {
        return componentExecutionInformations.get(wfNodeId);
    }

    @Override
    public Collection<ComponentExecutionInformation> getComponentExecutionInformations() {
        return componentExecutionInformations.values();
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    @Override
    public LogicalNodeId getNodeIdStartedExecution() {
        return nodeIdentifierStartedExecution;
    }

    @Override
    public String getAdditionalInformationProvidedAtStart() {
        return additionalInformation;
    }

    private void setWorkflowExecutionContext(WorkflowExecutionContext wfExeCtx) {
        componentExecutionInformations.clear();
        for (WorkflowNode wfNode : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
            ComponentExecutionInformationImpl componentExecutionInformation = new ComponentExecutionInformationImpl();
            ComponentExecutionIdentifier cei = wfExeCtx.getCompExeIdByWfNode(wfNode);
            componentExecutionInformation.setIdentifier(cei.toString()); // TODO
            componentExecutionInformation.setInstanceName(wfNode.getName());
            componentExecutionInformation.setNodeId(NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(
                wfNode.getComponentDescription().getComponentInstallation().getNodeId()));
            componentExecutionInformation.setComponentIdentifier(wfNode.getComponentDescription().getIdentifier());
            componentExecutionInformation.setStorageNetworkDestination(wfExeCtx.getStorageNetworkDestination());
            componentExecutionInformation.setWorkflowExecutionIdentifier(wfExeCtx.getExecutionIdentifier());
            componentExecutionInformation.setWorkflowInstanceName(wfExeCtx.getInstanceName());
            componentExecutionInformation.setWorkflowNodeId(wfExeCtx.getNodeId());
            componentExecutionInformations.put(wfNode.getIdentifierAsObject(), componentExecutionInformation);
        }
        nodeIdentifierStartedExecution = wfExeCtx.getNodeIdStartedExecution();
        additionalInformation = wfExeCtx.getAdditionalInformationProvidedAtStart();
    }

    @Override
    public int compareTo(WorkflowExecutionInformation other) {
        return getWorkflowDescription().getName().compareToIgnoreCase(other.getWorkflowDescription().getName());
    }

    @Override
    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    @Override
    public Long getWorkflowDataManagementId() {
        return workflowDataManagementId;
    }

    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
    }

    public void setWorkflowDataManagementId(Long workflowDataManagementId) {
        this.workflowDataManagementId = workflowDataManagementId;
    }

}
