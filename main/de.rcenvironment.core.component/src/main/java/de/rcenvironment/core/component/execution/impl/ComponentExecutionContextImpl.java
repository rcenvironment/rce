/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.utils.common.StringUtils;

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
    
    // EndpointDatumRecipient stored as String for serialization purposes
    private Map<String, List<String>> serializedEndpointDatumRecipients = new HashMap<String, List<String>>();

    private File workingDirectory;
    
    private WorkflowGraph workflowGraph;
    
    private Long workflowInstanceDataManagementId;
    
    private Long instanceDataManagementId;
    
    private Map<String, Long> inputDataManagementIds;

    private Map<String, Long> outputDataManagementIds;
    
    private Map<String, List<EndpointDatumRecipient>> cachedEndpointDatumRecipients;

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
        if (cachedEndpointDatumRecipients == null) {
            Map<String, List<EndpointDatumRecipient>> edrs = new HashMap<>();
            for (String output : serializedEndpointDatumRecipients.keySet()) {
                edrs.put(output, new ArrayList<EndpointDatumRecipient>());
                for (String sedr : serializedEndpointDatumRecipients.get(output)) {
                    String[] parts = StringUtils.splitAndUnescape(sedr);
                    edrs.get(output).add(EndpointDatumRecipientFactory.createEndpointDatumRecipient(
                        parts[0], parts[1], parts[2], NodeIdentifierFactory.fromNodeId(parts[3])));
                }
            }
            cachedEndpointDatumRecipients = edrs;
        }
        return cachedEndpointDatumRecipients;
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
    
    /**
     * Sets {@link EndpointDatumRecipient}s per output and serializes {@link EndpointDatumRecipient} instances.
     * 
     * @param endpointDatumRecipients list of {@link EndpointDatumRecipient}s per output
     */
    public void setEndpointDatumRecipients(Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients) {
        for (String output : endpointDatumRecipients.keySet()) {
            serializedEndpointDatumRecipients.put(output, new ArrayList<String>());
            for (EndpointDatumRecipient edr : endpointDatumRecipients.get(output)) {
                serializedEndpointDatumRecipients.get(output).add(StringUtils.escapeAndConcat(
                    new String[] { edr.getInputName(), edr.getInputsComponentExecutionIdentifier(),
                        edr.getInputsComponentInstanceName(), edr.getInputsNodeId().getIdString() }));
            }
        }
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
