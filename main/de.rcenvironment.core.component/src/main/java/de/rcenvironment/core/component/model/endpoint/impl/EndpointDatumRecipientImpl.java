/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.model.endpoint.impl;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Implementation of {@link EndpointDatumRecipient}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumRecipientImpl implements EndpointDatumRecipient {

    private static final long serialVersionUID = 2972547709143320719L;

    private String inputIdentifier;
    
    private String inputsComponentExecutionIdentifier;
    
    private NodeIdentifier inputsNode;
    
    @Override
    public String getInputName() {
        return inputIdentifier;
    }

    @Override
    public String getInputComponentExecutionIdentifier() {
        return inputsComponentExecutionIdentifier;
    }

    @Override
    public NodeIdentifier getInputsNodeId() {
        return inputsNode;
    }

    public void setIdentifier(String identifier) {
        this.inputIdentifier = identifier;
    }

    public void setInputsComponentExecutionIdentifier(String inputsCompExeIdentifier) {
        this.inputsComponentExecutionIdentifier = inputsCompExeIdentifier;
    }

    public void setInputsNodeId(NodeIdentifier node) {
        this.inputsNode = node;
    }

}
