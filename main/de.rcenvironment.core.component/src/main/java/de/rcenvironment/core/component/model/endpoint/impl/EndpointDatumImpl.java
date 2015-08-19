/*
 * Copyright (C) 2006-2015 DLR, Germany
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

/**
 * Implementation of {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumImpl implements EndpointDatum {

    private static final long serialVersionUID = -6636084678450015668L;

    private String inputName;
    
    private String inputsComponentExecutionIdentifier;
    
    private NodeIdentifier inputsNode;
    
    private NodeIdentifier workflowNode;
    
    private String workflowExecutionIdentifier;
    
    private Long dataManagementId;
    
    private TypedDatum value;

    @Override
    public TypedDatum getValue() {
        return value;
    }
    
    @Override
    public String getInputName() {
        return inputName;
    }

    @Override
    public String getInputComponentExecutionIdentifier() {
        return inputsComponentExecutionIdentifier;
    }

    @Override
    public NodeIdentifier getInputsNodeId() {
        return inputsNode;
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

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public void setValue(TypedDatum value) {
        this.value = value;
    }

    public void setWorkflowExecutionIdentifier(String wfExeIdentifier) {
        this.workflowExecutionIdentifier = wfExeIdentifier;
    }
    
    public void setInputsComponentExecutionIdentifier(String inputsCompExeIdentifier) {
        this.inputsComponentExecutionIdentifier = inputsCompExeIdentifier;
    }

    public void setInputsNode(NodeIdentifier node) {
        this.inputsNode = node;
    }
    
    public void setWorkfowNodeId(NodeIdentifier node) {
        this.workflowNode = node;
    }
    
    public void setDataManagementId(Long dataManagementId) {
        this.dataManagementId = dataManagementId;
    }
    
    /**
     * Sets information about the {@link EndpointDatumRecipient}.
     * 
     * @param endpointDatumRecipient {@link EndpointDatumRecipient} to fetch information from
     */
    public void setEndpointDatumRecipient(EndpointDatumRecipient endpointDatumRecipient) {
        setInputName(endpointDatumRecipient.getInputName());
        setInputsComponentExecutionIdentifier(endpointDatumRecipient.getInputComponentExecutionIdentifier());
        setInputsNode(endpointDatumRecipient.getInputsNodeId());
    }
    
    @Override
    public String toString() {
        return getValue().toString() + "@" + inputName;
    }

}
