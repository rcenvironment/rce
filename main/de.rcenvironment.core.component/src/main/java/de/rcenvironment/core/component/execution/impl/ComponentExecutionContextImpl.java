/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Implementation of {@link ComponentExecutionContext}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionContextImpl implements ComponentExecutionContext {

    private static final long serialVersionUID = -6480792333241604054L;

    private String executionIdentifier;

    private String instanceName;
    
    private NodeIdentifier controllerNode;

    private String workflowExecutionIdentifier;
    
    private String workflowInstanceName;

    private NodeIdentifier defaultStorageNodeId;
    
    private ComponentDescription componentDescription;
    
    private boolean isConnectedToEndpointDatumSenders;
    
    private Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients;

    private File workingDirectory;
    
    private WorkflowGraph workflowGraph;
    
    private Long workflowInstanceDataManagementId;
    
    private Long instanceDataManagementId;
    
    private Map<String, Long> inputDataManagementIds;

    private Map<String, Long> outputDataManagementIds;

    @Override
    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return controllerNode;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return workflowExecutionIdentifier;
    }

    @Override
    public String getWorkflowInstanceName() {
        return workflowInstanceName;
    }
    
    @Override
    public NodeIdentifier getDefaultStorageNodeId() {
        return defaultStorageNodeId;
    }

    @Override
    public ComponentDescription getComponentDescription() {
        return componentDescription;
    }

    @Override
    public boolean isConnectedToEndpointDatumSenders() {
        return isConnectedToEndpointDatumSenders;
    }
    
    @Override
    public NodeIdentifier getNodeId() {
        return NodeIdentifierFactory.fromNodeId(componentDescription.getComponentInstallation().getNodeId());
    }

    @Override
    public Map<String, List<EndpointDatumRecipient>> getEndpointDatumRecipients() {
        return endpointDatumRecipients;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }
    
    @Override
    public WorkflowGraph getWorkflowGraph() {
        return workflowGraph;
    }
    
    @Override
    public Long getWorkflowInstanceDataManagementId() {
        return workflowInstanceDataManagementId;
    }
    
    @Override
    public Long getInstanceDataManagementId() {
        return instanceDataManagementId;
    }
    
    @Override
    public Map<String, Long> getInputDataManagementIds() {
        return inputDataManagementIds;
    }

    @Override
    public Map<String, Long> getOutputDataManagementIds() {
        return outputDataManagementIds;
    }
    
    public void setExecutionIdentifier(String executionIdentifier) {
        this.executionIdentifier = executionIdentifier;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setIsConnectedToEndpointDatumSenders(boolean isConnectedToEndpointDatumSenders) {
        this.isConnectedToEndpointDatumSenders = isConnectedToEndpointDatumSenders;
    }
    
    public void setWorkflowExecutionIdentifier(String wfExeId) {
        this.workflowExecutionIdentifier = wfExeId;
    }
    
    public void setWorkflowInstanceName(String wfInstanceName) {
        this.workflowInstanceName = wfInstanceName;
    }
    
    public void setWorkflowNodeId(NodeIdentifier wfNodeId) {
        this.controllerNode = wfNodeId;
    }
    
    public void setDefaultStorageNode(NodeIdentifier defaultStorageNode) {
        this.defaultStorageNodeId = defaultStorageNode;
    }
    
    public void setComponentDescription(ComponentDescription componentDescription) {
        this.componentDescription = componentDescription;
    }
    
    public void setEndpointDatumRecipients(Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients) {
        this.endpointDatumRecipients = endpointDatumRecipients;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setWorkflowGraph(WorkflowGraph workflowGraph) {
        this.workflowGraph = workflowGraph;
    }
    
    public void setWorkflowInstanceDataManagementId(Long workflowInstanceDataManagementId) {
        this.workflowInstanceDataManagementId = workflowInstanceDataManagementId;
    }
    
    public void setInstanceDataManagementId(Long instanceDataManagementId) {
        this.instanceDataManagementId = instanceDataManagementId;
    }
    
    public void setInputDataManagementIds(Map<String, Long> inputDataManagementIds) {
        this.inputDataManagementIds = inputDataManagementIds;
    }
    
    public void setOutputDataManagementIds(Map<String, Long> outputDataManagementIds) {
        this.outputDataManagementIds = outputDataManagementIds;
    }

}
