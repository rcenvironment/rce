/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl.InternalTDType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Tests for {@link EndpointDatumSerializerImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 * 
 *         TODO (p2) 8.1.0: looks good, but was wondering why InstanceNodeSessionId is created first to get a LogicalNodeId. Would expect
 *         that NodeIdentifierTestUtils.createTestDefaultLogicalNodeIdWithDisplayName(displayName) is used instead. - seid_do (with regards
 *         to former "(p1) 8.0.0: needs semantic review after migration is complete - misc_ro")
 */
public class EndpointDatumSerializerImplTest {

    private static final String COLON = ":";
    

    private final InstanceNodeSessionId targetCompNodeInstanceSessionId = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("comp-node_t");

    private final LogicalNodeId targetCompNodeLogicalNodeId = targetCompNodeInstanceSessionId.convertToDefaultLogicalNodeId();

    private final String targetCompNodeSerializedForm = StringUtils.escapeSeparator(targetCompNodeLogicalNodeId.getLogicalNodeIdString());

    private final InstanceNodeSessionId sourceCompNodeInstanceSessionId = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("comp-node_s");

    private final LogicalNodeId sourceCompNodeLogicalNodeId = sourceCompNodeInstanceSessionId.convertToDefaultLogicalNodeId();

    private final String sourceCompNodeSerializedForm = StringUtils.escapeSeparator(sourceCompNodeLogicalNodeId.getLogicalNodeIdString());

    private final InstanceNodeSessionId wfCtrlNodeId =
        NodeIdentifierTestUtils.createTestInstanceNodeSessionIdWithDisplayName("wf-ctrl-node");

    private final LogicalNodeId wfCtrlNodeLogicalNodeId = wfCtrlNodeId.convertToDefaultLogicalNodeId();

    private final String wfCtrlNodeSerializedForm = StringUtils.escapeSeparator(wfCtrlNodeLogicalNodeId.getLogicalNodeIdString());

    /**
     * Test the serialization of {@link EndpointDatum}s with an internal {@link TypedDatum} as value.
     */
    @Test
    public void testSerializationOfEndDatumWithInternalTypedDatum() {

        EndpointDatumSerializerImpl endpointDatumSerializer = new EndpointDatumSerializerImpl();

        InternalTDImpl internalTypedDatumMock = EasyMock.createNiceMock(InternalTDImpl.class);
        EasyMock.expect(internalTypedDatumMock.getDataType()).andReturn(DataType.Internal).anyTimes();
        EasyMock.expect(internalTypedDatumMock.serialize()).andReturn("{\"t\":\"WorkflowFinish\",\"i\""
            + ":\"6b5d89c8-3a12-48aa-9440-c078646e7172\"}").anyTimes();
        EasyMock.replay(internalTypedDatumMock);

        EndpointDatum internalEndpointDatumMock = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(internalEndpointDatumMock.getInputName()).andReturn("int-input-name").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getValue()).andReturn(internalTypedDatumMock).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsComponentExecutionIdentifier()).andReturn("comp-exe-id-1").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsComponentInstanceName()).andReturn("comp name 1").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsNodeId()).andReturn(targetCompNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowNodeId()).andReturn(wfCtrlNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getOutputsComponentExecutionIdentifier()).andReturn("comp-exe-id-4").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getOutputsNodeId()).andReturn(sourceCompNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowExecutionIdentifier()).andReturn("wf-exe-id-1").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getDataManagementId()).andReturn(null).anyTimes();
        EasyMock.replay(internalEndpointDatumMock);

        String expectedSerializedEndpointDatum =
            "int-input-name:{\"t\"\\:\"WorkflowFinish\",\"i\"\\:\"6b5d89c8-"
                + "3a12-48aa-9440-c078646e7172\"}:comp-exe-id-1:comp name 1:" + targetCompNodeSerializedForm
                + ":comp-exe-id-4:" + sourceCompNodeSerializedForm + ":wf-exe-id-1:" + wfCtrlNodeSerializedForm + COLON;

        String serializedEndpointDatum = endpointDatumSerializer.serializeEndpointDatum(internalEndpointDatumMock);
        assertEquals(expectedSerializedEndpointDatum, serializedEndpointDatum);
    }

    /**
     * Test the serialization of {@link EndpointDatum}s with an non-internal {@link TypedDatum} as value.
     */
    @Test
    public void testSerializationOfEndDatumWithNonInternalTypedDatumWithoutDataManagementId() {
        testSerializationOfEndDatumWithNonInternalTypedDatum(null);
    }

    /**
     * Test the serialization of {@link EndpointDatum}s with an non-internal {@link TypedDatum} as value and with a data management id.
     */
    @Test
    public void testSerializationOfEndDatumWithNonInternalTypedDatumWithDataManagementId() {
        testSerializationOfEndDatumWithNonInternalTypedDatum(Long.valueOf(1));
    }

    private void testSerializationOfEndDatumWithNonInternalTypedDatum(Long dmId) {

        EndpointDatumSerializerImpl endpointDatumSerializer = new EndpointDatumSerializerImpl();

        TypedDatum typedDatumMock = EasyMock.createNiceMock(TypedDatum.class);
        EasyMock.expect(typedDatumMock.getDataType()).andReturn(DataType.Float).anyTimes();
        EasyMock.replay(typedDatumMock);

        TypedDatumSerializer typedDatumSerializerMock = EasyMock.createStrictMock(TypedDatumSerializer.class);
        EasyMock.expect(typedDatumSerializerMock.serialize(typedDatumMock)).andReturn("serial-float-td");
        EasyMock.replay(typedDatumSerializerMock);

        TypedDatumService typedDatumServiceMock = EasyMock.createStrictMock(TypedDatumService.class);
        EasyMock.expect(typedDatumServiceMock.getSerializer()).andReturn(typedDatumSerializerMock);
        EasyMock.replay(typedDatumServiceMock);

        endpointDatumSerializer.bindTypedDatumService(typedDatumServiceMock);

        EndpointDatum internalEndpointDatumMock = EasyMock.createNiceMock(EndpointDatum.class);
        EasyMock.expect(internalEndpointDatumMock.getInputName()).andReturn("input-name").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getValue()).andReturn(typedDatumMock).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsComponentExecutionIdentifier()).andReturn("comp-exe-id-2").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsComponentInstanceName()).andReturn("comp name 2").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsNodeId()).andReturn(targetCompNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowNodeId()).andReturn(wfCtrlNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowExecutionIdentifier()).andReturn("wf-exe-id-2").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getOutputsComponentExecutionIdentifier()).andReturn("comp-exe-id-5").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getOutputsNodeId()).andReturn(sourceCompNodeLogicalNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getDataManagementId()).andReturn(dmId).anyTimes();
        EasyMock.replay(internalEndpointDatumMock);

        String expectedSerializedEndpointDatum =
            "input-name:serial-float-td:comp-exe-id-2:comp name 2" + COLON + targetCompNodeSerializedForm + ":comp-exe-id-5:"
                + sourceCompNodeSerializedForm + ":wf-exe-id-2:" + wfCtrlNodeSerializedForm + COLON;
        if (dmId != null) {
            expectedSerializedEndpointDatum += dmId;
        }
        String serializedEndpointDatum = endpointDatumSerializer.serializeEndpointDatum(internalEndpointDatumMock);
        assertEquals(expectedSerializedEndpointDatum, serializedEndpointDatum);
    }

    /**
     * Test the deserialization of {@link EndpointDatum}s with an internal {@link TypedDatum} as value.
     */
    @Test
    public void testDeserializationOfEndDatumWithInternalTypedDatum() {
        EndpointDatumSerializerImpl endpointDatumSerializer = new EndpointDatumSerializerImpl();
        EndpointDatum deserializeEndpointDatum =
            endpointDatumSerializer
                .deserializeEndpointDatum(
                    "int-input-name:{\"t\"\\:\"WorkflowFinish\",\"i\"\\:\"6b5d89c8-3a12-48aa-9440-c078646e7172\"}:"
                        + "comp-exe-id-1:comp name 1:" + targetCompNodeSerializedForm + ":comp-exe-id-3:" + sourceCompNodeSerializedForm
                        + ":wf-exe-id-1:" + wfCtrlNodeSerializedForm + COLON);

        assertEquals("int-input-name", deserializeEndpointDatum.getInputName());
        assertNull(deserializeEndpointDatum.getDataManagementId());
        assertEquals("comp-exe-id-1", deserializeEndpointDatum.getInputsComponentExecutionIdentifier());
        assertEquals("comp name 1", deserializeEndpointDatum.getInputsComponentInstanceName());
        assertEquals(targetCompNodeLogicalNodeId, deserializeEndpointDatum.getInputsNodeId());
        assertEquals("wf-exe-id-1", deserializeEndpointDatum.getWorkflowExecutionIdentifier());
        assertEquals("comp-exe-id-3", deserializeEndpointDatum.getOutputsComponentExecutionIdentifier());
        assertEquals(sourceCompNodeLogicalNodeId, deserializeEndpointDatum.getOutputsNodeId());
        assertEquals(wfCtrlNodeLogicalNodeId, deserializeEndpointDatum.getWorkflowNodeId());
        assertEquals("6b5d89c8-3a12-48aa-9440-c078646e7172", ((InternalTDImpl) deserializeEndpointDatum.getValue()).getIdentifier());
        assertEquals(InternalTDType.WorkflowFinish, ((InternalTDImpl) deserializeEndpointDatum.getValue()).getType());
    }

    /**
     * Test the deserialization of {@link EndpointDatum}s with an non-internal {@link TypedDatum} as value.
     */
    @Test
    public void testDeserializationOfEndDatumWithNonInternalTypedDatumWithoutDataManagementId() {
        testDeserializationOfEndDatumWithNonInternalTypedDatum(null);
    }

    /**
     * Test the deserialization of {@link EndpointDatum}s with an non-internal {@link TypedDatum} as value and with a data management id.
     */
    @Test
    public void testDeserializationOfEndDatumWithNonInternalTypedDatumWithDataManagementId() {
        testDeserializationOfEndDatumWithNonInternalTypedDatum(Long.valueOf(1));
    }

    private void testDeserializationOfEndDatumWithNonInternalTypedDatum(Long dmId) {
        EndpointDatumSerializerImpl endpointDatumSerializer = new EndpointDatumSerializerImpl();

        TypedDatum typedDatumMock = EasyMock.createNiceMock(TypedDatum.class);
        EasyMock.expect(typedDatumMock.getDataType()).andReturn(DataType.Float).anyTimes();
        EasyMock.replay(typedDatumMock);

        TypedDatumSerializer typedDatumSerializerMock = EasyMock.createStrictMock(TypedDatumSerializer.class);
        EasyMock.expect(typedDatumSerializerMock.deserialize("serial-float-td")).andReturn(typedDatumMock);
        EasyMock.replay(typedDatumSerializerMock);

        TypedDatumService typedDatumServiceMock = EasyMock.createStrictMock(TypedDatumService.class);
        EasyMock.expect(typedDatumServiceMock.getSerializer()).andReturn(typedDatumSerializerMock);
        EasyMock.replay(typedDatumServiceMock);

        endpointDatumSerializer.bindTypedDatumService(typedDatumServiceMock);

        String serializedEndpointDatum =
            "input-name:serial-float-td:comp-exe-id-2:comp name 2"
                + COLON + targetCompNodeSerializedForm + ":comp-exe-id-5:" + sourceCompNodeSerializedForm + ":wf-exe-id-2:"
                + wfCtrlNodeSerializedForm + COLON;
        if (dmId != null) {
            serializedEndpointDatum += dmId + COLON;
        }
        EndpointDatum deserializedEndpointDatum = endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum);

        assertEquals("input-name", deserializedEndpointDatum.getInputName());
        assertEquals(dmId, deserializedEndpointDatum.getDataManagementId());
        assertEquals("comp-exe-id-2", deserializedEndpointDatum.getInputsComponentExecutionIdentifier());
        assertEquals("comp name 2", deserializedEndpointDatum.getInputsComponentInstanceName());
        assertEquals(targetCompNodeLogicalNodeId, deserializedEndpointDatum.getInputsNodeId());
        assertEquals("wf-exe-id-2", deserializedEndpointDatum.getWorkflowExecutionIdentifier());
        assertEquals("comp-exe-id-5", deserializedEndpointDatum.getOutputsComponentExecutionIdentifier());
        assertEquals(sourceCompNodeLogicalNodeId, deserializedEndpointDatum.getOutputsNodeId());
        assertEquals(wfCtrlNodeLogicalNodeId, deserializedEndpointDatum.getWorkflowNodeId());
        assertEquals(typedDatumMock, deserializedEndpointDatum.getValue());
    }

}
