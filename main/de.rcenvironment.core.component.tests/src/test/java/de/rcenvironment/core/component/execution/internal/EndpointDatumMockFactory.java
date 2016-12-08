/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;

/**
 * Mock factory for {@link EndpointDatum}s.
 * 
 * @author Doreen Seider
 */
public final class EndpointDatumMockFactory {

    private EndpointDatumMockFactory() {};

    /**
     * Creates mock instance of {@link EndpointDatum}.
     * 
     * @param inputsCompExeId component execution id of input's component
     * @param inputsNodeId {@link LogicalNodeId} of input's component
     * @param outputsCompExeId component execution id of output's component
     * @param outputsNodeId component execution id of output's component
     * @return mock instance of {@link EndpointDatum}
     */
    public static EndpointDatum createEndpointDatumMock(String inputsCompExeId, LogicalNodeId inputsNodeId,
        String outputsCompExeId, LogicalNodeId outputsNodeId) {
        EndpointDatum endpointDatumToProcessMock = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumToProcessMock.getInputsComponentExecutionIdentifier()).andReturn(inputsCompExeId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getInputsNodeId()).andReturn(inputsNodeId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getOutputsComponentExecutionIdentifier()).andReturn(outputsCompExeId).anyTimes();
        EasyMock.expect(endpointDatumToProcessMock.getOutputsNodeId()).andReturn(outputsNodeId).anyTimes();
        EasyMock.replay(endpointDatumToProcessMock);
        return endpointDatumToProcessMock;
    }
}
