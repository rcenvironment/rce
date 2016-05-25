/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.DataModelConstants;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;

/**
 * Wrapper class that calls life cycle methods of components by considering optional limitations concerning the amount of parallel
 * executions.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutor {

    protected static final int DEAFULT_WAIT_INTERVAL_AFTER_CANCELLED_SEC = 10;

    protected static int waitIntervalAfterCacelledCalledSec = DEAFULT_WAIT_INTERVAL_AFTER_CANCELLED_SEC;

    private static final int WAIT_INTERVAL_NOT_RUN_SEC = 10;

    private static final Log LOG = LogFactory.getLog(ComponentExecutor.class);

    /**
     * Type of component execution to be performed.
     * 
     * @author Doreen Seider
     */
    protected enum ComponentExecutionType {

        StartAsInit(ComponentState.STARTING, ComponentStateMachineEventType.START_FAILED, null),
        StartAsRun(ComponentState.STARTING, ComponentStateMachineEventType.START_FAILED, null),
        ProcessInputs(ComponentState.PROCESSING_INPUTS, ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED, null),
        Reset(ComponentState.RESETTING, ComponentStateMachineEventType.RESET_FAILED, null),
        TearDown(ComponentState.TEARING_DOWN, ComponentStateMachineEventType.TEARED_DOWN, ComponentState.FAILED);

        private ComponentState compStateOnSuccess;

        private ComponentStateMachineEventType compStateMachineEventTypeOnFailure;

        private ComponentState compStateAfterFailure;

        private Component.FinalComponentState finalCompStateAfterTearedDown;

        ComponentExecutionType(ComponentState compStateOnSuccess, ComponentStateMachineEventType compStateMachineEventTypeOnFailure,
            ComponentState compStateOnFailure) {
            this.compStateOnSuccess = compStateOnSuccess;
            this.compStateMachineEventTypeOnFailure = compStateMachineEventTypeOnFailure;
            this.compStateAfterFailure = compStateOnFailure;
        }

        protected void setFinalComponentStateAfterTearedDown(Component.FinalComponentState finalState) {
            this.finalCompStateAfterTearedDown = finalState;
        }

    }

    private static TypedDatumService typedDatumService;

    private static ComponentExecutionPermitsService componentExecutionPermitService;

    private static EndpointDatumSerializer endpointDatumSerializer;

    private static ComponentExecutionStatsService compExeStatsService;

    private static boolean sendInputsProcessedToWfCtrl;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private final AsyncOrderedExecutionQueue executionQueue = new AsyncOrderedExecutionQueue(
        AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, SharedThreadPool.getInstance());

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private ComponentExecutionType compExeType;

    private boolean treatAsRun;

    private boolean isTearDown;

    private boolean writesCompRunRelatedDataToDM;

    private AtomicReference<Future<Boolean>> aquirePermissionTask = new AtomicReference<Future<Boolean>>(null);

    private AtomicReference<Future<ComponentException>> executeTask = new AtomicReference<Future<ComponentException>>(null);

    private AtomicBoolean isDone = new AtomicBoolean(false);

    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private CountDownLatch executionLatch = new CountDownLatch(1);

    private boolean executionPermissionAcquired = false;

    @Deprecated
    public ComponentExecutor() {}

    protected ComponentExecutor(ComponentExecutionRelatedInstances compExeRelatedInstances, ComponentExecutionType compExeType) {
        this.compExeRelatedInstances = compExeRelatedInstances;
        this.compExeType = compExeType;
        this.treatAsRun = compExeType == ComponentExecutionType.StartAsRun || compExeType == ComponentExecutionType.ProcessInputs;
        this.writesCompRunRelatedDataToDM = treatAsRun;
        this.isTearDown = compExeType == ComponentExecutionType.TearDown;
    }

    protected void executeByConsideringLimitations() throws ComponentException, ComponentExecutionException {
        acquireExecutionPermission();
        performExecutionAndReleasePermission();
    }

    protected void acquireExecutionPermission() {
        if (treatAsRun && !isCancelled.get()) {
            try {
                aquirePermissionTask.set(componentExecutionPermitService
                    .acquire(compExeRelatedInstances.compExeCtx.getComponentDescription().getIdentifier(),
                        compExeRelatedInstances.compExeCtx.getExecutionIdentifier()));
                executionPermissionAcquired = aquirePermissionTask.get().get();
            } catch (CancellationException e) {
                if (isCancelled.get()) {
                    compExeRelatedInstances.compExeRelatedStates.isComponentCancelled.set(true);
                    return;
                }
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(compExeType.compStateMachineEventTypeOnFailure,
                        compExeType.compStateAfterFailure, e));
            } catch (ExecutionException | InterruptedException e) {
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(compExeType.compStateMachineEventTypeOnFailure,
                        compExeType.compStateAfterFailure, e));
            }
        }
    }
    
    protected void performExecutionAndReleasePermission() throws ComponentExecutionException, ComponentException {
        try {
            if (isCancelled.get()) {
                compExeRelatedInstances.compExeRelatedStates.isComponentCancelled.set(true);
                return;
            }
            if (!isTearDown) {
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RUNNING, compExeType.compStateOnSuccess));
                if (treatAsRun) {
                    compExeRelatedInstances.compExeStorageBridge
                        .addComponentExecution(compExeRelatedInstances.compExeCtx,
                            compExeRelatedInstances.compExeRelatedStates.executionCount.get());
                    storeInputs();
                    compExeStatsService.addStatsAtComponentRunStart(compExeRelatedInstances.compExeCtx);
                }
            }
            try {
                executeAsync();
                awaitExecution();

                if (compExeRelatedInstances.compExeRelatedStates.compHasSentConsoleRowLogMessages.get()) {
                    writesCompRunRelatedDataToDM = true;
                    prepareDmForComponentRunRelatedDataIfNeeded();
                }
            } catch (ComponentException e) {
                writesCompRunRelatedDataToDM = true;
                prepareDmForComponentRunRelatedDataIfNeeded();
                handleComponentExecutionFailure(e);

            } finally {
                if (treatAsRun) {
                    compExeStatsService.addStatsAtComponentRunTermination(compExeRelatedInstances.compExeCtx);
                }
                if (writesCompRunRelatedDataToDM && !compExeRelatedInstances.compExeScheduler.isLoopResetRequested()) {
                    compExeRelatedInstances.consoleRowsSender.sendLogFileWriteTriggerAsConsoleRow();
                    compExeRelatedInstances.compExeStorageBridge.setComponentExecutionFinished();
                }
                if (isCancelled.get()) {
                    compExeRelatedInstances.compExeRelatedStates.isComponentCancelled.set(true);
                }
                executeTask.set(null);
            }
        } finally {
            if (executionPermissionAcquired) {
                componentExecutionPermitService
                    .release(compExeRelatedInstances.compExeCtx.getComponentDescription().getIdentifier());
            }
        }
    }

    private void awaitExecution() throws ComponentException {
        ComponentException exception = null;
        try {
            if (compExeType == ComponentExecutionType.StartAsRun || compExeType == ComponentExecutionType.ProcessInputs) {
                try {
                    exception = executeTask.get().get();
                } catch (CancellationException e) {
                    LOG.debug(StringUtils.format("Task was cancelled that was executing '%s' of %s", compExeType.name(),
                        ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
                }
            } else {
                try {
                    exception = executeTask.get().get(WAIT_INTERVAL_NOT_RUN_SEC, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    executeTask.get().cancel(true);
                    exception = new ComponentException(StringUtils.format(
                        "Task didn't terminate in time and is cancelled; it is executing '%s' of %s", compExeType.name(),
                        ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)),
                        e);
                }
            }
        } catch (InterruptedException e) {
            executeTask.get().cancel(true);
            exception = new ComponentException(StringUtils.format(
                "Waiting for task to terminate was interrupted; it is cancelled now; it is executing '%s' of %s", compExeType.name(),
                ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)),
                e);
        } catch (ExecutionException e) {
            exception = new ComponentException("Unexpected error during component execution", e.getCause());
        }
        try {
            if (!executionLatch.await(waitIntervalAfterCacelledCalledSec, TimeUnit.SECONDS)) {
                exception = new ComponentException(StringUtils.format(
                    "Task didn't terminate in time after it was cancelled; it was executing '%s' of %s", compExeType.name(),
                    ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
            }
        } catch (InterruptedException e) {
            exception = new ComponentException(
                StringUtils.format("Interrupted when waiting for the task that was executing '%s' of %s", compExeType.name(),
                    ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
        }
        isDone.set(true);
        if (exception != null) {
            throw exception;
        }
    }

    private void executeAsync() {
        executeTask.set(threadPool.submit(new Callable<ComponentException>() {

            @TaskDescription("Execute component life-cycle method")
            @Override
            public ComponentException call() throws Exception {
                try {
                    execute();
                } catch (ComponentException e) {
                    return e;
                } catch (RuntimeException e) {
                    return new ComponentException("Unexpected error during execution", e);
                } finally {
                    executionLatch.countDown();
                }
                return null;
            }
        }));
    }

    protected void execute() throws ComponentException, ComponentExecutionException {

        if (compExeType == ComponentExecutionType.StartAsInit || compExeType == ComponentExecutionType.StartAsRun
            || compExeType == ComponentExecutionType.ProcessInputs) {
            try {
                if (compExeType == ComponentExecutionType.ProcessInputs) {
                    compExeRelatedInstances.component.get().processInputs();
                } else {
                    compExeRelatedInstances.component.get().start();
                }
            } catch (ComponentException | RuntimeException e) {
                checkForThreadInterrupted();
                if (!isDone.get()) {
                    callCompleteStartOrProcessInputsOnFailure();
                    throw e;
                } else {
                    LOG.error(StringUtils.format("Ignored error in %s of %s as the task was already considered as done "
                        + "(most likely, it was interupted before): %s", compExeType.name(),
                        ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx),
                        e.getMessage()));
                }
            }
        } else if (compExeType == ComponentExecutionType.Reset) {
            compExeRelatedInstances.component.get().reset();
        } else if (compExeType == ComponentExecutionType.TearDown) {
            compExeRelatedInstances.component.get().tearDown(compExeType.finalCompStateAfterTearedDown);
        } else {
            throw new ComponentExecutionException("Given component execution type not supported: " + compExeType);
        }
    }

    private void checkForThreadInterrupted() {
        if (Thread.interrupted()) {
            LOG.warn(StringUtils.format("Task (thread) was interrupted after executing '%s' of %s",
                compExeType.name(),
                ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
        }
    }

    private void prepareDmForComponentRunRelatedDataIfNeeded() throws ComponentExecutionException {
        if (!compExeRelatedInstances.compExeStorageBridge.hasUnfinishedComponentExecution()) {
            if (isTearDown) {
                compExeRelatedInstances.compExeStorageBridge.addComponentExecution(
                    compExeRelatedInstances.compExeCtx,
                    DataModelConstants.TEAR_DOWN_RUN);
            } else {
                compExeRelatedInstances.compExeStorageBridge.addComponentExecution(compExeRelatedInstances.compExeCtx,
                    compExeRelatedInstances.compExeRelatedStates.executionCount.get());
            }
        }
    }

    private void handleComponentExecutionFailure(Exception e) throws ComponentException, ComponentExecutionException {
        String errId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG,
            StringUtils.format("Executing %s failed",
                ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)),
            e);
        String errorConsoleRow = ComponentUtils.createErrorLogMessage(e, errId);
        compExeRelatedInstances.consoleRowsSender.sendLogMessageAsConsoleRow(Type.COMPONENT_ERROR, errorConsoleRow);

        WorkflowGraphNode loopDriver = compExeRelatedInstances.compExeCtx.getWorkflowGraph()
            .getLoopDriver(compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
        if (loopDriver != null && loopDriver.isDrivingFaultTolerantLoop()) {
            writeFailureOutputData();
        } else {
            throw new ComponentException(errId);
        }
    }

    private void writeFailureOutputData() throws ComponentExecutionException {
        Map<String, Set<Deque<WorkflowGraphHop>>> hopsToTraverseOnFailure =
            compExeRelatedInstances.compExeCtx.getWorkflowGraph()
                .getHopsToTraverseOnFailure(compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
        for (String outputName : hopsToTraverseOnFailure.keySet()) {
            for (Queue<WorkflowGraphHop> hops : hopsToTraverseOnFailure.get(outputName)) {
                WorkflowGraphHop firstHop = hops.poll();
                NotAValueTD notAValue = typedDatumService.getFactory().createNotAValue(
                    UUID.randomUUID().toString() + NotAValueTD.FAILURE_CAUSE_SUFFIX, NotAValueTD.Cause.Failure);
                Long outputDmId = compExeRelatedInstances.compExeStorageBridge.addOutput(outputName,
                    typedDatumService.getSerializer().serialize(notAValue));
                compExeRelatedInstances.compExeScheduler.addNotAValueDatumSent(notAValue.getIdentifier());
                InternalTDImpl failureDatum = new InternalTDImpl(InternalTDImpl.InternalTDType.FailureInLoop,
                    notAValue.getIdentifier(), hops, String.valueOf(outputDmId));
                compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(outputName,
                    failureDatum,
                    firstHop.getTargetExecutionIdentifier(), firstHop.getTargetInputName());
            }
        }

    }

    private void storeInputs() throws ComponentExecutionException {

        for (final Entry<String, EndpointDatum> entry : compExeRelatedInstances.compCtxBridge.getEndpointDatumsForExecution().entrySet()) {
            if (entry.getValue().getDataManagementId() != null) {
                compExeRelatedInstances.compExeStorageBridge.addInput(entry.getKey(),
                    entry.getValue().getDataManagementId());
                if (sendInputsProcessedToWfCtrl) {
                    executionQueue.enqueue(new Runnable() {

                        @Override
                        public void run() {
                            compExeRelatedInstances.wfExeCtrlBridgeDelegator
                                .onInputProcessed(endpointDatumSerializer.serializeEndpointDatum(entry.getValue()));
                        }
                    });
                }
            }
        }
    }

    protected void onCancelled() {
        isCancelled.set(true);
        if (aquirePermissionTask.get() != null && !aquirePermissionTask.get().isDone()) {
            aquirePermissionTask.get().cancel(true);
        } else if (executeTask.get() != null && !executeTask.get().isDone()) {
            try {
                switch (compExeType) {
                case StartAsInit:
                case StartAsRun:
                    compExeRelatedInstances.component.get().onStartInterrupted(new ThreadHandler(executeTask.get()));
                    break;
                case ProcessInputs:
                    compExeRelatedInstances.component.get().onProcessInputsInterrupted(new ThreadHandler(executeTask.get()));
                    break;
                default:
                    break;
                }
            } catch (RuntimeException e) {
                LOG.error(StringUtils.format("Failed to interrupt task that is executing '%s' of %s",
                    compExeType.name(),
                    ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)), e);
            }
        }
    }

    private void callCompleteStartOrProcessInputsOnFailure() {
        try {
            compExeRelatedInstances.component.get().completeStartOrProcessInputsAfterFailure();
        } catch (ComponentException | RuntimeException e) {
            LOG.error(StringUtils.format("Error in 'completeStartOrProcessInputsAfterFailure' after executing %s", compExeType.name(),
                ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowUpperCase(compExeRelatedInstances.compExeCtx)), e);
        }
    }

    protected void bindTypedDatumService(TypedDatumService newService) {
        ComponentExecutor.typedDatumService = newService;
    }

    protected void bindComponentExecutionPermitsService(ComponentExecutionPermitsService newService) {
        ComponentExecutor.componentExecutionPermitService = newService;
    }

    protected void bindEndpointDatumSerializer(EndpointDatumSerializer newService) {
        ComponentExecutor.endpointDatumSerializer = newService;
    }

    protected void bindComponentExecutionStatsService(ComponentExecutionStatsService newService) {
        ComponentExecutor.compExeStatsService = newService;
    }

    protected void bindConfigurationService(ConfigurationService newService) {
        ComponentExecutor.sendInputsProcessedToWfCtrl = newService.getConfigurationSegment("general")
            .getBoolean(ComponentConstants.CONFIG_KEY_ENABLE_INPUT_TAB, false);
    }

}
