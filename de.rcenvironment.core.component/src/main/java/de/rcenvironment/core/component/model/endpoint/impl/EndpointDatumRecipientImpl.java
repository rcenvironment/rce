/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.model.endpoint.impl;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;

/**
 * Implementation of {@link EndpointDatumRecipient}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumRecipientImpl implements EndpointDatumRecipient {

    private String inputIdentifier;

    private String inputsComponentExecutionIdentifier;

    private String inputsComponentInstanceName;

    private LogicalNodeId inputsNodeId;

    @Override
    public String getInputName() {
        return inputIdentifier;
    }

    @Override
    public String getInputsComponentExecutionIdentifier() {
        return inputsComponentExecutionIdentifier;
    }

    @Override
    public String getInputsComponentInstanceName() {
        return inputsComponentInstanceName;
    }

    @Override
    public LogicalNodeId getInputsNodeId() {
        return inputsNodeId;
    }

    public void setIdentifier(String identifier) {
        this.inputIdentifier = identifier;
    }

    public void setInputsComponentExecutionIdentifier(String inputsComponentExecutionIdentifier) {
        this.inputsComponentExecutionIdentifier = inputsComponentExecutionIdentifier;
    }

    public void setInputsComponentInstanceName(String inputsComponentInstanceName) {
        this.inputsComponentInstanceName = inputsComponentInstanceName;
    }

    public void setInputsNodeId(LogicalNodeId inputsNodeId) {
        this.inputsNodeId = inputsNodeId;
    }

}
