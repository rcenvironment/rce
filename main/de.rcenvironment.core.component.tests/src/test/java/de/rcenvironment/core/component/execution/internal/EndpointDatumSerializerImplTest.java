/*
 * Copyright (C) 2006-2015 DLR, Germany
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

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl.InternalTDType;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.api.TypedDatumService;

/**
 * Tests for {@link EndpointDatumSerializerImpl}.
 * 
 * @author Doreen Seider
 * 
 */
public class EndpointDatumSerializerImplTest {

    private NodeIdentifier compNodeId = NodeIdentifierFactory.fromNodeId("comp-node");
    
    private NodeIdentifier wfCtrlNodeId = NodeIdentifierFactory.fromNodeId("wf-ctrl-node");

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
        EasyMock.expect(internalEndpointDatumMock.getInputComponentExecutionIdentifier()).andReturn("comp-exe-id-1").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsNodeId()).andReturn(compNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowNodeId()).andReturn(wfCtrlNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowExecutionIdentifier()).andReturn("wf-exe-id-1").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getDataManagementId()).andReturn(null).anyTimes();
        EasyMock.replay(internalEndpointDatumMock);
        
        String serializedEndpointDatum = endpointDatumSerializer.serializeEndpointDatum(internalEndpointDatumMock);
        assertEquals("int-input-name:{\"t\"\\:\"WorkflowFinish\",\"i\"\\:\"6b5d89c8-3a12-48aa-9440-c078646e7172\"}"
            + ":comp-exe-id-1:comp-node:wf-exe-id-1:wf-ctrl-node:", serializedEndpointDatum);
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
        EasyMock.expect(internalEndpointDatumMock.getInputComponentExecutionIdentifier()).andReturn("comp-exe-id-2").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getInputsNodeId()).andReturn(compNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowNodeId()).andReturn(wfCtrlNodeId).anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getWorkflowExecutionIdentifier()).andReturn("wf-exe-id-2").anyTimes();
        EasyMock.expect(internalEndpointDatumMock.getDataManagementId()).andReturn(dmId).anyTimes();
        EasyMock.replay(internalEndpointDatumMock);
        
        String expectedSerializedEndpointDatum = "input-name:serial-float-td:comp-exe-id-2:comp-node:wf-exe-id-2:wf-ctrl-node:";
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
        EndpointDatum deserializeEndpointDatum = endpointDatumSerializer.deserializeEndpointDatum(
            "int-input-name:{\"t\"\\:\"WorkflowFinish\",\"i\"\\:\"6b5d89c8-3a12-48aa-9440-c078646e7172\"}"
            + ":comp-exe-id-1:comp-node:wf-exe-id-1:wf-ctrl-node:");
        
        assertEquals("int-input-name", deserializeEndpointDatum.getInputName());
        assertNull(deserializeEndpointDatum.getDataManagementId());
        assertEquals("comp-exe-id-1", deserializeEndpointDatum.getInputComponentExecutionIdentifier());
        assertEquals(compNodeId, deserializeEndpointDatum.getInputsNodeId());
        assertEquals("wf-exe-id-1", deserializeEndpointDatum.getWorkflowExecutionIdentifier());
        assertEquals(wfCtrlNodeId, deserializeEndpointDatum.getWorkflowNodeId());
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
        
        String serializedEndpointDatum = "input-name:serial-float-td:comp-exe-id-2:comp-node:wf-exe-id-2:wf-ctrl-node:";
        if (dmId != null) {
            serializedEndpointDatum += dmId + ":";
        }
        EndpointDatum deserializedEndpointDatum = endpointDatumSerializer.deserializeEndpointDatum(serializedEndpointDatum);
        
        assertEquals("input-name", deserializedEndpointDatum.getInputName());
        assertEquals(dmId, deserializedEndpointDatum.getDataManagementId());
        assertEquals("comp-exe-id-2", deserializedEndpointDatum.getInputComponentExecutionIdentifier());
        assertEquals(compNodeId, deserializedEndpointDatum.getInputsNodeId());
        assertEquals("wf-exe-id-2", deserializedEndpointDatum.getWorkflowExecutionIdentifier());
        assertEquals(wfCtrlNodeId, deserializedEndpointDatum.getWorkflowNodeId());
        assertEquals(typedDatumMock, deserializedEndpointDatum.getValue());
    }

}
