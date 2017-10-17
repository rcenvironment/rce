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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.internal.ComponentExecutor.ComponentExecutionType;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.testutils.ComponentDefaultStub;
import de.rcenvironment.core.datamodel.api.DataModelConstants;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Test cases for {@link ComponentExecutor}.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutorTest {

    private static final int WAIT_INTERVAL_100_MSEC = 100;

    private static final int TEST_TIMEOUT_500_MSEC = 800;

    private static final String COMP_ID = "comp-id";

    private static final String COMP_EXE_ID = "comp-exe-id";

    private static final String COMP_INSTANCE_NAME = "comp instance name";

    private static final String WF_EXE_ID = "wf-exe-id";

    private static final String WF_INSTANCE_NAME = "wf instance name";

    /**
     * Tests if a {@link Component}'s execution is not considered if the {@link ComponentExecutor} got cancelled before the actual run was
     * performed.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected interruption
     * @throws ExecutionException on unexpected interruption
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testOnCancelledBeforeExecuteByConsLimWasCalled()
        throws ComponentException, ComponentExecutionException, InterruptedException, ExecutionException {
        final ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();
        final ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        Future<Boolean> cancelTask = ConcurrencyUtils.getAsyncTaskService().submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                compExecutor.onCancelled();
                boolean isComponentKnownCanceled = compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get();
                return isComponentKnownCanceled;
            }
        });
        assertFalse(cancelTask.get());
        compExecutor.executeByConsideringLimitations();
        assertTrue(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
    }

    /**
     * Tests if the execution of the {@link Component}'s start method has no timeout.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testSartAsInitHasNoTimeout() throws ComponentExecutionException, ComponentException {
        final ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.start();
        final int startMethodTimeMSec = 200;
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {

            @Override
            public Void answer() throws Throwable {
                try {
                    Thread.sleep(startMethodTimeMSec);
                } catch (InterruptedException e) {
                    throw new ComponentException("Unexpected error", e);
                }
                return null;
            }
        });
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineEventForRuns(compStateMachineEventCapture);
        compExeRelatedInstancesStub.compStateMachine = compStateMachineMock;

        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.StartAsInit);

        ComponentExecutor.waitIntervalAfterCacelledCalledMSec = WAIT_INTERVAL_100_MSEC;
        ComponentExecutor.waitIntervalNotRunMSec = WAIT_INTERVAL_100_MSEC;
        compExecutor.executeByConsideringLimitations();

        assertFalse(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
    }

    /**
     * Tests if a {@link Component}'s execution is not considered if the {@link ComponentExecutor} got cancelled before the actual run was
     * performed.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected interruption
     * @throws ExecutionException on unexpected interruption
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testOnCancelledDuringExecuteByConsLimWasCalled()
        throws ComponentException, ComponentExecutionException, InterruptedException, ExecutionException {
        ComponentExecutionPermitsService compExePermitsService = createComponentExecutionPermitServiceMock();
        final ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();
        final ComponentExecutor compExecutor = new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(compExePermitsService);
        compExecutor.acquireExecutionPermission();
        Future<Boolean> cancelTask = ConcurrencyUtils.getAsyncTaskService().submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                compExecutor.onCancelled();
                return compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get();
            }
        });
        assertFalse(cancelTask.get());
        compExecutor.performExecutionAndReleasePermission();
        EasyMock.verify(compExePermitsService);
        assertTrue(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
    }

    /**
     * Tests if waiting for execution permission is interrupted if component is cancelled.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected interruption
     * @throws ExecutionException on unexpected interruption
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testWaitingForExecutionPermissionOnCancelled()
        throws ComponentException, ComponentExecutionException, InterruptedException, ExecutionException {
        ComponentExecutionPermitsService compExePermitsService = createComponentExecutionPermitServiceMock(false);
        final ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();
        final ComponentExecutor compExecutor = new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(compExePermitsService);
        final int delayMsec = 100;
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay(new Runnable() {

            @TaskDescription("Delayed cancel call")
            @Override
            public void run() {
                compExecutor.onCancelled();
            }
        }, delayMsec);
        compExecutor.acquireExecutionPermission();
        EasyMock.verify(compExePermitsService);
        assertTrue(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
    }

    /**
     * Tests tearing down component in success case.
     * 
     * @throws ComponentException on unexpected error
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testTearDownSuccess() throws ComponentException, ComponentExecutionException {

        Component.FinalComponentState finalCompState = Component.FinalComponentState.FINISHED;
        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.tearDown(finalCompState);
        EasyMock.expectLastCall();
        EasyMock.replay(compMock);

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();
        compExeRelatedInstancesStub.component.set(compMock);

        ComponentExecutionType compExeType = ComponentExecutionType.TearDown;
        compExeType.setFinalComponentStateAfterTearedDown(finalCompState);
        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.TearDown);
        compExecutor.executeByConsideringLimitations();

        EasyMock.verify(compMock);
    }

    /**
     * Tests tearing down component in success case.
     * 
     * @throws ComponentException on unexpected error
     * @throws ComponentExecutionException on unexpected error
     */
    @Test
    public void testTearDownFailure() throws ComponentException, ComponentExecutionException {

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        String errorMessage = "some error in tear down";
        Component.FinalComponentState finalCompState = Component.FinalComponentState.FINISHED;
        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.tearDown(finalCompState);
        EasyMock.expectLastCall().andThrow(new RuntimeException(errorMessage));
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        ComponentExecutionStorageBridge compExeStorageBridgeMock =
            createComponentExecutionStorageBridgeNonRunFailure(compExeRelatedInstancesStub, DataModelConstants.TEAR_DOWN_RUN);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        Capture<ConsoleRow.Type> consoleRowTypeCapture = new Capture<>();
        Capture<String> logMessageCapture = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(consoleRowTypeCapture, logMessageCapture);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionType compExeType = ComponentExecutionType.TearDown;
        compExeType.setFinalComponentStateAfterTearedDown(finalCompState);
        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.TearDown);

        try {
            compExecutor.executeByConsideringLimitations();
            fail("ComponentException expected");
        } catch (ComponentException e) {
            assertFailureHandling(consoleRowTypeCapture, logMessageCapture, e, errorMessage);
        }

        EasyMock.verify(compMock);
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    /**
     * Tests executing of {@link Component#processInputs()} in success case.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testProcessingInputsSuccess()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.processInputs();
        EasyMock.expectLastCall();
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineEventForRuns(compStateMachineEventCapture);
        compExeRelatedInstancesStub.compStateMachine = compStateMachineMock;

        ComponentExecutionStorageBridge compExeStorageBridgeMock =
            createComponentExecutionStorageBridgeRunSuccess(compExeRelatedInstancesStub);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        ComponentContextBridge compCtxBridgeMock = EasyMock.createStrictMock(ComponentContextBridge.class);
        EasyMock.expect(compCtxBridgeMock.getEndpointDatumsForExecution()).andStubReturn(new HashMap<String, EndpointDatum>());
        EasyMock.replay(compCtxBridgeMock);
        compExeRelatedInstancesStub.compCtxBridge = compCtxBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(null, null);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionStatsService compExeStatsServiceMock = createComponentExecutionStatsServiceMock(compExeRelatedInstancesStub);

        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());
        compExecutor.bindComponentExecutionStatsService(compExeStatsServiceMock);

        compExecutor.executeByConsideringLimitations();

        assertFalse(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compMock);
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RUNNING, compStateMachineEventCapture.getValue().getType());
        assertEquals(ComponentState.PROCESSING_INPUTS, compStateMachineEventCapture.getValue().getNewComponentState());
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    /**
     * Tests executing of {@link Component#completeStartOrProcessInputsAfterVerificationDone()} in case results are rejected.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testCompleteVerificationResultsRejected()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub(true);

        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.completeStartOrProcessInputsAfterVerificationDone();
        EasyMock.expectLastCall();
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        compExeStorageBridgeMock.setComponentExecutionFinished(FinalComponentRunState.RESULTS_REJECTED);
        EasyMock.expectLastCall();
        EasyMock.replay(compExeStorageBridgeMock);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(null, null);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionType compExeType = ComponentExecutionType.CompleteVerification;
        compExeType.setFinalComponentStateAfterRun(FinalComponentRunState.RESULTS_REJECTED);
        ComponentExecutor compExecutor = new ComponentExecutor(compExeRelatedInstancesStub, compExeType);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());

        compExecutor.executeByConsideringLimitations();

        assertFalse(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compMock);
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    /**
     * Tests executing of {@link Component#handleVerificationToken(String)}: verification token passed properly and life cycle method call
     * handled properly.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testHandleVerificationToken()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {

        final String someToken = "some-token";

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub(true);

        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.handleVerificationToken(someToken);
        EasyMock.expectLastCall();
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        ComponentExecutionType compExeType = ComponentExecutionType.HandleVerificationToken;
        compExeType.setVerificationToken(someToken);
        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, compExeType);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());

        compExecutor.executeByConsideringLimitations();

        assertFalse(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compMock);
    }

    /**
     * Tests executing of {@link Component#processInputs()} in failure case, that means {@link Component#processInputs()} throws a
     * {@link ComponentException} or {@link RuntimeException}.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test
    public void testProcessingInputsFailureComponentException()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {
        testProcessInputsFailure(new ComponentException("some error when processing inputs"));
    }

    /**
     * Tests executing of {@link Component#processInputs()} in failure case, that means {@link Component#processInputs()} throws a
     * {@link ComponentException} or {@link RuntimeException}.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testProcessingInputsFailureRuntimeException()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {
        testProcessInputsFailure(new RuntimeException());
    }

    private void testProcessInputsFailure(Exception expectedException)
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        Component compMock = EasyMock.createStrictMock(Component.class);
        compMock.processInputs();
        EasyMock.expectLastCall().andThrow(expectedException);
        compMock.completeStartOrProcessInputsAfterFailure();
        EasyMock.expectLastCall();
        EasyMock.replay(compMock);
        compExeRelatedInstancesStub.component.set(compMock);

        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineEventForRuns(compStateMachineEventCapture);
        compExeRelatedInstancesStub.compStateMachine = compStateMachineMock;

        ComponentExecutionStorageBridge compExeStorageBridgeMock =
            createComponentExecutionStorageBridgeRunFailure(compExeRelatedInstancesStub);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        ComponentContextBridge compCtxBridgeMock = EasyMock.createStrictMock(ComponentContextBridge.class);
        EasyMock.expect(compCtxBridgeMock.getEndpointDatumsForExecution()).andStubReturn(new HashMap<String, EndpointDatum>());
        EasyMock.replay(compCtxBridgeMock);
        compExeRelatedInstancesStub.compCtxBridge = compCtxBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        Capture<ConsoleRow.Type> consoleRowTypeCapture = new Capture<>();
        Capture<String> logMessageCapture = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(consoleRowTypeCapture, logMessageCapture);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionStatsService compExeStatsServiceMock = createComponentExecutionStatsServiceMock(compExeRelatedInstancesStub);

        ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());
        compExecutor.bindComponentExecutionStatsService(compExeStatsServiceMock);

        try {
            compExecutor.executeByConsideringLimitations();
            fail("ComponentException expected");
        } catch (ComponentException e) {
            if (expectedException.getMessage() == null) {
                assertFailureHandling(consoleRowTypeCapture, logMessageCapture, e, "Unexpected error");
            } else {
                assertFailureHandling(consoleRowTypeCapture, logMessageCapture, e, expectedException.getMessage());
            }
        }

        assertFalse(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compMock);
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RUNNING, compStateMachineEventCapture.getValue().getType());
        assertEquals(ComponentState.PROCESSING_INPUTS, compStateMachineEventCapture.getValue().getNewComponentState());
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    /**
     * Tests canceling of {@link Component#processInputs()} in success case, that means {@link Component#processInputs()} returns within
     * expected amount of time.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testCancelProcessingInputsSuccess()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        final AtomicReference<CountDownLatch> countDownLatchRef = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        Component compStub = new ComponentDefaultStub.Default() {

            private int processInputsCount = 0;

            private int onProcessInputsInterruptedCount = 0;

            @Override
            public void processInputs() throws ComponentException {
                if (processInputsCount > 0) {
                    fail("'processInputs' is expected to be called only once");
                }
                try {
                    countDownLatchRef.get().await();
                } catch (InterruptedException e) {
                    fail("unexpected InterruptedException");
                }
            }

            @Override
            public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
                if (onProcessInputsInterruptedCount > 0) {
                    fail("'onProcessInputsInterrupted' is expected to be called only once");
                }
                countDownLatchRef.get().countDown();
            }
        };
        compExeRelatedInstancesStub.component.set(compStub);

        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        ComponentStateMachine compStateMachineMock = createComponentStateMachineEventForRuns(compStateMachineEventCapture);
        compExeRelatedInstancesStub.compStateMachine = compStateMachineMock;

        ComponentExecutionStorageBridge compExeStorageBridgeMock =
            createComponentExecutionStorageBridgeRunSuccess(compExeRelatedInstancesStub);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        ComponentContextBridge compCtxBridgeMock = EasyMock.createStrictMock(ComponentContextBridge.class);
        EasyMock.expect(compCtxBridgeMock.getEndpointDatumsForExecution()).andStubReturn(new HashMap<String, EndpointDatum>());
        EasyMock.replay(compCtxBridgeMock);
        compExeRelatedInstancesStub.compCtxBridge = compCtxBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(null, null);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionStatsService compExeStatsServiceMock = createComponentExecutionStatsServiceMock(compExeRelatedInstancesStub);

        final ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());
        compExecutor.bindComponentExecutionStatsService(compExeStatsServiceMock);

        final int delayMsec = 100;
        ConcurrencyUtils.getAsyncTaskService().scheduleAfterDelay(new Runnable() {

            @TaskDescription("Delayed cancel call")
            @Override
            public void run() {
                compExecutor.onCancelled();
            }
        }, delayMsec);

        compExecutor.executeByConsideringLimitations();

        assertTrue(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RUNNING, compStateMachineEventCapture.getValue().getType());
        assertEquals(ComponentState.PROCESSING_INPUTS, compStateMachineEventCapture.getValue().getNewComponentState());
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    /**
     * Tests canceling of {@link Component#processInputs()} in failure case, that means {@link Component#processInputs()} doesn't return
     * within expected amount of time.
     * 
     * @throws ComponentExecutionException on unexpected error
     * @throws ComponentException on unexpected error
     * @throws InterruptedException on unexpected error
     * @throws ExecutionException on unexpected error
     */
    @Test(timeout = TEST_TIMEOUT_500_MSEC)
    public void testCancelProcessingInputsFailure()
        throws ComponentExecutionException, ComponentException, InterruptedException, ExecutionException {

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = createComponentExecutionRelatedInstancesStub();

        final CountDownLatch processInputsCalledLatch = new CountDownLatch(1);
        Component compStub = new ComponentDefaultStub.Default() {

            private int processInputsCount = 0;

            private int onProcessInputsInterruptedCount = 0;

            @Override
            public void processInputs() throws ComponentException {
                if (processInputsCount > 0) {
                    fail("'processInputs' is expected to be called only once");
                }
                processInputsCalledLatch.countDown();
                final CountDownLatch dummyLatch = new CountDownLatch(1);
                while (true) {
                    try {
                        dummyLatch.await();
                    } catch (InterruptedException e) {
                        // ignore for test purposes
                        e = null;
                    }
                }
            }

            @Override
            public void onProcessInputsInterrupted(ThreadHandler executingThreadHandler) {
                if (onProcessInputsInterruptedCount > 0) {
                    fail("'onProcessInputsInterrupted' is expected to be called only once");
                }
            }
        };
        compExeRelatedInstancesStub.component.set(compStub);

        ComponentStateMachine compStateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        Capture<ComponentStateMachineEvent> compStateMachineEventCapture = new Capture<>();
        compStateMachineMock.postEvent(EasyMock.capture(compStateMachineEventCapture));
        EasyMock.expectLastCall();
        EasyMock.replay(compStateMachineMock);
        compExeRelatedInstancesStub.compStateMachine = compStateMachineMock;

        ComponentExecutionStorageBridge compExeStorageBridgeMock =
            createComponentExecutionStorageBridgeRunFailure(compExeRelatedInstancesStub);
        compExeRelatedInstancesStub.compExeStorageBridge = compExeStorageBridgeMock;

        ComponentContextBridge compCtxBridgeMock = EasyMock.createStrictMock(ComponentContextBridge.class);
        EasyMock.expect(compCtxBridgeMock.getEndpointDatumsForExecution()).andStubReturn(new HashMap<String, EndpointDatum>());
        EasyMock.replay(compCtxBridgeMock);
        compExeRelatedInstancesStub.compCtxBridge = compCtxBridgeMock;

        compExeRelatedInstancesStub.compExeScheduler = createComponentExecutionSchedulerMock(false);

        Capture<ConsoleRow.Type> consoleRowTypeCapture = new Capture<>();
        Capture<String> logMessageCapture = new Capture<>();
        ConsoleRowsSender consoleRowsSenderMock = createConsoleRowsSenderMock(consoleRowTypeCapture, logMessageCapture);
        compExeRelatedInstancesStub.consoleRowsSender = consoleRowsSenderMock;

        ComponentExecutionStatsService compExeStatsServiceMock = createComponentExecutionStatsServiceMock(compExeRelatedInstancesStub);

        final ComponentExecutor compExecutor =
            new ComponentExecutor(compExeRelatedInstancesStub, ComponentExecutionType.ProcessInputs);
        compExecutor.bindComponentExecutionPermitsService(createComponentExecutionPermitServiceMock());
        compExecutor.bindComponentExecutionStatsService(compExeStatsServiceMock);
        ComponentExecutor.waitIntervalAfterCacelledCalledMSec = WAIT_INTERVAL_100_MSEC;

        final AtomicReference<Exception> expectedExceptionRef = new AtomicReference<Exception>(null);

        final CountDownLatch executedLatch = new CountDownLatch(1);
        final Future<?> executeTask = ConcurrencyUtils.getAsyncTaskService().submit(new Runnable() {

            @Override
            public void run() {
                try {
                    compExecutor.executeByConsideringLimitations();
                } catch (ComponentException | ComponentExecutionException e) {
                    expectedExceptionRef.set(e);
                }
                executedLatch.countDown();
            }
        });
        processInputsCalledLatch.await();
        compExecutor.onCancelled();
        executeTask.cancel(true);

        executedLatch.await();

        assertNotNull(expectedExceptionRef.get());
        assertTrue(expectedExceptionRef.get() instanceof ComponentException);
        assertFailureHandling(consoleRowTypeCapture, logMessageCapture, (ComponentException) expectedExceptionRef.get(),
            "didn't terminate in time");

        assertTrue(compExeRelatedInstancesStub.compExeRelatedStates.isComponentCancelled.get());
        EasyMock.verify(compStateMachineMock);
        assertEquals(ComponentStateMachineEventType.RUNNING, compStateMachineEventCapture.getValue().getType());
        assertEquals(ComponentState.PROCESSING_INPUTS, compStateMachineEventCapture.getValue().getNewComponentState());
        EasyMock.verify(compExeStorageBridgeMock);
        EasyMock.verify(consoleRowsSenderMock);
    }

    private ComponentExecutionStorageBridge createComponentExecutionStorageBridgeRunFailure(
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub) throws ComponentExecutionException {
        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        compExeStorageBridgeMock.addComponentExecution(compExeRelatedInstancesStub.compExeCtx,
            compExeRelatedInstancesStub.compExeRelatedStates.executionCount.get());
        EasyMock.expectLastCall();
        EasyMock.expect(compExeStorageBridgeMock.hasUnfinishedComponentExecution()).andReturn(true);
        Capture<FinalComponentRunState> finalStateCapture = new Capture<>();
        compExeStorageBridgeMock.setComponentExecutionFinished(EasyMock.capture(finalStateCapture));
        EasyMock.expectLastCall();
        EasyMock.replay(compExeStorageBridgeMock);
        return compExeStorageBridgeMock;
    }

    private ComponentStateMachine createComponentStateMachineEventForRuns(
        Capture<ComponentStateMachineEvent> compStateMachineEventCapture) {
        ComponentStateMachine compStateMachineMock = EasyMock.createStrictMock(ComponentStateMachine.class);
        compStateMachineMock.postEvent(EasyMock.capture(compStateMachineEventCapture));
        EasyMock.expectLastCall();
        EasyMock.replay(compStateMachineMock);
        return compStateMachineMock;
    }

    private ComponentExecutionStorageBridge createComponentExecutionStorageBridgeNonRunFailure(
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub, Integer exeCount) throws ComponentExecutionException {
        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        EasyMock.expect(compExeStorageBridgeMock.hasUnfinishedComponentExecution()).andReturn(false);
        compExeStorageBridgeMock.addComponentExecution(compExeRelatedInstancesStub.compExeCtx, exeCount);
        EasyMock.expectLastCall();
        Capture<FinalComponentRunState> finalStateCapture = new Capture<>();
        compExeStorageBridgeMock.setComponentExecutionFinished(EasyMock.capture(finalStateCapture));
        EasyMock.expectLastCall();
        EasyMock.replay(compExeStorageBridgeMock);
        return compExeStorageBridgeMock;
    }

    private void assertFailureHandling(Capture<ConsoleRow.Type> consoleRowTypeCapture, Capture<String> logMessageCapture,
        ComponentException e, String expectedErrorMessage) {
        assertTrue(e.getMessage().matches("E#[0-9]{13}"));
        assertTrue(consoleRowTypeCapture.hasCaptured());
        assertEquals(ConsoleRow.Type.COMPONENT_ERROR, consoleRowTypeCapture.getValue());
        assertTrue(logMessageCapture.hasCaptured());
        assertTrue(logMessageCapture.getValue().contains(e.getMessage()));
        assertTrue(logMessageCapture.getValue().contains(expectedErrorMessage));
    }

    private ComponentExecutionStorageBridge createComponentExecutionStorageBridgeRunSuccess(
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub)
        throws ComponentExecutionException {
        return createComponentExecutionStorageBridgeRunSuccess(compExeRelatedInstancesStub, FinalComponentRunState.FINISHED);
    }

    private ComponentExecutionStorageBridge createComponentExecutionStorageBridgeRunSuccess(
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub, FinalComponentRunState finalState)
        throws ComponentExecutionException {
        ComponentExecutionStorageBridge compExeStorageBridgeMock = EasyMock.createStrictMock(ComponentExecutionStorageBridge.class);
        compExeStorageBridgeMock.addComponentExecution(compExeRelatedInstancesStub.compExeCtx,
            compExeRelatedInstancesStub.compExeRelatedStates.executionCount.get());
        EasyMock.expectLastCall();
        compExeStorageBridgeMock.setComponentExecutionFinished(finalState);
        EasyMock.expectLastCall();
        EasyMock.replay(compExeStorageBridgeMock);
        return compExeStorageBridgeMock;
    }

    private ConsoleRowsSender createConsoleRowsSenderMock(Capture<ConsoleRow.Type> consoleRowTypeCapture,
        Capture<String> logMessageCapture) {
        ConsoleRowsSender consoleRowsSenderMock = EasyMock.createStrictMock(ConsoleRowsSender.class);
        if (consoleRowTypeCapture != null) {
            consoleRowsSenderMock.sendLogMessageAsConsoleRow(EasyMock.capture(consoleRowTypeCapture), EasyMock.capture(logMessageCapture),
                EasyMock.anyInt());
            EasyMock.expectLastCall();
        }
        consoleRowsSenderMock.sendLogFileWriteTriggerAsConsoleRow();
        EasyMock.expectLastCall();
        EasyMock.replay(consoleRowsSenderMock);
        return consoleRowsSenderMock;
    }

    private ComponentExecutionStatsService createComponentExecutionStatsServiceMock(
        ComponentExecutionRelatedInstances compExeRelatedInstancesStub) {
        ComponentExecutionStatsService compExeStatsServiceMock = EasyMock.createStrictMock(ComponentExecutionStatsService.class);
        compExeStatsServiceMock.addStatsAtComponentRunStart(compExeRelatedInstancesStub.compExeCtx);
        EasyMock.expectLastCall();
        compExeStatsServiceMock.addStatsAtComponentRunTermination(compExeRelatedInstancesStub.compExeCtx);
        EasyMock.expectLastCall();
        EasyMock.replay(compExeStatsServiceMock);
        return compExeStatsServiceMock;
    }

    private ComponentExecutionPermitsService createComponentExecutionPermitServiceMock(boolean permitting)
        throws InterruptedException, ExecutionException {
        Future<Boolean> futureStub = new FutureStub(permitting);
        ComponentExecutionPermitsService compExePermitsServiceMock = EasyMock.createStrictMock(ComponentExecutionPermitsService.class);
        EasyMock.expect(compExePermitsServiceMock.acquire(COMP_ID, COMP_EXE_ID)).andReturn(futureStub);
        if (permitting) {
            compExePermitsServiceMock.release(COMP_ID);
            EasyMock.expectLastCall();
        }
        EasyMock.replay(compExePermitsServiceMock);
        return compExePermitsServiceMock;
    }

    private ComponentExecutionPermitsService createComponentExecutionPermitServiceMock() throws InterruptedException, ExecutionException {
        return createComponentExecutionPermitServiceMock(true);
    }

    private ComponentExecutionRelatedInstances createComponentExecutionRelatedInstancesStub() throws ComponentExecutionException {
        return createComponentExecutionRelatedInstancesStub(false);
    }

    private ComponentExecutionRelatedInstances createComponentExecutionRelatedInstancesStub(boolean requiresOutputApproval)
        throws ComponentExecutionException {

        ComponentDescription compDescMock = EasyMock.createStrictMock(ComponentDescription.class);
        EasyMock.expect(compDescMock.getIdentifier()).andStubReturn(COMP_ID);
        EasyMock.expect(compDescMock.getConfigurationDescription())
            .andStubReturn(ConfigurationDescriptionMockFactory.createConfigurationDescriptionMock(requiresOutputApproval, false));

        EasyMock.replay(compDescMock);

        ComponentExecutionContext compExeCtxMock = EasyMock.createStrictMock(ComponentExecutionContext.class);
        EasyMock.expect(compExeCtxMock.getComponentDescription()).andStubReturn(compDescMock);
        EasyMock.expect(compExeCtxMock.getExecutionIdentifier()).andStubReturn(COMP_EXE_ID);
        EasyMock.expect(compExeCtxMock.getInstanceName()).andStubReturn(COMP_INSTANCE_NAME);
        EasyMock.expect(compExeCtxMock.getWorkflowExecutionIdentifier()).andStubReturn(WF_EXE_ID);
        EasyMock.expect(compExeCtxMock.getWorkflowInstanceName()).andStubReturn(WF_INSTANCE_NAME);
        EasyMock.expect(compExeCtxMock.getWorkflowGraph()).andStubReturn(createWorkflowGraphMock());
        EasyMock.replay(compExeCtxMock);

        ComponentExecutionRelatedInstances compExeRelatedInstancesStub = new ComponentExecutionRelatedInstances();
        compExeRelatedInstancesStub.compExeCtx = compExeCtxMock;
        compExeRelatedInstancesStub.compExeRelatedStates = new ComponentExecutionRelatedStates();

        return compExeRelatedInstancesStub;
    }

    private ComponentExecutionScheduler createComponentExecutionSchedulerMock(boolean isLoopResetRequested) {
        ComponentExecutionScheduler compExeSchedulerMock = EasyMock.createStrictMock(ComponentExecutionScheduler.class);
        EasyMock.expect(compExeSchedulerMock.isLoopResetRequested()).andStubReturn(false);
        EasyMock.replay(compExeSchedulerMock);
        return compExeSchedulerMock;
    }

    private WorkflowGraph createWorkflowGraphMock() throws ComponentExecutionException {
        WorkflowGraph wfGraphMock = EasyMock.createStrictMock(WorkflowGraph.class);
        EasyMock.expect(wfGraphMock.getLoopDriver(COMP_EXE_ID)).andReturn(null);
        EasyMock.replay(wfGraphMock);
        return wfGraphMock;
    }

    /**
     * Stub implementation for {@link Future} stubbing the permission acquiring task.
     *
     * @author Doreen Seider
     */
    private class FutureStub implements Future<Boolean> {

        private final boolean permitting;

        private final CountDownLatch waitingDoneLatch = new CountDownLatch(1);

        protected FutureStub(boolean permitting) {
            this.permitting = permitting;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            waitingDoneLatch.countDown();
            return true;
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            return permitting;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
            if (permitting) {
                return true;
            }
            waitingDoneLatch.await();
            throw new CancellationException();
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException();
        }

    }
}
