/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link ComponentExecutionContext}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class ComponentExecutionContextImpl implements ComponentExecutionContext {

    private static final long serialVersionUID = -6480792333241604054L;

    private ComponentExecutionIdentifier executionIdentifier;

    private String instanceName;

    private LogicalNodeId controllerNode;

    private String workflowExecutionIdentifier;

    private String workflowInstanceName;

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

    private Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients;

    private NetworkDestination workflowStorageNetworkDestination;

    private NetworkDestination workflowControllerNetworkDestination;

    private LogicalNodeId storageNodeId;

    @Deprecated
    @Override
    public String getExecutionIdentifier() {
        return executionIdentifier.toString();
    }

    @Override
    public ComponentExecutionIdentifier getExecutionIdentifierAsObject() {
        return executionIdentifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public LogicalNodeId getWorkflowNodeId() {
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
    public LogicalNodeId getStorageNodeId() {
        return storageNodeId;
    }

    @Override
    public NetworkDestination getStorageNetworkDestination() {
        return workflowStorageNetworkDestination;
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
    public LogicalNodeId getNodeId() {
        return NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(componentDescription
            .getComponentInstallation().getNodeId());
    }

    @Override
    public synchronized Map<String, List<EndpointDatumRecipient>> getEndpointDatumRecipients() {
        if (endpointDatumRecipients == null) {
            throw new IllegalStateException("EndpointDatumRecipients have not been initialized");
        }
        return endpointDatumRecipients;
    }

    /**
     * Initializes the point-to-point reliable RPC streams for passing {@link EndpointDatum}s. Called as part of the local
     * createExecutionController() setup.
     * 
     * @param communicationService the {@link CommunicationService} to set up the reliable RPC streams
     * @return a list of all deserialized {@link EndpointDatumRecipient} instances for external processing
     * @throws RemoteOperationException on setup failure
     */
    // TODO decide: make part of interface?
    public synchronized List<EndpointDatumRecipient> deserializeEndpointDatumRecipients(CommunicationService communicationService)
        throws RemoteOperationException {
        endpointDatumRecipients = new HashMap<>();
        List<EndpointDatumRecipient> allCreatedRecipients = new ArrayList<>();
        for (String output : serializedEndpointDatumRecipients.keySet()) {
            endpointDatumRecipients.put(output, new ArrayList<EndpointDatumRecipient>());
            for (String sedr : serializedEndpointDatumRecipients.get(output)) {
                String[] parts = StringUtils.splitAndUnescape(sedr);
                final String inputIdentifier = parts[0];
                final String componentExecutionIdentifier = parts[1];
                final String componentInstanceName = parts[2];
                // TODO (p2) use more specific parsing method; the id type should be known
                final LogicalNodeId targetNodeId = NodeIdentifierUtils.parseArbitraryIdStringToLogicalNodeIdWithExceptionWrapping(parts[3]);
                EndpointDatumRecipient endpointDatumRecipient = EndpointDatumRecipientFactory.createEndpointDatumRecipient(inputIdentifier,
                    componentExecutionIdentifier, componentInstanceName, targetNodeId);
                endpointDatumRecipients.get(output).add(endpointDatumRecipient);
                allCreatedRecipients.add(endpointDatumRecipient);
            }
        }
        return allCreatedRecipients;
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

    @Deprecated
    public void setExecutionIdentifier(String executionIdentifier) {
        this.executionIdentifier = new ComponentExecutionIdentifier(executionIdentifier);
    }

    public void setExecutionIdentifier(ComponentExecutionIdentifier executionIdentifier) {
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

    public void setWorkflowNodeId(LogicalNodeId wfNodeId) {
        this.controllerNode = wfNodeId;
    }

    public void setStorageNodeId(LogicalNodeId defaultStorageNode) {
        this.storageNodeId = defaultStorageNode;
    }

    public void setStorageNetworkDestination(NetworkDestination storageNetworkDestination) {
        this.workflowStorageNetworkDestination = storageNetworkDestination;
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
                        edr.getInputsComponentInstanceName(), edr.getDestinationNodeId().getLogicalNodeIdString() }));
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

    @Override
    public ServiceCallContext getServiceCallContext() {
        return null;
    }

}
