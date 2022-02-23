/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccessStub;

/**
 * Test cases for {@link WorkflowExecutionControllerImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionControllerImplTest {

    private static final String COMP_EXE_ID_1 = "comp-exe-id-1";

    private WorkflowExecutionContext wfCtxMock;

    private ComponentStatesChangedEntirelyVerifier compStatesChangedVerifierMock;

    private ComponentDisconnectWatcher compLostWatcherMock;

    private WorkflowStateMachine wfStateMachineMock;

    private DistributedNotificationService notificationServiceMock;

    private NodeRestartWatcher nodeRestartWatcherMock;

    /**
     * Creates mock instances with default behavior used by {@link WorkflowExecutionControllerImpl}. Behavior will be overwritten in
     * particular test cases.
     */
    @Before
    public void setUp() {
        WorkflowDescription wfDescMock = EasyMock.createStrictMock(WorkflowDescription.class);
        EasyMock.expect(wfDescMock.getWorkflowNodes()).andStubReturn(new ArrayList<WorkflowNode>());
        EasyMock.expect(wfDescMock.removeWorkflowNodesAndRelatedConnectionsWithoutNotify(new ArrayList<WorkflowNode>()))
            .andStubReturn(new ArrayList<Connection>());
        EasyMock.expect(wfDescMock.clone()).andStubReturn(wfDescMock);
        EasyMock.replay(wfDescMock);

        wfCtxMock = EasyMock.createStrictMock(WorkflowExecutionContext.class);
        EasyMock.expect(wfCtxMock.getInstanceName()).andStubReturn("wf instance name");
        EasyMock.expect(wfCtxMock.getExecutionIdentifier()).andStubReturn("wf-exe-id");
        EasyMock.expect(wfCtxMock.getWorkflowDescription()).andStubReturn(wfDescMock);
        EasyMock.replay(wfCtxMock);

        compStatesChangedVerifierMock = EasyMock.createStrictMock(ComponentStatesChangedEntirelyVerifier.class);
        compStatesChangedVerifierMock.addListener(EasyMock.anyObject(ComponentStatesChangedEntirelyListener.class));
        EasyMock.replay(compStatesChangedVerifierMock);

        compLostWatcherMock = EasyMock.createStrictMock(ComponentDisconnectWatcher.class);
        EasyMock.replay(compLostWatcherMock);

        nodeRestartWatcherMock = EasyMock.createStrictMock(NodeRestartWatcher.class);
        EasyMock.replay(nodeRestartWatcherMock);

        WorkflowExecutionStorageBridge wfExeStorageBridgeMock = EasyMock.createStrictMock(WorkflowExecutionStorageBridge.class);
        EasyMock.replay(wfExeStorageBridgeMock);

        wfStateMachineMock = EasyMock.createStrictMock(WorkflowStateMachine.class);
        EasyMock.replay(wfStateMachineMock);

        WorkflowExecutionRelatedInstancesFactory wfExeInstancesFacMock =
            EasyMock.createStrictMock(WorkflowExecutionRelatedInstancesFactory.class);
        EasyMock
            .expect(wfExeInstancesFacMock.createComponentConsoleLogFileWriter(EasyMock.anyObject(WorkflowExecutionStorageBridge.class)))
            .andStubReturn(null);
        EasyMock.expect(wfExeInstancesFacMock.createComponentStatesEntirelyChangedVerifier(0))
            .andStubReturn(compStatesChangedVerifierMock);
        EasyMock.expect(wfExeInstancesFacMock.createComponentLostWatcher(wfCtxMock, compStatesChangedVerifierMock))
            .andStubReturn(compLostWatcherMock);
        EasyMock.expect(wfExeInstancesFacMock.createNodeRestartWatcher(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject()))
            .andStubReturn(nodeRestartWatcherMock);
        EasyMock.expect(wfExeInstancesFacMock.createWorkflowExecutionStorageBridge(wfCtxMock))
            .andStubReturn(wfExeStorageBridgeMock);
        EasyMock.expect(wfExeInstancesFacMock.createWorkflowStateMachine(EasyMock.anyObject(WorkflowStateMachineContext.class)))
            .andStubReturn(wfStateMachineMock);
        EasyMock.replay(wfExeInstancesFacMock);

        notificationServiceMock = EasyMock.createStrictMock(DistributedNotificationService.class);
        EasyMock.replay(notificationServiceMock);

        @SuppressWarnings("deprecation") WorkflowExecutionControllerImpl wfExeCtrlComp = new WorkflowExecutionControllerImpl();
        wfExeCtrlComp.bindWorkflowExecutionRelatedInstancesFactory(wfExeInstancesFacMock);
        wfExeCtrlComp.bindDistributedNotificationService(notificationServiceMock);
    }

    /**
     * Tests if component heartbeat are forwarded properly.
     */
    @Test
    public void testComponentHeartbeatForwarding() {

        EasyMock.reset(compLostWatcherMock);
        compLostWatcherMock.announceComponentHeartbeat(COMP_EXE_ID_1);
        EasyMock.replay(compLostWatcherMock);

        WorkflowExecutionControllerImpl wfExeCtrl = new WorkflowExecutionControllerImpl(wfCtxMock, new ServiceRegistryAccessStub(false));
        wfExeCtrl.onComponentHeartbeatReceived(COMP_EXE_ID_1);

        EasyMock.verify(compLostWatcherMock);
    }

    /**
     * Tests if lifecycle methods delegates requests properly to the underlying {@link WorkflowStateMachine}.
     */
    @Test
    public void testLifeCycleRequestDelegation() {
        WorkflowExecutionControllerImpl wfExeCtrl = new WorkflowExecutionControllerImpl(wfCtxMock, new ServiceRegistryAccessStub(false));

        Capture<WorkflowStateMachineEvent> wfStateMachineEveCapture = resetAndSetupWorkflowStateMachineMock();
        wfExeCtrl.start();
        verifyLifecycleRequestDelegation(wfStateMachineEveCapture, WorkflowStateMachineEventType.START_REQUESTED);

        wfStateMachineEveCapture = resetAndSetupWorkflowStateMachineMock();
        wfExeCtrl.pause();
        verifyLifecycleRequestDelegation(wfStateMachineEveCapture, WorkflowStateMachineEventType.PAUSE_REQUESTED);

        wfStateMachineEveCapture = resetAndSetupWorkflowStateMachineMock();
        wfExeCtrl.resume();
        verifyLifecycleRequestDelegation(wfStateMachineEveCapture, WorkflowStateMachineEventType.RESUME_REQUESTED);

        wfStateMachineEveCapture = resetAndSetupWorkflowStateMachineMock();
        wfExeCtrl.cancel();
        verifyLifecycleRequestDelegation(wfStateMachineEveCapture, WorkflowStateMachineEventType.CANCEL_REQUESTED);

        wfStateMachineEveCapture = resetAndSetupWorkflowStateMachineMock();
        wfExeCtrl.dispose();
        verifyLifecycleRequestDelegation(wfStateMachineEveCapture, WorkflowStateMachineEventType.DISPOSE_REQUESTED);
    }

    private Capture<WorkflowStateMachineEvent> resetAndSetupWorkflowStateMachineMock() {
        EasyMock.reset(wfStateMachineMock);
        Capture<WorkflowStateMachineEvent> wfStateMachineEveCapture = Capture.newInstance();
        wfStateMachineMock.postEvent(EasyMock.capture(wfStateMachineEveCapture));
        EasyMock.replay(wfStateMachineMock);
        return wfStateMachineEveCapture;
    }

    private void verifyLifecycleRequestDelegation(Capture<WorkflowStateMachineEvent> wfStateMachineEveCapture,
        WorkflowStateMachineEventType expectedType) {
        EasyMock.verify(wfStateMachineMock);
        assertTrue(wfStateMachineEveCapture.hasCaptured());
        assertEquals(expectedType, wfStateMachineEveCapture.getValue().getType());
    }

    /**
     * Tests if component state change announcements are processed properly.
     * 
     * @throws InterruptedException on unexpected error
     */
    @Test
    public void testComponentStateChangeProcessingInCaseOfNoFailure() throws InterruptedException {
        testComponentStateChangeProcessing(ComponentState.CANCELED);
    }

    /**
     * Tests if component state change announcements are processed properly in case of {@link ComponentState#FAILED}.
     * 
     * @throws InterruptedException on unexpected error
     */
    @Test
    public void testComponentStateChangeProcessingOnFailure() throws InterruptedException {
        EasyMock.reset(wfStateMachineMock);
        Capture<WorkflowStateMachineEvent> wfStateMachineEveCapture = Capture.newInstance();
        wfStateMachineMock.postEvent(EasyMock.capture(wfStateMachineEveCapture));
        EasyMock.replay(wfStateMachineMock);

        testComponentStateChangeProcessing(ComponentState.FAILED);

        EasyMock.verify(wfStateMachineMock);
        assertTrue(wfStateMachineEveCapture.hasCaptured());
        assertEquals(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED, wfStateMachineEveCapture.getValue().getType());

    }

    private void testComponentStateChangeProcessing(ComponentState compState) throws InterruptedException {
        final String executionCountOnReset = "3";

        EasyMock.reset(notificationServiceMock);
        Capture<String> notificationBodyCapture = Capture.newInstance(CaptureType.ALL);
        Capture<String> notificationIdCapture = Capture.newInstance(CaptureType.ALL);
        notificationServiceMock.send(EasyMock.capture(notificationIdCapture), EasyMock.capture(notificationBodyCapture));
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(notificationServiceMock);

        EasyMock.reset(compStatesChangedVerifierMock);
        compStatesChangedVerifierMock.addListener(EasyMock.anyObject(ComponentStatesChangedEntirelyListener.class));
        Capture<String> compExeIdCapture = Capture.newInstance();
        Capture<ComponentState> compStateCapture = Capture.newInstance();
        compStatesChangedVerifierMock.announceComponentState(EasyMock.capture(compExeIdCapture), EasyMock.capture(compStateCapture));
        EasyMock.replay(compStatesChangedVerifierMock);

        WorkflowExecutionControllerImpl wfExeCtrl = new WorkflowExecutionControllerImpl(wfCtxMock, new ServiceRegistryAccessStub(false));
        wfExeCtrl.onComponentStateChanged(COMP_EXE_ID_1, compState, 5, executionCountOnReset, null, null);

        final int waitForAsyncSendingMsec = 200;
        Thread.sleep(waitForAsyncSendingMsec);

        EasyMock.verify(notificationServiceMock);
        assertTrue(notificationIdCapture.hasCaptured());
        assertEquals(2, notificationIdCapture.getValues().size());
        assertTrue(notificationIdCapture.getValues().get(0).contains(COMP_EXE_ID_1));
        assertTrue(notificationIdCapture.getValues().get(1).contains(COMP_EXE_ID_1));
        assertEquals(2, notificationBodyCapture.getValues().size());
        assertEquals(compState.name(), notificationBodyCapture.getValues().get(0));
        assertEquals(executionCountOnReset, notificationBodyCapture.getValues().get(1));

        EasyMock.verify(compStatesChangedVerifierMock);

        assertTrue(compExeIdCapture.hasCaptured());
        assertEquals(COMP_EXE_ID_1, compExeIdCapture.getValue());
    }

}
