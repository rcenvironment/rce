/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ScheduledFuture;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContextBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

public class SynchronousWorkflowExecutionServiceImplTest {

    /** Maximal time between two heartbeats. */
    private static final int HEARTBEAT_TIMEOUT = 20000;

    private Capture<Runnable> heartbeatChecker = Capture.newInstance(CaptureType.FIRST);

    private AsyncTaskService taskService;

    private SynchronousWorkflowExecutionServiceImplUnderTest service;

    private WorkflowExecutionService mockExecutionService;
    
    private DistributedNotificationService notificationService;

    private boolean workflowExecutionSucceeded;

    private Capture<WorkflowStateNotificationHandler> workflowStateNotificationHandler = Capture.newInstance(CaptureType.FIRST);

    @Before
    public void initializeWorkflowExecutionService() throws RemoteOperationException {
        this.service = new SynchronousWorkflowExecutionServiceImplUnderTest();

        createAndBindTaskService();
        createAndBindNotificationService();
        createAndBindWorkflowExecutionService();
    }

    private void createAndBindTaskService() {
        taskService = EasyMock.createMock(AsyncTaskService.class);
        service.bindTaskService(taskService);
    }

    private void createAndBindNotificationService()
        throws RemoteOperationException {
        this.notificationService = EasyMock.createMock(DistributedNotificationService.class);

        service.bindDistributedNotificationService(notificationService);
    }

    private void expectNotificationSubscription() throws RemoteOperationException {
        EasyMock.expect(notificationService.subscribe(
            EasyMock.eq(notificationId(someExecutionIdentifier())),
            EasyMock.capture(workflowStateNotificationHandler),
            EasyMock.eq(null))).andStubReturn(null);
        EasyMock.replay(notificationService);
    }

    private String someExecutionIdentifier() {
        return "some execution identifier";
    }

    private String notificationId(String executionIdentifier) {
        return WorkflowConstants.STATE_NOTIFICATION_ID + executionIdentifier;
    }

    private void createAndBindWorkflowExecutionService() {
        this.mockExecutionService = EasyMock.createMock(WorkflowExecutionService.class);
        service.bindWorkflowExecutionService(mockExecutionService);
    }

    @Test
    public void whenWorkflowStartFailsThenComponentExceptionIsThrown() {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();

        final Exception exceptionToThrow = new WorkflowExecutionException("Some exception message");
        expectWorkflowStartAndThrowException(contextToExecute, exceptionToThrow);

        ComponentException thrownException = null;
        try {
            service.executeWorkflow(contextToExecute);
        } catch (ComponentException e) {
            thrownException = e;
        }

        assertNotNull("Expected a ComponentException to be thrown, but none was caught", thrownException);
        assertEquals(exceptionToThrow, thrownException.getCause());
    }

    @Test
    public void whenWorkflowTerminatesBeforeFirstHeartbeatThenExecutionSucceeds() throws InterruptedException, RemoteOperationException {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();
        final WorkflowExecutionInformation info = createWorkflowExecutionInformation();

        expectWorkflowStartAndSucceed(contextToExecute, info);
        expectNotificationSubscription();
        expectHeartbeatCheckerScheduling();

        service.startWorkflowExecutionAsynchronously(contextToExecute);

        awaitNotificationSubscription();
        sendWorkflowFinishedNotification();

        this.workflowExecutionSucceeded = service.awaitWorkflowTermination();

        assertTrue(workflowExecutionSucceeded);
    }
    
    @Test
    public void whenHeartbeatArrivesThenTimeoutIsReset() throws InterruptedException, RemoteOperationException {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();
        final WorkflowExecutionInformation info = createWorkflowExecutionInformation();

        expectWorkflowStartAndSucceed(contextToExecute, info);
        expectNotificationSubscription();
        expectHeartbeatCheckerScheduling();

        service.startWorkflowExecutionAsynchronously(contextToExecute);

        awaitNotificationSubscription();
        
        sendHeartbeatNotification();
        service.advanceWalltime(HEARTBEAT_TIMEOUT / 2);
        sendHeartbeatNotification();
        service.advanceWalltime(HEARTBEAT_TIMEOUT / 2);
        sendHeartbeatNotification();
        service.advanceWalltime(HEARTBEAT_TIMEOUT / 2);
        sendWorkflowFinishedNotification();

        this.workflowExecutionSucceeded = service.awaitWorkflowTermination();

        assertTrue(workflowExecutionSucceeded);
    }
    
    @Test
    public void whenWorkflowFailsThenExecuteWorkflowReturnsFalse() throws InterruptedException, RemoteOperationException {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();
        final WorkflowExecutionInformation info = createWorkflowExecutionInformation();

        expectWorkflowStartAndSucceed(contextToExecute, info);
        expectNotificationSubscription();
        expectHeartbeatCheckerScheduling();

        service.startWorkflowExecutionAsynchronously(contextToExecute);

        awaitNotificationSubscription();
        
        sendWorkflowFailedNotification();

        this.workflowExecutionSucceeded = service.awaitWorkflowTermination();

        assertFalse(workflowExecutionSucceeded);
    }
    
    @Test
    public void whenWorkflowIsCancelledThenExecuteWorkflowReturnsFalse() throws InterruptedException, RemoteOperationException {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();
        final WorkflowExecutionInformation info = createWorkflowExecutionInformation();

        expectWorkflowStartAndSucceed(contextToExecute, info);
        expectNotificationSubscription();
        expectHeartbeatCheckerScheduling();

        service.startWorkflowExecutionAsynchronously(contextToExecute);

        awaitNotificationSubscription();
        
        sendWorkflowCancelledNotification();

        this.workflowExecutionSucceeded = service.awaitWorkflowTermination();

        assertTrue(workflowExecutionSucceeded);
    }

    private void awaitNotificationSubscription() {
        while (!this.workflowStateNotificationHandler.hasCaptured()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO
            }
        }
    }

    @Test
    public void whenHeartbeatIsMissingThenWorkflowExecutionFails()
        throws ComponentException, WorkflowExecutionException, RemoteOperationException, InterruptedException {
        final WorkflowExecutionContext contextToExecute = createWorkflowExecutionContext();
        final WorkflowExecutionInformation info = createWorkflowExecutionInformation();

        expectWorkflowStartAndSucceed(contextToExecute, info);
        expectHeartbeatCheckerScheduling();

        expectNotificationSubscription();

        service.startWorkflowExecutionAsynchronously(contextToExecute);
        this.awaitHeartbeatCapture();

        service.advanceWalltime(HEARTBEAT_TIMEOUT * 2);
        this.executeHeartbeatChecker();

        this.workflowExecutionSucceeded = service.awaitWorkflowTermination();

        assertFalse(workflowExecutionSucceeded);
    }

    private void expectWorkflowStartAndSucceed(final WorkflowExecutionContext expectedExecutionContext,
        final WorkflowExecutionInformation info) throws InterruptedException {
        try {
            EasyMock.expect(this.mockExecutionService.startWorkflowExecution(expectedExecutionContext)).andStubReturn(info);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            // Not handled as the potentially throwing method is only invoked on a mock object
        }

        EasyMock.replay(this.mockExecutionService);
    }

    private void expectWorkflowStartAndThrowException(WorkflowExecutionContext contextToExecute, Throwable exceptionToThrow) {
        try {
            EasyMock.expect(this.mockExecutionService.startWorkflowExecution(contextToExecute)).andThrow(exceptionToThrow);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            // Not handled as the potentially throwing method is only invoked on a mock object
        }

        EasyMock.replay(this.mockExecutionService);
    }

    protected WorkflowExecutionContext createWorkflowExecutionContext() {
        final WorkflowExecutionContext context =
            new WorkflowExecutionContextBuilder(new WorkflowDescription("some workflow description identifier")).build();
        return context;
    }

    protected WorkflowExecutionInformation createWorkflowExecutionInformation() {
        // Even though WorkflowExecutionInformation should only be a value object, we mock it here instead of instantiating its actual
        // implementation. This is due to the implementation of WorkflowExecutionInformation being an internal class that is not part of the
        // API
        final WorkflowExecutionInformation info = EasyMock.createMock(WorkflowExecutionInformation.class);
        EasyMock.expect(info.getExecutionIdentifier()).andStubReturn(someExecutionIdentifier());
        EasyMock.replay(info);

        return info;
    }

    private void expectHeartbeatCheckerScheduling() {
        final ScheduledFuture<?> mockFuture = EasyMock.createMock(ScheduledFuture.class);

        ((IExpectationSetters) EasyMock.expect(taskService.scheduleAtFixedInterval(
            EasyMock.eq("Check for workflow heartbeat"),
            EasyMock.capture(this.heartbeatChecker),
            EasyMock.anyLong())))
                .andStubReturn(mockFuture);

        EasyMock.expect(mockFuture.cancel(EasyMock.anyBoolean())).andStubReturn(true);

        EasyMock.replay(mockFuture, taskService);

    }

    public void executeHeartbeatChecker() {
        this.heartbeatChecker.getValue().run();
    }

    public void awaitHeartbeatCapture() {
        while (!this.heartbeatChecker.hasCaptured()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

    }

    private void sendHeartbeatNotification() {
        sendWorkflowStateNotification(WorkflowState.IS_ALIVE);
    }

    private void sendWorkflowCancelledNotification() {
        sendWorkflowStateNotification(WorkflowState.CANCELLED);
    }

    private void sendWorkflowFailedNotification() {
        sendWorkflowStateNotification(WorkflowState.FAILED);
    }

    private void sendWorkflowFinishedNotification() {
        sendWorkflowStateNotification(WorkflowState.FINISHED);
    }

    private void sendWorkflowStateNotification(WorkflowState workflowState) {
        this.workflowStateNotificationHandler.getValue().processNotification(new Notification(
            notificationId(someExecutionIdentifier()),
            0, EasyMock.createMock(InstanceNodeSessionId.class),
            workflowState.toString()));
    }
}
