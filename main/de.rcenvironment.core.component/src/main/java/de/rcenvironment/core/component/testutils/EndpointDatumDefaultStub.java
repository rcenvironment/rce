/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Default mock for {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumDefaultStub implements EndpointDatum {

    @Override
    public String getInputName() {
        return null;
    }

    @Override
    public String getInputsComponentExecutionIdentifier() {
        return null;
    }

    @Override
    public NodeIdentifier getInputsNodeId() {
        return null;
    }

    @Override
    public TypedDatum getValue() {
        return null;
    }
    
    @Override
    public String getOutputsComponentExecutionIdentifier() {
        return null;
    }

    @Override
    public String getWorkflowExecutionIdentifier() {
        return null;
    }

    @Override
    public NodeIdentifier getWorkflowNodeId() {
        return null;
    }

    @Override
    public Long getDataManagementId() {
        return null;
    }

    @Override
    public String getInputsComponentInstanceName() {
        return null;
    }

    @Override
    public NodeIdentifier getOutputsNodeId() {
        return null;
    }

    @Override
    public EndpointDatumRecipient getEndpointDatumRecipient() {
        return null;
    }

}
