/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.ComponentExecutionInformation;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

/**
 * Implementation of {@link WorkflowExecutionInformation}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionInformationImpl extends ComponentExecutionInformationImpl implements WorkflowExecutionInformation  {

    private static final long serialVersionUID = 8037878257248368500L;

    private long instantiationTime;
    
    private WorkflowDescription workflowDescription;
    
    private Map<String, ComponentExecutionInformation> componentExecutionInformations = new HashMap<>();
    
    private NodeIdentifier nodeIdentifierStartedExecution;
    
    private String additionalInformation;
    
    private WorkflowState workflowState;
    
    private Long workflowDataManagementId = null;
    
    public WorkflowExecutionInformationImpl(WorkflowExecutionContext wfExeCtx) {
        setInstanceName(wfExeCtx.getInstanceName());
        setNodeId(wfExeCtx.getNodeId());
        setDefaultStorageNodeId(wfExeCtx.getDefaultStorageNodeId());
        setWorkflowDescription(wfExeCtx.getWorkflowDescription());
        setWorkflowExecutionContext(wfExeCtx);
        instantiationTime = new Date().getTime();
    }
    
    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    @Override
    public long getStartTime() {
        return instantiationTime;
    }
    
    @Override
    public ComponentExecutionInformation getComponentExecutionInformation(String wfNodeId) {
        return componentExecutionInformations.get(wfNodeId);
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    @Override
    public NodeIdentifier getNodeIdStartedExecution() {
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
            componentExecutionInformation.setIdentifier(wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier()));
            componentExecutionInformation.setInstanceName(wfNode.getName());
            componentExecutionInformation.setNodeId(NodeIdentifierFactory.fromNodeId(wfNode.getComponentDescription()
                .getComponentInstallation().getNodeId()));
            componentExecutionInformation.setComponentIdentifier(wfNode.getComponentDescription().getIdentifier());
            componentExecutionInformation.setDefaultStorageNodeId(wfExeCtx.getDefaultStorageNodeId());
            componentExecutionInformations.put(wfNode.getIdentifier(), componentExecutionInformation);
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
