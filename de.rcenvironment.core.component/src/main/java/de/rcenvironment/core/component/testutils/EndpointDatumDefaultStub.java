/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.testutils;

import de.rcenvironment.core.communication.common.LogicalNodeId;
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
    public LogicalNodeId getInputsNodeId() {
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
    public LogicalNodeId getWorkflowNodeId() {
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
    public LogicalNodeId getOutputsNodeId() {
        return null;
    }

    @Override
    public EndpointDatumRecipient getEndpointDatumRecipient() {
        return null;
    }

}
