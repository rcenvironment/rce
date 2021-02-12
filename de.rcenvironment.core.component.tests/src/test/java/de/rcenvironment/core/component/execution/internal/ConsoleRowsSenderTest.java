/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.BatchingConsoleRowsForwarder;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.testutils.ComponentExecutionContextDefaultStub;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Test cases for {@link ConsoleRowsSender}.
 * 
 * @author Doreen Seider
 */
public class ConsoleRowsSenderTest {
    
    private static final Long DM_ID = Long.valueOf(7);
    
    private static final int CONSOLE_ROW_SEQ_NUMBER = 8;
    
    private static final int EXE_COUNT = 9;

    private BatchingConsoleRowsForwarder batchingConsoleRowForwarderMock;

    /**
     * Tests if the trigger for writing log files to the data management is done properly. That includes that certain flags are set properly
     * afterwards.
     */
    @Test
    public void testSendLogFileWriteTriggerAsConsoleRow() {
        
        Capture<ConsoleRow> consoleRowCapture = Capture.newInstance();
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances(consoleRowCapture);
        ConsoleRowsSender consoleRowsSender = new ConsoleRowsSender(compExeRelatedInstances);
        
        consoleRowsSender.sendLogFileWriteTriggerAsConsoleRow();
        
        ConsoleRow capturedConsoleRow = verifyAfterConsoleRowSent(consoleRowCapture, ConsoleRow.Type.LIFE_CYCLE_EVENT);
        
        String[] payload = StringUtils.splitAndUnescape(capturedConsoleRow.getPayload());
        assertEquals(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_LOG_FINISHED.name(), payload[0]);
        assertEquals(String.valueOf(DM_ID), payload[1]);
        assertEquals(String.valueOf(EXE_COUNT), payload[2]);
        
        assertEquals(0, compExeRelatedInstances.compExeRelatedStates.consoleRowSequenceNumber.get());
        assertFalse(compExeRelatedInstances.compExeRelatedStates.compHasSentConsoleRowLogMessages.get());
        
    }
    
    /**
     * Tests if a log message is sent as expected.
     */
    @Test
    public void testSendLogMessageAsConsoleRow() {
        Capture<ConsoleRow> consoleRowCapture = Capture.newInstance();
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances(consoleRowCapture);
        ConsoleRowsSender consoleRowsSender = new ConsoleRowsSender(compExeRelatedInstances);
        
        String payload = "some message";
        int compRun = 5;
        consoleRowsSender.sendLogMessageAsConsoleRow(ConsoleRow.Type.TOOL_ERROR, payload, compRun);
        
        ConsoleRow capturedConsoleRow = verifyAfterConsoleRowSent(consoleRowCapture, ConsoleRow.Type.TOOL_ERROR);
        assertEquals(payload, capturedConsoleRow.getPayload());
        assertEquals(compRun, capturedConsoleRow.getComponentRun());
    }
    
    /**
     * Tests if a log message is sent as expected.
     */
    @Test
    public void testSendTimelineEventAsConsoleRow() {
        Capture<ConsoleRow> consoleRowCapture = Capture.newInstance();
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances(consoleRowCapture);
        ConsoleRowsSender consoleRowsSender = new ConsoleRowsSender(compExeRelatedInstances);
        
        consoleRowsSender.sendTimelineEventAsConsoleRow(ConsoleRow.Type.LIFE_CYCLE_EVENT, WorkflowLifecyleEventType.TOOL_STARTING.name());
        
        ConsoleRow capturedConsoleRow = verifyAfterConsoleRowSent(consoleRowCapture, ConsoleRow.Type.LIFE_CYCLE_EVENT);
        String[] payload = StringUtils.splitAndUnescape(capturedConsoleRow.getPayload());
        assertEquals(payload[0], WorkflowLifecyleEventType.TOOL_STARTING.name());
        assertEquals(payload[1], String.valueOf(DM_ID));
    }
    
    /**
     * Tests if a {@link ComponentState} is sent as expected.
     */
    @Test
    public void testSendStateAsConsoleRow() {
        Capture<ConsoleRow> consoleRowCapture = Capture.newInstance();
        ComponentExecutionRelatedInstances compExeRelatedInstances = createComponentExecutionRelatedInstances(consoleRowCapture);
        ConsoleRowsSender consoleRowsSender = new ConsoleRowsSender(compExeRelatedInstances);
        
        consoleRowsSender.sendStateAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED);
        
        ConsoleRow capturedConsoleRow = verifyAfterConsoleRowSent(consoleRowCapture, ConsoleRow.Type.LIFE_CYCLE_EVENT);
        assertEquals(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED.name(), capturedConsoleRow.getPayload());
    }
    
    private ConsoleRow verifyAfterConsoleRowSent(Capture<ConsoleRow> consoleRowCapture, ConsoleRow.Type expectedType) {
        EasyMock.verify(batchingConsoleRowForwarderMock);
        assertTrue(consoleRowCapture.hasCaptured());
        assertEquals(1, consoleRowCapture.getValues().size());
        
        ConsoleRow capturedConsoleRow = consoleRowCapture.getValue();
        assertEquals(ComponentExecutionContextDefaultStub.COMP_EXE_ID, capturedConsoleRow.getComponentIdentifier());
        assertEquals(ComponentExecutionContextDefaultStub.WF_EXE_ID, capturedConsoleRow.getWorkflowIdentifier());
        assertEquals(ComponentExecutionContextDefaultStub.COMP_INSTANCE_NAME, capturedConsoleRow.getComponentName());
        assertEquals(ComponentExecutionContextDefaultStub.WF_INSTANCE_NAME, capturedConsoleRow.getWorkflowName());
        assertEquals(expectedType, capturedConsoleRow.getType());
        
        return capturedConsoleRow;
    }
    
    private ComponentExecutionRelatedInstances createComponentExecutionRelatedInstances(Capture<ConsoleRow> consoleRowCapture) {
        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        EasyMock.expect(compExeStorageBridgeMock.getComponentExecutionDataManagementId()).andStubReturn(DM_ID);
        EasyMock.replay(compExeStorageBridgeMock);
        
        ComponentExecutionRelatedStates compExeRelatedStates = new ComponentExecutionRelatedStates();
        compExeRelatedStates.executionCount.set(EXE_COUNT);
        compExeRelatedStates.consoleRowSequenceNumber.set(CONSOLE_ROW_SEQ_NUMBER);
        compExeRelatedStates.compHasSentConsoleRowLogMessages.set(true);
        
        batchingConsoleRowForwarderMock = EasyMock.createStrictMock(BatchingConsoleRowsForwarder.class);
        batchingConsoleRowForwarderMock.onConsoleRow(EasyMock.capture(consoleRowCapture));
        EasyMock.replay(batchingConsoleRowForwarderMock);
        
        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeStorageBridge = compExeStorageBridgeMock;
        compExeRelatedInstances.compExeCtx = new ComponentExecutionContextDefaultStub();
        compExeRelatedInstances.compExeRelatedStates = compExeRelatedStates;
        compExeRelatedInstances.batchingConsoleRowsForwarder = batchingConsoleRowForwarderMock;
        
        return compExeRelatedInstances;
    }

}
