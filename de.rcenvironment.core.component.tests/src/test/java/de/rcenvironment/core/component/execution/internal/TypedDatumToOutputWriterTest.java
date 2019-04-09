/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.EndpointDatumDispatchService;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.datamodel.api.TypedDatum;

/**
 * Test cases for {@link TypedDatumToOutputWriter}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class TypedDatumToOutputWriterTest {

    private static final String INP_NAME_1 = "inp-name-1";

    private static final String INP_COMP_EXE_ID_1 = "inp-comp-exe-id-1";

    private static final String INP_NAME_2 = "inp-name-2";

    private static final String INP_COMP_EXE_ID_2 = "inp-comp-exe-id-2";

    private static final String OUTPUT_CONNECTED = "output-connected";

    private static final String OUTPUT_NOT_CONNECTED = "output-non-connected";

    private static final String OUTP_COMP_EXE_ID = "outp-comp-exe-id";

    private static final InstanceNodeSessionId OUTP_COMP_INSTANCE_SESSION_ID =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("outp-comp-node-id");

    private static final LogicalNodeId OUTP_COMP_LOGICAL_NODE_ID =
        OUTP_COMP_INSTANCE_SESSION_ID.convertToDefaultLogicalNodeId();

    private static final String WF_CTRL_EXE_ID = "wf-ctrl-exe-id";

    private static final InstanceNodeSessionId WF_CTRL_INSTANCE_SESSION_ID =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("wf-ctrl-node-id");

    private static final LogicalNodeId WF_CTRL_LOGICAL_NODE_ID =
        WF_CTRL_INSTANCE_SESSION_ID.convertToDefaultLogicalNodeId();

    private EndpointDatumRecipient epRecipientMock1;

    private EndpointDatumRecipient epRecipientMock2;

    /**
     * Tests writing {@link TypedDatum} to an output that is not connected at all by considering all target inputs.
     */
    @Test
    public void testWritingTypedDatumToNotConnectedOutput() {

        EndpointDatumDispatchService epDispatcherMock = EasyMock.createStrictMock(EndpointDatumDispatchService.class);
        EasyMock.replay(epDispatcherMock);

        TypedDatumToOutputWriter outputWriter = createTypedDatumToOutputWriterTestInstance(epDispatcherMock);

        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        outputWriter.writeTypedDatumToOutput(OUTPUT_NOT_CONNECTED, typedDatumMock);

        EasyMock.verify(epDispatcherMock);
    }

    /**
     * Tests writing {@link TypedDatum} to an output that is connected by considering all target inputs.
     */
    @Test
    public void testWritingTypedDatumToConnectedOutput() {
        testWritingTypedDatumToOutput(null);
        testWritingTypedDatumToOutput(new Long(4));
    }

    private void testWritingTypedDatumToOutput(Long dmId) {

        EndpointDatumDispatchService epDispatcherMock = EasyMock.createStrictMock(EndpointDatumDispatchService.class);
        Capture<EndpointDatum> epCapture1 = new Capture<>();
        epDispatcherMock.dispatchEndpointDatum(EasyMock.capture(epCapture1));
        Capture<EndpointDatum> epCapture2 = new Capture<>();
        epDispatcherMock.dispatchEndpointDatum(EasyMock.capture(epCapture2));
        EasyMock.replay(epDispatcherMock);

        TypedDatumToOutputWriter outputWriter = createTypedDatumToOutputWriterTestInstance(epDispatcherMock);

        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        if (dmId != null) {
            outputWriter.writeTypedDatumToOutput(OUTPUT_CONNECTED, typedDatumMock, dmId);
        } else {
            outputWriter.writeTypedDatumToOutput(OUTPUT_CONNECTED, typedDatumMock);
        }

        EasyMock.verify(epDispatcherMock);
        assertTrue(epCapture1.hasCaptured());
        assertEquals(typedDatumMock, epCapture1.getValue().getValue());
        assertTrue(epCapture2.hasCaptured());
        assertEquals(typedDatumMock, epCapture2.getValue().getValue());

        assertEquals(epRecipientMock1, epCapture1.getValue().getEndpointDatumRecipient());
        assertEquals(OUTP_COMP_EXE_ID, epCapture1.getValue().getOutputsComponentExecutionIdentifier());
        assertEquals(OUTP_COMP_LOGICAL_NODE_ID, epCapture1.getValue().getOutputsNodeId());
        assertEquals(WF_CTRL_EXE_ID, epCapture1.getValue().getWorkflowExecutionIdentifier());
        assertEquals(WF_CTRL_LOGICAL_NODE_ID, epCapture1.getValue().getWorkflowControllerLocation());
        assertEquals(dmId, epCapture1.getValue().getDataManagementId());
    }

    /**
     * Tests writing {@link TypedDatum} to an output that is not connected at all or not connected to the input given by its name and
     * execution identifier of its component by considering only certain target inputs.
     */
    @Test
    public void testWritingTypedDatumToNotConnectedOutputConsideringTargetInput() {

        EndpointDatumDispatchService epDispatcherMock = EasyMock.createStrictMock(EndpointDatumDispatchService.class);
        EasyMock.replay(epDispatcherMock);

        TypedDatumToOutputWriter outputWriter = createTypedDatumToOutputWriterTestInstance(epDispatcherMock);

        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        outputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(OUTPUT_NOT_CONNECTED, typedDatumMock, INP_COMP_EXE_ID_1,
            INP_NAME_1);

        EasyMock.verify(epDispatcherMock);

        outputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(OUTPUT_CONNECTED, typedDatumMock, INP_COMP_EXE_ID_1, INP_NAME_2);

        EasyMock.verify(epDispatcherMock);
    }

    /**
     * Tests writing {@link TypedDatum} to an output that is connected by considering only certain target input.
     */
    @Test
    public void testWritingTypedDatumToConnectedOutputConsideringTargetInput() {

        EndpointDatumDispatchService epDispatcherMock = EasyMock.createStrictMock(EndpointDatumDispatchService.class);
        Capture<EndpointDatum> epCapture = new Capture<>();
        epDispatcherMock.dispatchEndpointDatum(EasyMock.capture(epCapture));
        EasyMock.replay(epDispatcherMock);

        TypedDatumToOutputWriter outputWriter = createTypedDatumToOutputWriterTestInstance(epDispatcherMock);

        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        outputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(OUTPUT_CONNECTED, typedDatumMock, INP_COMP_EXE_ID_1, INP_NAME_1);

        EasyMock.verify(epDispatcherMock);
        assertTrue(epCapture.hasCaptured());

        assertEquals(typedDatumMock, epCapture.getValue().getValue());

        assertEquals(epRecipientMock1, epCapture.getValue().getEndpointDatumRecipient());
        assertEquals(OUTP_COMP_EXE_ID, epCapture.getValue().getOutputsComponentExecutionIdentifier());
        assertEquals(OUTP_COMP_LOGICAL_NODE_ID, epCapture.getValue().getOutputsNodeId());
        assertEquals(WF_CTRL_EXE_ID, epCapture.getValue().getWorkflowExecutionIdentifier());
        assertEquals(WF_CTRL_LOGICAL_NODE_ID, epCapture.getValue().getWorkflowControllerLocation());
        assertNull(epCapture.getValue().getDataManagementId());

    }

    private TypedDatumToOutputWriter createTypedDatumToOutputWriterTestInstance(EndpointDatumDispatchService epDispatcherMock) {
        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = createComponentExecutionContextMock();
        TypedDatumToOutputWriter outputWriter = new TypedDatumToOutputWriter(compExeRelatedInstances);
        outputWriter.bindEndpointDatumDispatcher(epDispatcherMock);
        return outputWriter;
    }

    private ComponentExecutionContext createComponentExecutionContextMock() {
        ComponentExecutionContext compExeCtxMock = EasyMock.createStrictMock(ComponentExecutionContext.class);
        EasyMock.expect(compExeCtxMock.getEndpointDatumRecipients()).andStubReturn(createEndpointDatumRecipientMocks());
        EasyMock.expect(compExeCtxMock.getExecutionIdentifier()).andStubReturn(OUTP_COMP_EXE_ID);
        EasyMock.expect(compExeCtxMock.getNodeId()).andStubReturn(OUTP_COMP_LOGICAL_NODE_ID);
        EasyMock.expect(compExeCtxMock.getWorkflowExecutionIdentifier()).andStubReturn(WF_CTRL_EXE_ID);
        EasyMock.expect(compExeCtxMock.getWorkflowNodeId()).andStubReturn(WF_CTRL_LOGICAL_NODE_ID);
        EasyMock.replay(compExeCtxMock);
        return compExeCtxMock;
    }

    private Map<String, List<EndpointDatumRecipient>> createEndpointDatumRecipientMocks() {
        epRecipientMock1 = EasyMock.createStrictMock(EndpointDatumRecipient.class);
        EasyMock.expect(epRecipientMock1.getInputsComponentExecutionIdentifier()).andStubReturn(INP_COMP_EXE_ID_1);
        EasyMock.expect(epRecipientMock1.getInputName()).andStubReturn(INP_NAME_1);
        EasyMock.replay(epRecipientMock1);

        epRecipientMock2 = EasyMock.createStrictMock(EndpointDatumRecipient.class);
        EasyMock.expect(epRecipientMock2.getInputsComponentExecutionIdentifier()).andStubReturn(INP_COMP_EXE_ID_2);
        EasyMock.expect(epRecipientMock2.getInputName()).andStubReturn(INP_NAME_2);
        EasyMock.replay(epRecipientMock2);

        List<EndpointDatumRecipient> epRecipientsForConnectedOutput = new ArrayList<>();
        epRecipientsForConnectedOutput.add(epRecipientMock1);
        epRecipientsForConnectedOutput.add(epRecipientMock2);

        Map<String, List<EndpointDatumRecipient>> allEpRecipients = new HashMap<>();
        allEpRecipients.put(OUTPUT_CONNECTED, epRecipientsForConnectedOutput);

        return allEpRecipients;
    }

}
