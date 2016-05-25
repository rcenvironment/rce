/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.internal.InternalTDImpl.InternalTDType;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.component.testutils.ComponentExecutionContextDefaultStub;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumSerializer;
import de.rcenvironment.core.datamodel.testutils.TypedDatumConverterDefaultStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumSerializerDefaultStub;
import de.rcenvironment.core.datamodel.testutils.TypedDatumServiceDefaultStub;

/**
 * Test cases for {@link ComponentContextBridge}.
 * 
 * @author Doreen Seider
 */
public class ComponentContextBridgeTest {

    private static final Map<String, DataType> INPUT_DATA_TYPES = new HashMap<>();

    private static final String INPUT_1 = "input_1";

    private static final String INPUT_2 = "input_2";

    private static final DataType DATA_TYPE_1 = DataType.Integer;

    private static final DataType DATA_TYPE_2 = DataType.Float;

    private static final String OUTPUT_1 = "output_1";

    private static final String OUTPUT_2 = "output_2";

    private static final String TARGET_COMP_EXE_ID_1 = "comp-exe-id-target-1";

    private static final String TARGET_COMP_EXE_ID_2 = "comp-exe-id-target-2";

    private static final String TARGET_INPUT_1 = "input_1-target";

    private static final String TARGET_INPUT_2 = "input_2-target";
    
    private static final String SER_OUTPUT_TYPED_DATUM = "ser-output-datum";
    
    private TypedDatum outputTypedDatum;

    static {
        INPUT_DATA_TYPES.put(INPUT_1, DATA_TYPE_1);
        INPUT_DATA_TYPES.put(INPUT_2, DATA_TYPE_2);
    }

    /**
     * Bind services to the class under test.
     */
    @Before
    public void bindServices() {
        @SuppressWarnings("deprecation") ComponentContextBridge componentContextBridge = new ComponentContextBridge();
        componentContextBridge.bindTypedDatumService(new TypedDatumServiceDefaultStub() {
            @Override
            public TypedDatumConverter getConverter() {
                return new TypedDatumConverterDefaultStub() {

                    @Override
                    public TypedDatum castOrConvert(TypedDatum input, DataType targetType) throws DataTypeException {
                        if (input.getDataType() == DataType.Integer && targetType == DataType.Float) {
                            return createTypedDatumMock(targetType);
                        } else {
                            throw new RuntimeException("Unexpected parameters passed");
                        }
                    }
                };
            }
            @Override
            public TypedDatumSerializer getSerializer() {
                return new TypedDatumSerializerDefaultStub() {
                    @Override
                    public String serialize(TypedDatum input) {
                        if (input == outputTypedDatum) {
                            return SER_OUTPUT_TYPED_DATUM;
                        } else {
                            throw new RuntimeException("Unexpected parameters passed");            
                        }
                    }
                };
            }
        });
        
        outputTypedDatum = createTypedDatumMock(DataType.Integer);
    }

    /**
     * Tests if {@link EndpointDatum}s provided for components are set correctly.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testSetGetEndpointDatumsForExecution() throws ComponentExecutionException {

        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        assertTrue(compCtxBridge.getEndpointDatumsForExecution().isEmpty());
        assertTrue(compCtxBridge.getInputsWithDatum().isEmpty());

        Map<String, EndpointDatum> endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DATA_TYPE_1));

        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
        assertEquals(endpointDatums, compCtxBridge.getEndpointDatumsForExecution());

        Set<String> inputsWithDatum = compCtxBridge.getInputsWithDatum();
        assertEquals(1, inputsWithDatum.size());
        assertEquals(INPUT_1, inputsWithDatum.iterator().next());

        try {
            compCtxBridge.getEndpointDatumsForExecution().remove(INPUT_1);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_2, createEndpointDatumMock(INPUT_2, DATA_TYPE_2));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
        assertEquals(endpointDatums, compCtxBridge.getEndpointDatumsForExecution());

        // not convertible but valid data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DataType.NotAValue));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);

        // convertible data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_2, createEndpointDatumMock(INPUT_2, DataType.Integer));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);

        // invalid data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DataType.Float));
        try {
            compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
            fail("ComponentExecutionException expected");
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }
    }

    /**
     * Tests reading inputs. That includes conversion of {@link TypedDatum}s.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testReadInput() throws ComponentExecutionException {
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        try {
            compCtxBridge.readInput(INPUT_1);
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }

        TypedDatum typedDatum1 = createTypedDatumMock(DATA_TYPE_1);
        TypedDatum typedDatum2 = createTypedDatumMock(DATA_TYPE_2);

        Map<String, EndpointDatum> endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DATA_TYPE_1, typedDatum1));
        endpointDatums.put(INPUT_2, createEndpointDatumMock(INPUT_2, DATA_TYPE_2, typedDatum2));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);

        assertEquals(typedDatum1, compCtxBridge.readInput(INPUT_1));
        assertEquals(typedDatum2, compCtxBridge.readInput(INPUT_2));

        // not convertible but valid data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DATA_TYPE_1, createTypedDatumMock(DataType.NotAValue)));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
        assertEquals(DataType.NotAValue, compCtxBridge.readInput(INPUT_1).getDataType());

        // convertible data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_2, createEndpointDatumMock(INPUT_2, DATA_TYPE_2, createTypedDatumMock(DataType.Integer)));
        compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
        assertEquals(DataType.Float, compCtxBridge.readInput(INPUT_2).getDataType());

        // invalid data type
        endpointDatums = new HashMap<>();
        endpointDatums.put(INPUT_1, createEndpointDatumMock(INPUT_1, DATA_TYPE_1, createTypedDatumMock(DataType.Float)));
        try {
            compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
            fail("ComponentExecutionException expected");
        } catch (ComponentExecutionException e) {
            assertTrue(true);
        }
        try {
            compCtxBridge.readInput(INPUT_1);
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    /**
     * Tests writing a value to an output.
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testWriteOutput() throws ComponentExecutionException {
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);
        
        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        Capture<String> outputNameCapture = new Capture<>();
        Capture<String> serDatumCapture = new Capture<>();
        EasyMock.expect(compExeStorageBridgeMock.addOutput(EasyMock.capture(outputNameCapture), EasyMock.capture(serDatumCapture)))
            .andReturn(Long.valueOf(8));
        Long dmId = Long.valueOf(5);
        EasyMock.expect(compExeStorageBridgeMock.addOutput(EasyMock.capture(outputNameCapture), EasyMock.capture(serDatumCapture)))
            .andReturn(dmId);
        EasyMock.replay(compExeStorageBridgeMock);
        compExeRelatedInstances.compExeStorageBridge = compExeStorageBridgeMock;
        
        compCtxBridge.writeOutput(OUTPUT_1, outputTypedDatum);
        assertEquals(OUTPUT_1, outputNameCapture.getValue());
        assertEquals(SER_OUTPUT_TYPED_DATUM, serDatumCapture.getValue());
        
        TypedDatumToOutputWriter typedDatumToOutputWriterMock = EasyMock.createStrictMock(TypedDatumToOutputWriter.class);
        Capture<String> nameCapture = new Capture<>();
        Capture<TypedDatum> datumToSendCapture = new Capture<>();
        Capture<Long> dmIdCapture = new Capture<>();
        typedDatumToOutputWriterMock.writeTypedDatumToOutput(EasyMock.capture(nameCapture),
            EasyMock.capture(datumToSendCapture), EasyMock.captureLong(dmIdCapture));
        EasyMock.replay(typedDatumToOutputWriterMock);
        compExeRelatedInstances.typedDatumToOutputWriter = typedDatumToOutputWriterMock;
        
        compCtxBridge.writeOutput(OUTPUT_2, outputTypedDatum);
        assertEquals(OUTPUT_2, nameCapture.getValue());
        assertEquals(outputTypedDatum, datumToSendCapture.getValue());
        assertEquals(dmId, dmIdCapture.getValue());
        
        EasyMock.verify(compExeStorageBridgeMock);
        
        // TODO test close is reset to false and not-a-value is handled properly
    }

    /**
     * Tests resetting output.
     * 
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testResetOutput() throws ComponentExecutionException {
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        compExeRelatedInstances.isNestedLoopDriver = false;
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);
        try {
            compCtxBridge.resetOutput(OUTPUT_1);
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            assertTrue(true);
        }
        compExeRelatedInstances.isNestedLoopDriver = true;

        Deque<WorkflowGraphHop> hops1 = new LinkedList<>();
        hops1.add(createWorkflowGraphHopMock(TARGET_COMP_EXE_ID_1, TARGET_INPUT_1));
        Deque<WorkflowGraphHop> hops2 = new LinkedList<>();
        hops2.add(createWorkflowGraphHopMock(TARGET_COMP_EXE_ID_2, TARGET_INPUT_2));
        Set<Deque<WorkflowGraphHop>> hopsSet = new HashSet<>();
        hopsSet.add(hops1);
        hopsSet.add(hops2);
        Map<String, Set<Deque<WorkflowGraphHop>>> hops = new HashMap<>();
        hops.put(OUTPUT_1, hopsSet);

        final WorkflowGraph wfGraphMock = EasyMock.createStrictMock(WorkflowGraph.class);
        EasyMock.expect(wfGraphMock.getHopsToTraverseWhenResetting(ComponentExecutionContextDefaultStub.COMP_EXE_ID)).andStubReturn(hops);
        EasyMock.replay(wfGraphMock);

        compExeRelatedInstances.compExeCtx = new ComponentExecutionContextDefaultStub() {

            private static final long serialVersionUID = 3735092511582905564L;

            @Override
            public WorkflowGraph getWorkflowGraph() {
                return wfGraphMock;
            }
        };
        ComponentExecutionScheduler compExeSchedulerMock = EasyMock.createStrictMock(ComponentExecutionScheduler.class);
        compExeSchedulerMock.addResetDataIdSent(EasyMock.anyObject(String.class));
        compExeSchedulerMock.addResetDataIdSent(EasyMock.anyObject(String.class));
        EasyMock.replay(compExeSchedulerMock);
        compExeRelatedInstances.compExeScheduler = compExeSchedulerMock;

        TypedDatumToOutputWriter typedDatumToOutputWriterMock = EasyMock.createStrictMock(TypedDatumToOutputWriter.class);
        Capture<String> outputNameCapture = new Capture<>(CaptureType.ALL);
        Capture<TypedDatum> datumToSendCapture = new Capture<>(CaptureType.ALL);
        Capture<String> inputCompExeIdCapture = new Capture<>(CaptureType.ALL);
        Capture<String> inputNameCapture = new Capture<>(CaptureType.ALL);
        typedDatumToOutputWriterMock.writeTypedDatumToOutputConsideringOnlyCertainInputs(EasyMock.capture(outputNameCapture),
            EasyMock.capture(datumToSendCapture), EasyMock.capture(inputCompExeIdCapture), EasyMock.capture(inputNameCapture));
        typedDatumToOutputWriterMock.writeTypedDatumToOutputConsideringOnlyCertainInputs(EasyMock.capture(outputNameCapture),
            EasyMock.capture(datumToSendCapture), EasyMock.capture(inputCompExeIdCapture), EasyMock.capture(inputNameCapture));
        EasyMock.replay(typedDatumToOutputWriterMock);
        compExeRelatedInstances.typedDatumToOutputWriter = typedDatumToOutputWriterMock;

        compCtxBridge.resetOutput(OUTPUT_1);
        EasyMock.verify(compExeSchedulerMock);
        EasyMock.verify(typedDatumToOutputWriterMock);
        List<String> outputNames = outputNameCapture.getValues();
        assertEquals(2, outputNames.size());
        assertTrue(outputNames.remove(OUTPUT_1));
        assertTrue(outputNames.remove(OUTPUT_1));
        assertEquals(0, outputNames.size());

        List<TypedDatum> datumsToSend = datumToSendCapture.getValues();
        assertEquals(2, datumsToSend.size());
        assertEquals(DataType.Internal, datumsToSend.get(0).getDataType());
        assertEquals(InternalTDType.NestedLoopReset, ((InternalTDImpl) datumsToSend.get(0)).getType());
        assertEquals(InternalTDType.NestedLoopReset, ((InternalTDImpl) datumsToSend.get(1)).getType());

        List<String> inputCompExeIds = inputCompExeIdCapture.getValues();
        assertEquals(2, inputCompExeIds.size());
        assertTrue(inputCompExeIds.remove(TARGET_COMP_EXE_ID_1));
        assertTrue(inputCompExeIds.remove(TARGET_COMP_EXE_ID_2));
        assertEquals(0, inputCompExeIds.size());

        List<String> inputNames = inputNameCapture.getValues();
        assertEquals(2, inputNames.size());
        assertTrue(inputNames.remove(TARGET_INPUT_1));
        assertTrue(inputNames.remove(TARGET_INPUT_2));
        assertEquals(0, inputNames.size());
    }

    /**
     * Tests writing history data.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws IOException on unexpected error
     */
    @Test
    public void testWriteHistoryData() throws ComponentExecutionException, IOException {
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeRelatedStates = new ComponentExecutionRelatedStates();
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        Capture<String> logMessageCapture = new Capture<>();
        Capture<ConsoleRow.Type> consoleRowTypeCatpure = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(logMessageCapture, consoleRowTypeCatpure);
        compExeRelatedInstances.consoleRowsSender = consoleRowsSenderMock;

        compCtxBridge.writeFinalHistoryDataItem(null);
        EasyMock.verify(consoleRowsSenderMock);
        assertEquals(ConsoleRow.Type.COMPONENT_ERROR, consoleRowTypeCatpure.getValue());
        assertFalse(compExeRelatedInstances.compExeRelatedStates.finalHistoryDataItemWritten.get());

        logMessageCapture = new Capture<>();
        consoleRowTypeCatpure = new Capture<>();
        consoleRowsSenderMock = createConsoleRowsSenderMock(logMessageCapture, consoleRowTypeCatpure);
        compExeRelatedInstances.consoleRowsSender = consoleRowsSenderMock;

        compCtxBridge.writeIntermediateHistoryData(null);
        EasyMock.verify(consoleRowsSenderMock);
        assertEquals(ConsoleRow.Type.COMPONENT_ERROR, consoleRowTypeCatpure.getValue());
        assertFalse(compExeRelatedInstances.compExeRelatedStates.intermediateHistoryDataWritten.get());

        EasyMock.reset(consoleRowsSenderMock);
        EasyMock.replay(consoleRowsSenderMock);

        String serCompHistoryDataItem = "ser-comp-history-data-item";

        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        Capture<String> serCompHistoryDataItemCapture = new Capture<>(CaptureType.ALL);
        compExeStorageBridgeMock.setOrUpdateHistoryDataItem(EasyMock.capture(serCompHistoryDataItemCapture));
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(compExeStorageBridgeMock);
        compExeRelatedInstances.compExeStorageBridge = compExeStorageBridgeMock;

        ComponentHistoryDataItem compHistoryDataItemMock = EasyMock.createStrictMock(ComponentHistoryDataItem.class);
        EasyMock.expect(compHistoryDataItemMock.serialize(EasyMock.anyObject(TypedDatumSerializer.class)))
            .andStubReturn(serCompHistoryDataItem);
        EasyMock.replay(compHistoryDataItemMock);

        compCtxBridge.writeFinalHistoryDataItem(compHistoryDataItemMock);
        assertTrue(compExeRelatedInstances.compExeRelatedStates.finalHistoryDataItemWritten.get());
        
        assertEquals(serCompHistoryDataItem, serCompHistoryDataItemCapture.getValues().get(0));
        
        compCtxBridge.writeIntermediateHistoryData(compHistoryDataItemMock);
        assertTrue(compExeRelatedInstances.compExeRelatedStates.intermediateHistoryDataWritten.get());
        
        assertEquals(serCompHistoryDataItem, serCompHistoryDataItemCapture.getValues().get(1));
        
        EasyMock.verify(consoleRowsSenderMock);
        EasyMock.verify(compExeStorageBridgeMock);
    }

    private ConsoleRowsSender createConsoleRowsSenderMock(Capture<String> logMessageCapture,
        Capture<ConsoleRow.Type> consoleRowTypeCatpure) {
        ConsoleRowsSender consoleRowsSenderMock = EasyMock.createStrictMock(ConsoleRowsSender.class);
        consoleRowsSenderMock.sendLogMessageAsConsoleRow(EasyMock.capture(consoleRowTypeCatpure), EasyMock.capture(logMessageCapture));
        EasyMock.replay(consoleRowsSenderMock);
        return consoleRowsSenderMock;
    }

    /**
     * Tests closing outputs.
     */
    @Test
    public void testCloseOutput() {

        TypedDatumToOutputWriter typedDatumToOutputWriterMock = EasyMock.createStrictMock(TypedDatumToOutputWriter.class);
        Capture<String> outputNameCapture = new Capture<>();
        Capture<TypedDatum> typedDatumCapture = new Capture<>();
        typedDatumToOutputWriterMock.writeTypedDatumToOutput(EasyMock.capture(outputNameCapture), EasyMock.capture(typedDatumCapture));
        EasyMock.replay(typedDatumToOutputWriterMock);

        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        compExeRelatedInstances.typedDatumToOutputWriter = typedDatumToOutputWriterMock;
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        assertFalse(compCtxBridge.isOutputClosed(OUTPUT_1));
        compCtxBridge.closeOutput(OUTPUT_1);
        assertTrue(compCtxBridge.isOutputClosed(OUTPUT_1));

        EasyMock.verify(typedDatumToOutputWriterMock);
        assertEquals(OUTPUT_1, outputNameCapture.getValue());
        assertEquals(DataType.Internal, typedDatumCapture.getValue().getDataType());
        assertEquals(InternalTDImpl.InternalTDType.WorkflowFinish, ((InternalTDImpl) typedDatumCapture.getValue()).getType());

        EasyMock.reset(typedDatumToOutputWriterMock);
        EasyMock.replay(typedDatumToOutputWriterMock);

        Capture<String> logMessageCapture = new Capture<>();
        Capture<ConsoleRow.Type> consoleRowTypeCatpure = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = EasyMock.createStrictMock(ConsoleRowsSender.class);
        consoleRowsSenderMock.sendLogMessageAsConsoleRow(EasyMock.capture(consoleRowTypeCatpure), EasyMock.capture(logMessageCapture));
        EasyMock.replay(consoleRowsSenderMock);
        compExeRelatedInstances.consoleRowsSender = consoleRowsSenderMock;

        compCtxBridge.closeOutput(OUTPUT_1);
        EasyMock.verify(typedDatumToOutputWriterMock);
        EasyMock.verify(consoleRowsSenderMock);

        assertEquals(ConsoleRow.Type.COMPONENT_WARN, consoleRowTypeCatpure.getValue());
    }

    /**
     * Tests closing all outputs.
     */
    @Test
    public void testCloseAllOutputs() {

        TypedDatumToOutputWriter typedDatumToOutputWriterMock = EasyMock.createStrictMock(TypedDatumToOutputWriter.class);
        Capture<String> outputNameCapture1 = new Capture<>();
        Capture<TypedDatum> typedDatumCapture1 = new Capture<>();
        Capture<String> outputNameCapture2 = new Capture<>();
        Capture<TypedDatum> typedDatumCapture2 = new Capture<>();
        typedDatumToOutputWriterMock.writeTypedDatumToOutput(EasyMock.capture(outputNameCapture1), EasyMock.capture(typedDatumCapture1));
        typedDatumToOutputWriterMock.writeTypedDatumToOutput(EasyMock.capture(outputNameCapture2), EasyMock.capture(typedDatumCapture2));
        EasyMock.replay(typedDatumToOutputWriterMock);

        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        compExeRelatedInstances.typedDatumToOutputWriter = typedDatumToOutputWriterMock;
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        assertFalse(compCtxBridge.isOutputClosed(OUTPUT_1));
        assertFalse(compCtxBridge.isOutputClosed(OUTPUT_2));
        compCtxBridge.closeAllOutputs();
        assertTrue(compCtxBridge.isOutputClosed(OUTPUT_1));
        assertTrue(compCtxBridge.isOutputClosed(OUTPUT_2));

        EasyMock.verify(typedDatumToOutputWriterMock);

        Set<String> expectedOutputs = new HashSet<>();
        expectedOutputs.add(OUTPUT_1);
        expectedOutputs.add(OUTPUT_2);
        expectedOutputs.remove(outputNameCapture1.getValue());
        expectedOutputs.remove(outputNameCapture2.getValue());
        assertEquals(0, expectedOutputs.size());
        assertEquals(DataType.Internal, typedDatumCapture1.getValue().getDataType());
        assertEquals(InternalTDImpl.InternalTDType.WorkflowFinish, ((InternalTDImpl) typedDatumCapture1.getValue()).getType());

        assertEquals(DataType.Internal, typedDatumCapture2.getValue().getDataType());
        assertEquals(InternalTDImpl.InternalTDType.WorkflowFinish, ((InternalTDImpl) typedDatumCapture2.getValue()).getType());
    }

    /**
     * Tests getting the execution count.
     */
    @Test
    public void testGetExecutionCount() {
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        ComponentExecutionRelatedStates compExeRelatedStates = new ComponentExecutionRelatedStates();
        compExeRelatedStates.executionCount.set(8);
        compExeRelatedInstances.compExeRelatedStates = compExeRelatedStates;
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        assertEquals(compExeRelatedInstances.compExeRelatedStates.executionCount.get(), compCtxBridge.getExecutionCount());
    }

    /**
     * Tests printing messages to the workflow console and related files in the data management.
     */
    @Test
    public void testPrintConsoleRow() {
        Capture<String> logMessageCapture = new Capture<>();
        Capture<ConsoleRow.Type> consoleRowTypeCatpure = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = EasyMock.createStrictMock(ConsoleRowsSender.class);
        consoleRowsSenderMock.sendLogMessageAsConsoleRow(EasyMock.capture(consoleRowTypeCatpure), EasyMock.capture(logMessageCapture));
        EasyMock.replay(consoleRowsSenderMock);

        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances();
        compExeRelatedInstances.consoleRowsSender = consoleRowsSenderMock;
        ComponentContextBridge compCtxBridge = new ComponentContextBridge(compExeRelatedInstances);

        String message = "some log message";
        compCtxBridge.printConsoleRow(message, ConsoleRow.Type.COMPONENT_WARN);

        EasyMock.verify(consoleRowsSenderMock);

        assertEquals(1, consoleRowTypeCatpure.getValues().size());
        assertEquals(ConsoleRow.Type.COMPONENT_WARN, consoleRowTypeCatpure.getValue());
        assertEquals(1, logMessageCapture.getValues().size());
        assertEquals(message, logMessageCapture.getValue());

        EasyMock.reset(consoleRowsSenderMock);

        logMessageCapture = new Capture<>();
        consoleRowTypeCatpure = new Capture<>();
        consoleRowsSenderMock.sendTimelineEventAsConsoleRow(EasyMock.capture(consoleRowTypeCatpure), EasyMock.capture(logMessageCapture));
        EasyMock.replay(consoleRowsSenderMock);
        compExeRelatedInstances.consoleRowsSender = consoleRowsSenderMock;

        compCtxBridge.printConsoleRow(ConsoleRow.WorkflowLifecyleEventType.TOOL_STARTING.name(), ConsoleRow.Type.LIFE_CYCLE_EVENT);

        EasyMock.verify(consoleRowsSenderMock);

        assertEquals(1, consoleRowTypeCatpure.getValues().size());
        assertEquals(ConsoleRow.Type.LIFE_CYCLE_EVENT, consoleRowTypeCatpure.getValue());
        assertEquals(1, logMessageCapture.getValues().size());
        assertEquals(ConsoleRow.WorkflowLifecyleEventType.TOOL_STARTING.name(), logMessageCapture.getValue());
    }

    private ComponentExecutionRelatedInstances createComponentExecutionRelatedInstances() {

        Map<String, EndpointDescription> inputDescriptions = new HashMap<>();
        inputDescriptions.put(INPUT_1, createEndpointDescriptionMock(INPUT_1, DATA_TYPE_1));
        inputDescriptions.put(INPUT_2, createEndpointDescriptionMock(INPUT_2, DATA_TYPE_2));
        EndpointDescriptionsManager inputDescManagerMock = createEndpointDescriptionManagerMock(inputDescriptions);

        Map<String, EndpointDescription> outputDescriptions = new HashMap<>();
        outputDescriptions.put(OUTPUT_1, createEndpointDescriptionMock(OUTPUT_1, DATA_TYPE_1));
        outputDescriptions.put(OUTPUT_2, createEndpointDescriptionMock(OUTPUT_2, DATA_TYPE_2, true));
        EndpointDescriptionsManager outputDescManagerMock = createEndpointDescriptionManagerMock(outputDescriptions);

        final ComponentDescription compDescMock = EasyMock.createStrictMock(ComponentDescription.class);
        EasyMock.expect(compDescMock.getInputDescriptionsManager()).andStubReturn(inputDescManagerMock);
        EasyMock.expect(compDescMock.getOutputDescriptionsManager()).andStubReturn(outputDescManagerMock);
        EasyMock.replay(compDescMock);

        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = new ComponentExecutionContextDefaultStub() {

            private static final long serialVersionUID = 8174990386242526783L;

            public ComponentDescription getComponentDescription() {
                return compDescMock;
            }
        };

        return compExeRelatedInstances;
    }

    private WorkflowGraphHop createWorkflowGraphHopMock(String targetCompExeId, String targetInputName) {
        WorkflowGraphHop wfGraphMock = EasyMock.createStrictMock(WorkflowGraphHop.class);
        EasyMock.expect(wfGraphMock.getTargetExecutionIdentifier()).andStubReturn(targetCompExeId);
        EasyMock.expect(wfGraphMock.getTargetInputName()).andStubReturn(targetInputName);
        EasyMock.replay(wfGraphMock);
        return wfGraphMock;
    }

    private EndpointDescriptionsManager createEndpointDescriptionManagerMock(Map<String, EndpointDescription> epDescriptions) {
        EndpointDescriptionsManager epDescManagerMock = EasyMock.createStrictMock(EndpointDescriptionsManager.class);
        EasyMock.expect(epDescManagerMock.getEndpointDescriptions()).andStubReturn(new HashSet<>(epDescriptions.values()));
        for (String name : epDescriptions.keySet()) {
            EasyMock.expect(epDescManagerMock.getEndpointDescription(name)).andStubReturn(epDescriptions.get(name));
        }
        EasyMock.replay(epDescManagerMock);
        return epDescManagerMock;
    }

    private EndpointDescription createEndpointDescriptionMock(String name, DataType dataType) {
        return createEndpointDescriptionMock(name, dataType, false);
    }
    private EndpointDescription createEndpointDescriptionMock(String name, DataType dataType, boolean connected) {
        EndpointDescription epDescMock = EasyMock.createStrictMock(EndpointDescription.class);
        EasyMock.expect(epDescMock.getName()).andStubReturn(name);
        EasyMock.expect(epDescMock.getDataType()).andStubReturn(dataType);
        EasyMock.expect(epDescMock.isConnected()).andStubReturn(connected);
        EasyMock.replay(epDescMock);
        return epDescMock;
    }

    private TypedDatum createTypedDatumMock(DataType dataType) {
        TypedDatum typedDatumMock = EasyMock.createStrictMock(TypedDatum.class);
        EasyMock.expect(typedDatumMock.getDataType()).andStubReturn(dataType);
        EasyMock.replay(typedDatumMock);
        return typedDatumMock;
    }

    private EndpointDatum createEndpointDatumMock(String inputName, DataType dataType) {
        return createEndpointDatumMock(inputName, dataType, createTypedDatumMock(dataType));
    }

    private EndpointDatum createEndpointDatumMock(String inputName, DataType dataType, TypedDatum typedDatum) {
        EndpointDatum endpointDatumMock = EasyMock.createStrictMock(EndpointDatum.class);
        EasyMock.expect(endpointDatumMock.getInputName()).andStubReturn(inputName);
        EasyMock.expect(endpointDatumMock.getValue()).andStubReturn(typedDatum);
        EasyMock.replay(endpointDatumMock);
        return endpointDatumMock;
    }
}
