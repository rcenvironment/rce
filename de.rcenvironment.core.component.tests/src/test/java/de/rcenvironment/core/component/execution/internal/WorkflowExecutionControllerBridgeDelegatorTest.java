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

import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallbackService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link WorkflowExecutionControllerBridgeDelegator}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionControllerBridgeDelegatorTest {

    private static final String EXECEPTION_MESSAGE = "exeception message";

    private static final String SERIALIZED_TYPED_DATUM = "ser-td";

    private static final String ERR_ID = "err-id";

    private static final String ERR_MSG = "failure";

    private static final String EXE_COUNT_ON_RESET = "5";

    private static final int EXE_COUNT = 5;

    private static final ComponentState COMP_STATE = ComponentState.CANCELED;

    private static final String COMP_EXE_ID = "comp-exe-id";

    private static final String WF_EXE_ID = "wf-exe-id";

    private ConsoleRow[] consoleRows = new ConsoleRow[5];

    /**
     * Tests the callback {@link WorkflowExecutionControllerBridgeDelegator#onConsoleRowsProcessed(ConsoleRow[])} in success and failure
     * case.
     * 
     * @throws ExecutionControllerException on unexpected error
     * @throws RemoteOperationException on unexpected error
     */
    @Test
    public void testOnConsoleRowsProcessedCallback() throws ExecutionControllerException, RemoteOperationException {

        WorkflowExecutionControllerCallbackService wfExeCtrlBridgeMock =
            EasyMock.createStrictMock(WorkflowExecutionControllerCallbackService.class);
        wfExeCtrlBridgeMock.onConsoleRowsProcessed(WF_EXE_ID, consoleRows);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE)).times(1);
        EasyMock.replay(wfExeCtrlBridgeMock);

        Capture<ComponentStateMachineEvent> eventCapture = Capture.newInstance();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineMockExpectingFailureEvent(eventCapture);

        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator =
            new WorkflowExecutionControllerBridgeDelegator(createCompExeRelatedInstancesStub(createComponentExecutionMock(),
                compStateMachineMock, wfExeCtrlBridgeMock));

        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);

        wfExeCtrlBridgeDelegator.processConsoleRows(consoleRows);

        verifyWorkflowControllerNotReachable(eventCapture, compStateMachineMock, wfExeCtrlBridgeDelegator);

        // to ensure even if called twice, the failure is posted to the state machine only once
        wfExeCtrlBridgeDelegator.processConsoleRows(consoleRows);
        verifyMocks(wfExeCtrlBridgeMock, compStateMachineMock);
    }

    /**
     * Tests the callback
     * {@link WorkflowExecutionControllerBridgeDelegator#onComponentStateChanged(String, ComponentState, Integer, String, String, String)}
     * in success and failure case.
     * 
     * @throws ExecutionControllerException on unexpected error
     * @throws RemoteOperationException on unexpected error
     */
    @Test
    public void testOnComponentStateChangedCallback() throws ExecutionControllerException, RemoteOperationException {

        WorkflowExecutionControllerCallbackService wfExeCtrlBridgeMock =
            EasyMock.createStrictMock(WorkflowExecutionControllerCallbackService.class);
        wfExeCtrlBridgeMock.onComponentStateChanged(WF_EXE_ID, COMP_EXE_ID, COMP_STATE, EXE_COUNT, EXE_COUNT_ON_RESET, ERR_ID, ERR_MSG);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE)).times(1);
        EasyMock.replay(wfExeCtrlBridgeMock);

        Capture<ComponentStateMachineEvent> eventCapture = Capture.newInstance();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineMockExpectingFailureEvent(eventCapture);

        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator =
            new WorkflowExecutionControllerBridgeDelegator(createCompExeRelatedInstancesStub(createComponentExecutionMock(),
                compStateMachineMock, wfExeCtrlBridgeMock));

        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);

        wfExeCtrlBridgeDelegator.onComponentStateChanged(COMP_EXE_ID, COMP_STATE, EXE_COUNT, EXE_COUNT_ON_RESET, ERR_ID, ERR_MSG);

        verifyWorkflowControllerNotReachable(eventCapture, compStateMachineMock, wfExeCtrlBridgeDelegator);

        // to ensure even if called twice, the failure is posted to the state machine only once
        wfExeCtrlBridgeDelegator.onComponentStateChanged(COMP_EXE_ID, COMP_STATE, EXE_COUNT, EXE_COUNT_ON_RESET, ERR_ID, ERR_MSG);
        verifyMocks(wfExeCtrlBridgeMock, compStateMachineMock);
    }

    /**
     * Tests the callback {@link WorkflowExecutionControllerBridgeDelegator#onInputProcessed(String)} in success and failure case.
     * 
     * @throws ExecutionControllerException on unexpected error
     * @throws RemoteOperationException on unexpected error
     */
    @Test
    public void testOnInputProcessedCallback() throws ExecutionControllerException, RemoteOperationException {

        WorkflowExecutionControllerCallbackService wfExeCtrlBridgeMock =
            EasyMock.createStrictMock(WorkflowExecutionControllerCallbackService.class);
        wfExeCtrlBridgeMock.onInputProcessed(WF_EXE_ID, SERIALIZED_TYPED_DATUM);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE));
        wfExeCtrlBridgeMock.onInputProcessed(WF_EXE_ID, SERIALIZED_TYPED_DATUM);
        EasyMock.expectLastCall();
        wfExeCtrlBridgeMock.onInputProcessed(WF_EXE_ID, SERIALIZED_TYPED_DATUM);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE)).times(5);
        EasyMock.replay(wfExeCtrlBridgeMock);

        Capture<ComponentStateMachineEvent> eventCapture = Capture.newInstance();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineMockExpectingFailureEvent(eventCapture);

        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator =
            new WorkflowExecutionControllerBridgeDelegator(createCompExeRelatedInstancesStub(createComponentExecutionMock(),
                compStateMachineMock, wfExeCtrlBridgeMock));

        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);

        verifyWorkflowControllerNotReachable(eventCapture, compStateMachineMock, wfExeCtrlBridgeDelegator);

        wfExeCtrlBridgeDelegator.onInputProcessed(SERIALIZED_TYPED_DATUM);
        verifyMocks(wfExeCtrlBridgeMock, compStateMachineMock);
    }

    /**
     * Tests the callback {@link WorkflowExecutionControllerBridgeDelegator#onComponentHeartbeatReceived(String))} in success and failure
     * case.
     * 
     * @throws ExecutionControllerException on unexpected error
     * @throws RemoteOperationException on unexpected error
     */
    @Test
    public void testOnComponentHeartbeatReceivedCallback() throws ExecutionControllerException, RemoteOperationException {

        WorkflowExecutionControllerCallbackService wfExeCtrlBridgeMock =
            EasyMock.createStrictMock(WorkflowExecutionControllerCallbackService.class);
        wfExeCtrlBridgeMock.onComponentHeartbeatReceived(WF_EXE_ID, COMP_EXE_ID);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE));
        wfExeCtrlBridgeMock.onComponentHeartbeatReceived(WF_EXE_ID, COMP_EXE_ID);
        EasyMock.expectLastCall();
        wfExeCtrlBridgeMock.onComponentHeartbeatReceived(WF_EXE_ID, COMP_EXE_ID);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(EXECEPTION_MESSAGE)).times(5);
        EasyMock.replay(wfExeCtrlBridgeMock);

        Capture<ComponentStateMachineEvent> eventCapture = Capture.newInstance();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineMockExpectingFailureEvent(eventCapture);

        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator =
            new WorkflowExecutionControllerBridgeDelegator(createCompExeRelatedInstancesStub(createComponentExecutionMock(),
                compStateMachineMock, wfExeCtrlBridgeMock));

        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyWorkflowControllerReachable(eventCapture, wfExeCtrlBridgeDelegator);
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);

        verifyWorkflowControllerNotReachable(eventCapture, compStateMachineMock, wfExeCtrlBridgeDelegator);

        // to ensure even if called twice, the failure is posted to the state machine only once
        wfExeCtrlBridgeDelegator.onComponentHeartbeatReceived(COMP_EXE_ID);
        verifyMocks(wfExeCtrlBridgeMock, compStateMachineMock);
    }

    private void verifyMocks(WorkflowExecutionControllerCallbackService wfExeCtrlBridgeMock, ComponentStateMachine compStateMachineMock) {
        EasyMock.verify(wfExeCtrlBridgeMock);
        EasyMock.verify(compStateMachineMock);
    }

    private void verifyWorkflowControllerReachable(Capture<ComponentStateMachineEvent> eventCapture,
        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator) {
        assertTrue(wfExeCtrlBridgeDelegator.isWorkflowControllerReachable());
        assertFalse(eventCapture.hasCaptured());
    }

    private void verifyWorkflowControllerNotReachable(Capture<ComponentStateMachineEvent> eventCapture,
        ComponentStateMachine compStateMachineMock,
        WorkflowExecutionControllerBridgeDelegator wfExeCtrlBridgeDelegator) {

        assertFalse(wfExeCtrlBridgeDelegator.isWorkflowControllerReachable());
        assertTrue(eventCapture.hasCaptured());
        assertEquals(1, eventCapture.getValues().size());
        assertEquals(ComponentStateMachineEventType.WF_CRTL_CALLBACK_FAILED, eventCapture.getValues().get(0).getType());
    }

    private ComponentExecutionContext createComponentExecutionMock() {
        ComponentExecutionContext compExeCtxMock = EasyMock.createStrictMock(ComponentExecutionContext.class);
        EasyMock.expect(compExeCtxMock.getExecutionIdentifier()).andStubReturn(COMP_EXE_ID);
        EasyMock.expect(compExeCtxMock.getInstanceName()).andStubReturn("comp inst name");
        EasyMock.expect(compExeCtxMock.getWorkflowExecutionIdentifier()).andStubReturn(WF_EXE_ID);
        EasyMock.expect(compExeCtxMock.getWorkflowInstanceName()).andStubReturn("wf inst name");
        EasyMock.replay(compExeCtxMock);
        return compExeCtxMock;
    }

    private ComponentStateMachine createComponentStateMachineMockExpectingFailureEvent(Capture<ComponentStateMachineEvent> eventCapture) {
        ComponentStateMachine compStateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        compStateMachineMock.postEvent(EasyMock.capture(eventCapture));
        EasyMock.replay(compStateMachineMock);
        return compStateMachineMock;
    }

    private ComponentExecutionRelatedInstances createCompExeRelatedInstancesStub(ComponentExecutionContext compExeCtx,
        ComponentStateMachine compStateMachine, WorkflowExecutionControllerCallbackService wfExeCtrlBridge) {
        ComponentExecutionRelatedInstances compExeRelatedInstances = new ComponentExecutionRelatedInstances();
        compExeRelatedInstances.compExeCtx = compExeCtx;
        compExeRelatedInstances.compStateMachine = compStateMachine;
        compExeRelatedInstances.wfExeCtrlBridge = wfExeCtrlBridge;
        return compExeRelatedInstances;
    }

}
