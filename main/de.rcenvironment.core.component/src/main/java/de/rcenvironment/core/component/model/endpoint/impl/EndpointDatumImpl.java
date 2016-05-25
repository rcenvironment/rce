/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.impl;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Implementation of {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumImpl implements EndpointDatum {

    private NodeIdentifier workflowNode;
    
    private String componentExecutionIdentifier;

    private NodeIdentifier outputsNodeId;

    private String workflowExecutionIdentifier;
    
    private Long dataManagementId;
    
    private TypedDatum value;
    
    private EndpointDatumRecipient endpointDatumRecipient;

    @Override
    public TypedDatum getValue() {
        return value;
    }
    
    @Override
    public String getInputName() {
        return endpointDatumRecipient.getInputName();
    }

    @Override
    public String getInputsComponentExecutionIdentifier() {
        return endpointDatumRecipient.getInputsComponentExecutionIdentifier();
    }
    
    @Override
    public String getInputsComponentInstanceName() {
        return endpointDatumRecipient.getInputsComponentInstanceName();
    }

    @Override
    public NodeIdentifier getInputsNodeId() {
        return endpointDatumRecipient.getInputsNodeId();
    }
    
    @Override
    public String getOutputsComponentExecutionIdentifier() {
        return componentExecutionIdentifier;
    }

    @Override
    public NodeIdentifier getOutputsNodeId() {
        return outputsNodeId;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return workflowExecutionIdentifier;
    }
    
    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return workflowNode;
    }
    
    @Override
    public Long getDataManagementId() {
        return dataManagementId;
    }
    
    @Override
    public EndpointDatumRecipient getEndpointDatumRecipient() {
        return endpointDatumRecipient;
    }

    public void setValue(TypedDatum value) {
        this.value = value;
    }
    
    public void setOutputsComponentExecutionIdentifier(String compExeIdentifier) {
        this.componentExecutionIdentifier = compExeIdentifier;
    }

    public void setOutputsNodeId(NodeIdentifier outputsNodeId) {
        this.outputsNodeId = outputsNodeId;
    }

    public void setWorkflowExecutionIdentifier(String wfExeIdentifier) {
        this.workflowExecutionIdentifier = wfExeIdentifier;
    }
    
    public void setWorkfowNodeId(NodeIdentifier node) {
        this.workflowNode = node;
    }
    
    public void setDataManagementId(Long dataManagementId) {
        this.dataManagementId = dataManagementId;
    }
    
    public void setEndpointDatumRecipient(EndpointDatumRecipient endpointDatumRecipient) {
        this.endpointDatumRecipient = endpointDatumRecipient;
    }
    
    @Override
    public String toString() {
        return StringUtils.format("'%s' (%s @ %s -> %s @ %s (%s) at %s)", getValue().toString(), 
            getOutputsComponentExecutionIdentifier(), getOutputsNodeId(),
            getInputName(), getInputsComponentInstanceName(), getInputsComponentExecutionIdentifier(), getInputsNodeId());
    }

}
