/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionContextImpl;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Create {@link ComponentExecutionContext} objects.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionContextBuilder {

    private ComponentExecutionContextImpl compExeCtx;

    public ComponentExecutionContextBuilder() {
        compExeCtx = new ComponentExecutionContextImpl();
    }

    /**
     * Sets the execution identifiers.
     * 
     * @param executionIdentifier execution identifier of the component executed
     * @param workflowExecutionIdentifier execution identifier of the workflow executed
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setExecutionIdentifiers(String executionIdentifier, String workflowExecutionIdentifier) {
        compExeCtx.setExecutionIdentifier(executionIdentifier);
        compExeCtx.setWorkflowExecutionIdentifier(workflowExecutionIdentifier);
        return this;
    }

    /**
     * Sets the instance names.
     * 
     * @param instanceName name of the component executed
     * @param workflowInstanceName name of the workflow executed
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setInstanceNames(String instanceName, String workflowInstanceName) {
        compExeCtx.setInstanceName(instanceName);
        compExeCtx.setWorkflowInstanceName(workflowInstanceName);
        return this;
    }

    /**
     * Sets the nodes.
     * 
     * @param workflowNode hosting node of the workflow (controller) executed
     * @param defaultStorageNode node of the default storage
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setNodes(LogicalNodeId workflowNode, LogicalNodeId defaultStorageNode) {
        compExeCtx.setWorkflowNodeId(workflowNode);
        compExeCtx.setStorageNodeId(defaultStorageNode);
        return this;
    }

    /**
     * Sets the {@link ComponentDescription} of the component executed.
     * 
     * @param componentDescription {@link ComponentDescription} of the component executed.
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setComponentDescription(ComponentDescription componentDescription) {
        compExeCtx.setComponentDescription(componentDescription);
        return this;
    }

    /**
     * Sets information about predecessors and successors of the component executed.
     * 
     * @param isConnectedToEndpointDatumSenders <code>true</code> if the component executed is connected to inputs, otherwise
     *        <code>false</code>
     * @param endpointDatumRecipients set of {@link EndpointDatumRecipient}s
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setPredecessorAndSuccessorInformation(boolean isConnectedToEndpointDatumSenders,
        Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients) {
        compExeCtx.setIsConnectedToEndpointDatumSenders(isConnectedToEndpointDatumSenders);
        compExeCtx.setEndpointDatumRecipients(endpointDatumRecipients);
        return this;
    }

    /**
     * Sets the workflow graph.
     * 
     * @param workflowGraph {@link WorkflowGraph} instance with graph information of the associated workflow
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setWorkflowGraph(WorkflowGraph workflowGraph) {
        compExeCtx.setWorkflowGraph(workflowGraph);
        return this;
    }

    /**
     * Sets the data management id of the component instance.
     * 
     * @param workflowInstanceDataManagementId data management id of the workflow instance
     * @param instanceDataManagementId data management id of the component instance
     * @param inputDataManagementIds data management ids of the inputs
     * @param outputDataManagementIds data management ids of the outputs
     * @return {@link ComponentExecutionContextBuilder} instance for method chaining purposes
     */
    public ComponentExecutionContextBuilder setDataManagementIds(Long workflowInstanceDataManagementId, Long instanceDataManagementId,
        Map<String, Long> inputDataManagementIds,
        Map<String, Long> outputDataManagementIds) {
        compExeCtx.setWorkflowInstanceDataManagementId(workflowInstanceDataManagementId);
        compExeCtx.setInstanceDataManagementId(instanceDataManagementId);
        compExeCtx.setInputDataManagementIds(inputDataManagementIds);
        compExeCtx.setOutputDataManagementIds(outputDataManagementIds);
        return this;
    }

    /**
     * @return {@link ComponentExecutionContext} object
     */
    public ComponentExecutionContext build() {
        return compExeCtx;
    }

}
