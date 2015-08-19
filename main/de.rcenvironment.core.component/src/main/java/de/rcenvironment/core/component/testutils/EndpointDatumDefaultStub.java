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
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Default mock for {@link EndpointDatum}.
 * 
 * @author Doreen Seider
 */
public class EndpointDatumDefaultStub implements EndpointDatum {

    private static final long serialVersionUID = 9197713563968264677L;

    @Override
    public String getInputName() {
        return null;
    }

    @Override
    public String getInputComponentExecutionIdentifier() {
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

}
