/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.impl.ComponentContextImpl;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionContextImpl;
import de.rcenvironment.core.component.execution.internal.ComponentExecutor.ComponentExecutionType;
import de.rcenvironment.core.component.model.api.ComponentInterface;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescriptionsManager;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.EndpointCharacter;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.AbstractStateMachine;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.core.utils.incubator.StateChangeException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.utils.common.IdGenerator;

/**
 * Component-specific implementation of {@link AbstractStateMachine}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (tweaked error handling)
 * 
 * Note: The component state transition graph created with the definition of the valid state transition seems to have some flaws. I
 * wouldn't trust it to be rock-solid. Nevertheless, it works right now and I wouldn't touch it except the workflow engine I gets a
 * re-design instead of replacing it with a workflow engine II.
 * --seid_do
 * 
 */
public class ComponentStateMachine extends AbstractFixedTransitionsStateMachine<ComponentState, ComponentStateMachineEvent> {

    private static final Log LOG = LogFactory.getLog(ComponentStateMachine.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(ComponentStateMachine.class);

    private static final int HEARTBEAT_SEND_INTERVAL_MSEC = 30 * 1000;

    private static final ComponentState[][] VALID_COMPONENT_STATE_TRANSITIONS = new ComponentState[][] {

        // normal life cycle
        { ComponentState.INIT, ComponentState.PREPARING },
        { ComponentState.PREPARING, ComponentState.PREPARED },
        { ComponentState.PREPARED, ComponentState.WAITING_FOR_RESOURCES },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.STARTING },
        { ComponentState.STARTING, ComponentState.IDLING },
        { ComponentState.STARTING, ComponentState.WAITING_FOR_APPROVAL },
        { ComponentState.IDLING, ComponentState.WAITING_FOR_RESOURCES },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.WAITING_FOR_RESOURCES },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.PROCESSING_INPUTS },
        { ComponentState.PROCESSING_INPUTS, ComponentState.IDLING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.WAITING_FOR_APPROVAL },
        { ComponentState.WAITING_FOR_APPROVAL, ComponentState.IDLING },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.RESETTING },
        { ComponentState.IDLING, ComponentState.IDLING },
        { ComponentState.RESETTING, ComponentState.IDLING_AFTER_RESET },
        { ComponentState.IDLING, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.IDLING_AFTER_RESET },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.TEARING_DOWN },
        { ComponentState.TEARING_DOWN, ComponentState.FINISHED },
        { ComponentState.TEARING_DOWN, ComponentState.FINISHED_WITHOUT_EXECUTION },
        { ComponentState.FINISHED_WITHOUT_EXECUTION, ComponentState.DISPOSING },
        { ComponentState.FINISHED, ComponentState.DISPOSING },
        { ComponentState.DISPOSING, ComponentState.DISPOSED },
        // canceling
        { ComponentState.PREPARING, ComponentState.CANCELLING },
        { ComponentState.PREPARED, ComponentState.CANCELLING },
        { ComponentState.STARTING, ComponentState.CANCELLING },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.CANCELLING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.CANCELLING },
        { ComponentState.WAITING_FOR_APPROVAL, ComponentState.CANCELLING },
        { ComponentState.IDLING, ComponentState.CANCELLING },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.CANCELLING },
        { ComponentState.RESETTING, ComponentState.CANCELLING },
        { ComponentState.STARTING, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.PROCESSING_INPUTS, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.IDLING, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.RESETTING, ComponentState.CANCELLING_AFTER_FAILURE },
        { ComponentState.CANCELLING, ComponentState.TEARING_DOWN },
        { ComponentState.CANCELLING_AFTER_FAILURE, ComponentState.TEARING_DOWN },
        { ComponentState.TEARING_DOWN, ComponentState.CANCELED },
        { ComponentState.CANCELED, ComponentState.DISPOSING },
        // pausing
        { ComponentState.PREPARING, ComponentState.PAUSING },
        { ComponentState.PREPARED, ComponentState.PAUSING },
        { ComponentState.STARTING, ComponentState.PAUSING },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.PAUSING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.PAUSING },
        { ComponentState.WAITING_FOR_APPROVAL, ComponentState.PAUSING },
        { ComponentState.IDLING, ComponentState.PAUSING },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.PAUSING },
        { ComponentState.RESETTING, ComponentState.PAUSING },
        { ComponentState.PAUSING, ComponentState.PAUSED },
        { ComponentState.PAUSING, ComponentState.PROCESSING_INPUTS },
        { ComponentState.PAUSED, ComponentState.CANCELLING },
        { ComponentState.PAUSED, ComponentState.RESUMING },
        { ComponentState.RESUMING, ComponentState.PREPARING },
        { ComponentState.RESUMING, ComponentState.PREPARED },
        { ComponentState.RESUMING, ComponentState.WAITING_FOR_RESOURCES },
        { ComponentState.RESUMING, ComponentState.IDLING },
        { ComponentState.RESUMING, ComponentState.IDLING_AFTER_RESET },
        // failures
        { ComponentState.PREPARING, ComponentState.TEARING_DOWN },
        { ComponentState.PREPARED, ComponentState.TEARING_DOWN },
        { ComponentState.STARTING, ComponentState.TEARING_DOWN },
        { ComponentState.WAITING_FOR_RESOURCES, ComponentState.TEARING_DOWN },
        { ComponentState.PROCESSING_INPUTS, ComponentState.TEARING_DOWN },
        { ComponentState.WAITING_FOR_APPROVAL, ComponentState.TEARING_DOWN },
        { ComponentState.PAUSING, ComponentState.TEARING_DOWN },
        { ComponentState.PAUSED, ComponentState.TEARING_DOWN },
        { ComponentState.RESUMING, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.TEARING_DOWN },
        { ComponentState.CANCELLING, ComponentState.TEARING_DOWN },
        { ComponentState.RESETTING, ComponentState.TEARING_DOWN },
        { ComponentState.TEARING_DOWN, ComponentState.FAILED },
        { ComponentState.TEARING_DOWN, ComponentState.RESULTS_REJECTED },
        { ComponentState.FAILED, ComponentState.DISPOSING },
        { ComponentState.RESULTS_REJECTED, ComponentState.DISPOSING }
    };

    private static ComponentExecutionService comExeService;

    private static ComponentExecutionStatsService compExeStatsService;

    private static ComponentExecutionRelatedInstancesFactory compExeInstancesFactory;

    // visibility is protected for test purposes
    protected final Map<ComponentStateMachineEventType, EventProcessor> eventProcessors = new HashMap<>();

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private AtomicReference<ComponentExecutor> compExecutorRef = new AtomicReference<ComponentExecutor>(null);

    private Future<?> currentTask = null;

    private String errorId = null;

    private String errorMessage = null;

    private boolean pauseWasRequested = false;

    private ComponentStateMachineEvent lastEventBeforePaused;

    private ComponentContextImpl componentContext;

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private SortedSet<Integer> executionCountOnResets = new TreeSet<>();

    private ScheduledFuture<?> heartbeatFuture;

    private volatile String latestVerificationToken;

    private Runnable heartbeatRunnable = new Runnable() {

        @Override
        @TaskDescription("Send heartbeat for component")
        public void run() {
            if (VERBOSE_LOGGING) {
                LOG.debug("Sending component heartbeat: " + compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
            }
            compExeRelatedInstances.wfExeCtrlBridgeDelegator
                .onComponentHeartbeatReceived(compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
        }
    };

    @Deprecated
    public ComponentStateMachine() {
        super(ComponentState.INIT, VALID_COMPONENT_STATE_TRANSITIONS);
    }

    public ComponentStateMachine(ComponentExecutionRelatedInstances compExeRelatedInstances) {
        super(ComponentState.INIT, VALID_COMPONENT_STATE_TRANSITIONS);
        this.compExeRelatedInstances = compExeRelatedInstances;

        boolean isNestedLoopDriver = Boolean.valueOf(compExeRelatedInstances.compExeCtx.getComponentDescription()
            .getConfigurationDescription().getConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));
        compExeRelatedInstances.isNestedLoopDriver = isNestedLoopDriver;
        compExeRelatedInstances.compExeRelatedStates = new ComponentExecutionRelatedStates();

        compExeRelatedInstances.typedDatumToOutputWriter = compExeInstancesFactory.createTypedDatumToOutputWriter(compExeRelatedInstances);
        compExeRelatedInstances.wfExeCtrlBridgeDelegator =
            compExeInstancesFactory.createWorkflowExecutionControllerBridgeDelegator(compExeRelatedInstances);
        compExeRelatedInstances.batchingConsoleRowsForwarder =
            compExeInstancesFactory.createBatchingConsoleRowsForwarder(compExeRelatedInstances);
        compExeRelatedInstances.consoleRowsSender = compExeInstancesFactory.createConsoleRowsSender(compExeRelatedInstances);
        compExeRelatedInstances.compCtxBridge = compExeInstancesFactory.createComponentContextBridge(compExeRelatedInstances);

        heartbeatFuture = threadPool.scheduleAtFixedRateAfterDelay(heartbeatRunnable, Math.round(Math.random() * 10),
            HEARTBEAT_SEND_INTERVAL_MSEC);

        initializeEventProcessors();
    }

    // visibility is protected for test purposes
    protected void initializeEventProcessors() {
        eventProcessors.put(ComponentStateMachineEventType.PREPARE_REQUESTED, new PrepareRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.START_REQUESTED, new StartRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.PROCESSING_INPUT_DATUMS_REQUESTED,
            new ProcessingInputsDatumRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RUNNING, new RunningEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESET_REQUESTED, new ResetRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.PAUSE_REQUESTED, new PauseRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESUME_REQUESTED, new ResumeRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.CANCEL_REQUESTED, new CancelRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.DISPOSE_REQUESTED, new DisposeRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.PREPARATION_SUCCESSFUL, new PreparationSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.START_SUCCESSFUL, new StartSuccessfulEventProcessor());
        StartAsRunOrProcessingInputsSuccessfulEventProcessor startOrProcessingInputsSuccessfulEventProcessor =
            new StartAsRunOrProcessingInputsSuccessfulEventProcessor();
        eventProcessors.put(ComponentStateMachineEventType.START_AS_RUN_SUCCESSFUL, startOrProcessingInputsSuccessfulEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.PROCESSING_INPUTS_SUCCESSFUL, startOrProcessingInputsSuccessfulEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.RESULT_APPROVAL_REQUESTED, new ResultsApprovalRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESULTS_APPROVED, new ResultsApprovedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESULTS_REJECTED, new ResultsRejectedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESULTS_APPROVED_COMPLETED,
            new ResultsApprovedCompletedSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESULTS_REJECTED_COMPLETED,
            new ResultsRejectedCompletedSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.IDLE_REQUESTED, new IdleRequestedEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.RESET_SUCCESSFUL, new ResetSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL, new CancelAttemptSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL, new DisposeAttemptSuccessfulEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.NEW_SCHEDULING_STATE, new NewSchedulingStateEventProcessor());
        eventProcessors.put(ComponentStateMachineEventType.FINISHED, new FinishedEventProcessor());
        FailedEventProcessor failedEventProcessor = new FailedEventProcessor();
        eventProcessors.put(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.START_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.RESET_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.PREPARATION_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.SCHEDULING_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.PAUSE_ATTEMPT_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.WF_CRTL_CALLBACK_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.VERIFICATION_COMPLETION_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.HANDLE_VERIFICATION_TOKEN_FAILED, failedEventProcessor);
        eventProcessors.put(ComponentStateMachineEventType.TEARED_DOWN, new TearedDownEventProcessor());
    }

    /**
     * @return latest verification token if component is in {@link ComponentState#WAITING_FOR_APPROVAL} (or {@link ComponentState#PAUSING}),
     *         otherwise <code>null</code> in case of another {@link ComponentState} or in case no token exists.
     */
    public String getVerificationToken() {
        if (getState().equals(ComponentState.WAITING_FOR_APPROVAL) || getState().equals(ComponentState.PAUSING)) {
            return latestVerificationToken;
        } else {
            return null;
        }
    }

    public boolean isWorkflowControllerReachable() {
        return compExeRelatedInstances.wfExeCtrlBridgeDelegator.isWorkflowControllerReachable();
    }

    private boolean checkStateChange(ComponentState currentState, ComponentState newState, ComponentStateMachineEvent event) {
        if (isStateChangeValid(currentState, newState)) {
            return true;
        } else {
            logInvalidStateChangeRequest(currentState, newState, event);
            return false;
        }
    }

    @Override
    protected ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) throws StateChangeException {
        if (pauseWasRequested && event.getType() != ComponentStateMachineEventType.RUNNING) {
            pauseWasRequested = false;
            if (!ComponentConstants.FINAL_COMPONENT_STATES_WITH_DISPOSED.contains(currentState)) {
                lastEventBeforePaused = event;
                return ComponentState.PAUSED;
            }
        }
        return eventProcessors.get(event.getType()).processEvent(currentState, event);
    }

    private void handleFailureEvent(ComponentStateMachineEvent event) {
        Throwable throwable = event.getThrowable();
        if (throwable != null) {
            final String message = StringUtils.format("Executing component '%s' (%s) failed",
                compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
            if (throwable.getCause() != null) {
                errorId = LogUtils.logExceptionWithStacktraceAndAssignUniqueMarker(LOG, message, throwable);
            } else {
                errorId = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(LOG, message, throwable);
            }
            errorMessage = ComponentUtils.createErrorLogMessage(throwable);
        } else if (event.getErrorId() != null) {
            errorId = event.getErrorId();
        }
    }

    private ComponentState handleFailure(ComponentState currentState, ComponentStateMachineEvent event) {

        handleFailureEvent(event);

        ComponentState finalCompState = ComponentState.FAILED;

        switch (event.getType()) {
        case WF_CRTL_CALLBACK_FAILED:
        case SCHEDULING_FAILED:
            cancelAsync();
            return ComponentState.CANCELLING_AFTER_FAILURE;
        default:
            currentTask = null;
            tearDownAsync(finalCompState);
            return ComponentState.TEARING_DOWN;
        }
    }

    private ComponentState handleFinished() {
        if (compExeRelatedInstances.compExeRelatedStates.executionCount.get() == 0) {
            tearDownAsync(ComponentState.FINISHED_WITHOUT_EXECUTION);
        } else {
            tearDownAsync(ComponentState.FINISHED);
        }
        return ComponentState.TEARING_DOWN;
    }

    private void logInvalidStateChangeRequest(ComponentState currentState, ComponentState requestedState,
        ComponentStateMachineEvent event) {
        LOG.debug(StringUtils.format("Ignored component state change request for component '%s' (%s) of workflow '%s' (%s) "
            + "as it will cause an invalid state transition: %s -> %s; event was: %s",
            compExeRelatedInstances.compExeCtx.getInstanceName(),
            compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
            compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
            compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier(), currentState, requestedState,
            event.getType().name()));
    }

    @Override
    protected void onStateChanged(ComponentState oldState, ComponentState newState) {
        LOG.debug(StringUtils.format("%s is now %s (previous state: %s)",
            ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowUpperCase(compExeRelatedInstances.compExeCtx), newState,
            oldState));

        if (newState.equals(ComponentState.FAILED)) {
            if (errorMessage != null) {
                compExeRelatedInstances.wfExeCtrlBridgeDelegator.onComponentStateChanged(
                    compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                    newState, compExeRelatedInstances.compExeRelatedStates.executionCount.get(),
                    getExecutionCountsOnResetAsString(), errorId, errorMessage);
                errorMessage = null;
            } else {
                compExeRelatedInstances.wfExeCtrlBridgeDelegator.onComponentStateChanged(
                    compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                    newState, compExeRelatedInstances.compExeRelatedStates.executionCount.get(),
                    getExecutionCountsOnResetAsString(), errorId);
            }
            errorId = null;
        } else {
            compExeRelatedInstances.wfExeCtrlBridgeDelegator.onComponentStateChanged(
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                newState,
                compExeRelatedInstances.compExeRelatedStates.executionCount.get(), getExecutionCountsOnResetAsString());
        }

        if (ComponentConstants.FINAL_COMPONENT_STATES.contains(newState)) {
            compExeRelatedInstances.consoleRowsSender.sendStateAsConsoleRow(WorkflowLifecyleEventType.COMPONENT_TERMINATED);
            heartbeatFuture.cancel(false);
            compExeStatsService.addStatsAtComponentTermination(compExeRelatedInstances.compExeCtx, newState);
            if (!compExeRelatedInstances.compExeCtx.getComponentDescription().performLazyDisposal()) {
                try {
                    comExeService.dispose(compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                        compExeRelatedInstances.compExeCtx.getComponentDescription().getNode());
                } catch (ExecutionControllerException | RemoteOperationException e) {
                    // should not happen; in case it does at least call the component's dispose method anyway
                    LOG.error(StringUtils.format("Failed to dispose component execution controller for %s",
                        ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)),
                        e);
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_REQUESTED));
                }
            }
        } else if (newState == ComponentState.STARTING) {
            compExeRelatedInstances.consoleRowsSender.sendStateAsConsoleRow(WorkflowLifecyleEventType.COMPONENT_STARTING);
        }
    }

    private String getExecutionCountsOnResetAsString() {
        List<String> counts = new ArrayList<>();
        synchronized (executionCountOnResets) {
            for (Integer countOnReset : executionCountOnResets) {
                counts.add(String.valueOf(countOnReset));
            }
            if (!executionCountOnResets.contains(compExeRelatedInstances.compExeRelatedStates.executionCount.get())) {
                counts.add(String.valueOf(compExeRelatedInstances.compExeRelatedStates.executionCount.get()));
            }
        }
        return StringUtils.escapeAndConcat(counts);
    }

    @Override
    protected void onStateChangeException(ComponentStateMachineEvent event, StateChangeException e) {
        LOG.error(
            StringUtils.format("Invalid state change for component '%s' (%s) of workflow '%s' (%s) attempt, caused by event '%s'",
                compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier(), event),
            e);
    }

    private void prepareAsync() {
        currentTask = threadPool.submit(new AsyncPrepareTask());
    }

    private void startAsync() {
        ComponentExecutor.ComponentExecutionType compExeType = ComponentExecutor.ComponentExecutionType.StartAsInit;
        synchronized (compExeRelatedInstances.component) {
            if (compExeRelatedInstances.component.get().treatStartAsComponentRun()) {
                compExeType = ComponentExecutor.ComponentExecutionType.StartAsRun;
            }
        }
        compExecutorRef.set(new ComponentExecutor(compExeRelatedInstances, compExeType));
        currentTask = threadPool.submit(new AsyncStartTask(compExeType));
    }

    private void processInputsAsync() {
        compExecutorRef.set(new ComponentExecutor(compExeRelatedInstances, ComponentExecutor.ComponentExecutionType.ProcessInputs));
        currentTask = threadPool.submit(new AsyncProcessInputsTask());
    }

    private void handleVerificationTokenAsync(String verificationToken) {
        ComponentExecutionType compExeType = ComponentExecutor.ComponentExecutionType.HandleVerificationToken;
        compExeType.setVerificationToken(verificationToken);
        latestVerificationToken = verificationToken;
        compExecutorRef.set(new ComponentExecutor(compExeRelatedInstances, compExeType));
        currentTask = threadPool.submit(new AsyncHandleVerificationTokenTask());
    }

    private void completeVerificationAsync(boolean outputsApproved) {
        ComponentExecutionType compExeType = ComponentExecutor.ComponentExecutionType.CompleteVerification;
        if (outputsApproved) {
            compExeType.setFinalComponentStateAfterRun(FinalComponentRunState.RESULTS_APPROVED);            
        } else {
            compExeType.setFinalComponentStateAfterRun(FinalComponentRunState.RESULTS_REJECTED);
        }
        compExecutorRef.set(new ComponentExecutor(compExeRelatedInstances, ComponentExecutor.ComponentExecutionType.CompleteVerification));
        currentTask = threadPool.submit(new AsyncCompleteVerificationTask(outputsApproved));
    }

    private void resetAsync() {
        compExecutorRef.set(new ComponentExecutor(compExeRelatedInstances, ComponentExecutor.ComponentExecutionType.Reset));
        currentTask = threadPool.submit(new AsyncResetTask());
    }

    private void cancelAsync() {
        threadPool.submit(new AsyncCancelTask(currentTask));
    }

    private void tearDownAsync(final ComponentState compState) {
        final Component.FinalComponentState finalCompState;
        FinalComponentState finalStateForDm = null;
        switch (compState) {
        case FINISHED_WITHOUT_EXECUTION:
            finalCompState = Component.FinalComponentState.FINISHED;
            finalStateForDm = FinalComponentState.FINISHED_WITHOUT_EXECUTION;
            break;
        case FINISHED:
            finalCompState = Component.FinalComponentState.FINISHED;
            finalStateForDm = FinalComponentState.FINISHED;
            break;
        case FAILED:
            finalCompState = Component.FinalComponentState.FAILED;
            finalStateForDm = FinalComponentState.FAILED;
            break;
        case RESULTS_REJECTED:
            finalCompState = Component.FinalComponentState.RESULTS_REJECTED;
            finalStateForDm = FinalComponentState.RESULTS_REJECTED;
            break;
        case CANCELED:
            finalCompState = Component.FinalComponentState.CANCELLED;
            finalStateForDm = FinalComponentState.CANCELLED;
            break;
        default:
            finalCompState = null;
            break;
        }
        ComponentExecutionType compExeType = ComponentExecutionType.TearDown;
        compExeType.setFinalComponentStateAfterTearedDown(finalCompState);
        ComponentExecutor compExecutor = new ComponentExecutor(compExeRelatedInstances, compExeType);
        compExecutorRef.set(compExecutor);
        currentTask = threadPool.submit(new AsyncTearDownTask(compState, finalCompState, finalStateForDm));
    }

    private void disposeAsync() {
        currentTask = threadPool.submit(new AsyncDisposeTask());
    }

    private void checkForIntermediateButNoFinalHistoryDataItemWritten() throws ComponentExecutionException {
        if (!compExeRelatedInstances.compExeRelatedStates.finalHistoryDataItemWritten.get()
            && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
            if (compExeRelatedInstances.compExeRelatedStates.intermediateHistoryDataWritten.get()) {
                LOG.warn(StringUtils.format("No final history data item was written for %s even if intermediate ones were",
                    ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
            }
        }
        compExeRelatedInstances.compExeRelatedStates.intermediateHistoryDataWritten.set(false);
        compExeRelatedInstances.compExeRelatedStates.finalHistoryDataItemWritten.set(false);
    }

    private void idle() {
        if (!compExeRelatedInstances.compExeCtx.isConnectedToEndpointDatumSenders()) {
            compExeRelatedInstances.compCtxBridge.closeAllOutputs();
            postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.FINISHED));
        } else {
            compExeRelatedInstances.compExeScheduler.enable();
        }
    }

    /**
     * Prepares the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncPrepareTask implements Runnable {

        @Override
        @TaskDescription("Prepare component")
        public void run() {
            compExeStatsService.addStatsAtComponentStart(compExeRelatedInstances.compExeCtx);
            try {
                compExeRelatedInstances.component.set(createNewComponentInstance());
                ((ComponentExecutionContextImpl) compExeRelatedInstances.compExeCtx)
                    .setWorkingDirectory(createWorkingDirectory());
                componentContext = new ComponentContextImpl(compExeRelatedInstances.compExeCtx, compExeRelatedInstances.compCtxBridge);
                synchronized (compExeRelatedInstances.component) {
                    compExeRelatedInstances.component.get().setComponentContext(componentContext);
                }
                compExeRelatedInstances.compExeScheduler.initialize(compExeRelatedInstances.compExeCtx);
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARATION_SUCCESSFUL));
            } catch (ComponentExecutionException | RuntimeException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARATION_FAILED, e));
            }
        }

        private Component createNewComponentInstance() throws ComponentExecutionException {
            final String message = StringUtils.format("Failed to instantiate component '%s' (%s) of workflow '%s' (%s)",
                compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier());
            try {
                return (Component) Class
                    .forName(compExeRelatedInstances.compExeCtx.getComponentDescription().getClassName()).getConstructor()
                    .newInstance();
            } catch (SecurityException | NoSuchMethodException | ClassNotFoundException | IllegalArgumentException
                | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new ComponentExecutionException(message, e);
            }
        }
    }

    /**
     * Starts the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncStartTask implements Runnable {

        private final ComponentExecutor.ComponentExecutionType compExeType;

        protected AsyncStartTask(ComponentExecutor.ComponentExecutionType compExeType) {
            this.compExeType = compExeType;
        }

        @Override
        @TaskDescription("Start component")
        public void run() {
            try {
                compExecutorRef.get().executeByConsideringLimitations();
                if (compExeType == ComponentExecutor.ComponentExecutionType.StartAsRun) {
                    checkForIntermediateButNoFinalHistoryDataItemWritten();
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_AS_RUN_SUCCESSFUL));
                } else {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_SUCCESSFUL));
                }
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_FAILED, e));
            } catch (ComponentException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_FAILED, e.getMessage()));
            }
        }
    }

    private File createWorkingDirectory() throws ComponentExecutionException {
        try {
            return TempFileServiceAccess.getInstance()
                .createManagedTempDir("cmp-" + compExeRelatedInstances.compExeCtx.getExecutionIdentifier());
        } catch (IOException e) {
            throw new ComponentExecutionException(StringUtils.format("Failed to create working directory for component '%s' "
                + "(%s) of workflow '%s' (%s)", compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getExecutionIdentifier(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowExecutionIdentifier()), e);
        }
    }

    /**
     * Resets the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncResetTask implements Runnable {

        @Override
        @TaskDescription("Reset component")
        public void run() {
            try {
                try {
                    compExecutorRef.get().executeByConsideringLimitations();
                } finally {
                    synchronized (executionCountOnResets) {
                        executionCountOnResets.add(compExeRelatedInstances.compExeRelatedStates.executionCount.get());
                    }
                }
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_SUCCESSFUL));
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_FAILED, e));
            } catch (ComponentException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_FAILED, e.getMessage()));
            }
        }
    }

    /**
     * Let the component process inputs.
     * 
     * @author Doreen Seider
     */
    private final class AsyncProcessInputsTask implements Runnable {

        @Override
        @TaskDescription("Let component process inputs")
        public void run() {
            try {
                compExeRelatedInstances.compExeRelatedStates.executionCount.incrementAndGet();
                compExecutorRef.get().executeByConsideringLimitations();
                checkForIntermediateButNoFinalHistoryDataItemWritten();
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_SUCCESSFUL));
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED, e));
            } catch (ComponentException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED,
                    e.getMessage()));
            }
        }
    }

    /**
     * Let the component handle the verification token.
     * 
     * @author Doreen Seider
     */
    private final class AsyncHandleVerificationTokenTask implements Runnable {

        @Override
        @TaskDescription("Let component handle the verification token")
        public void run() {
            try {
                compExecutorRef.get().executeByConsideringLimitations();
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.HANDLE_VERIFICATION_TOKEN_FAILED, e));
            } catch (ComponentException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.HANDLE_VERIFICATION_TOKEN_FAILED,
                    e.getMessage()));
            }
        }
    }

    /**
     * Let the component complete the verification.
     * 
     * @author Doreen Seider
     */
    private final class AsyncCompleteVerificationTask implements Runnable {

        private final boolean outputsApprove;

        protected AsyncCompleteVerificationTask(boolean outputsApprove) {
            this.outputsApprove = outputsApprove;
        }

        @Override
        @TaskDescription("Let component complete verification")
        public void run() {
            try {
                compExecutorRef.get().executeByConsideringLimitations();
                if (outputsApprove) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESULTS_APPROVED_COMPLETED));
                } else {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESULTS_REJECTED_COMPLETED));
                }
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.VERIFICATION_COMPLETION_FAILED, e));
            } catch (ComponentException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.VERIFICATION_COMPLETION_FAILED,
                    e.getMessage()));
            }
        }
    }

    /**
     * Cancels the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncCancelTask implements Runnable {

        private static final int WAIT_INTERVAL_CANCEL_SEC = 70;

        private final Future<?> task;

        AsyncCancelTask(Future<?> future) {
            this.task = future;
        }

        @Override
        @TaskDescription("Cancel component")
        public void run() {
            ComponentExecutor compExecutor = compExecutorRef.get();
            if (compExecutor != null) {
                compExecutor.onCancelled();
            }
            if (task != null) {
                try {
                    task.get(WAIT_INTERVAL_CANCEL_SEC, TimeUnit.SECONDS);
                    cancelAttemptSuccessful();
                } catch (ExecutionException e) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                } catch (InterruptedException | TimeoutException e) {
                    task.cancel(true);
                }
            } else {
                cancelAttemptSuccessful();
            }
        }

        private void cancelAttemptSuccessful() {
            if (compExeRelatedInstances.compExeScheduler.isLoopResetRequested()) {
                compExeRelatedInstances.consoleRowsSender.sendLogFileWriteTriggerAsConsoleRow();
            }
            postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
        }
    }

    /**
     * Tears down the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncTearDownTask implements Runnable {

        private final ComponentState compState;

        private final Component.FinalComponentState finalCompState;

        private final FinalComponentState finalStateForDm;

        AsyncTearDownTask(ComponentState compState, Component.FinalComponentState finalCompState, FinalComponentState finalStateForDm) {
            this.compState = compState;
            this.finalCompState = finalCompState;
            this.finalStateForDm = finalStateForDm;
        }

        @Override
        @TaskDescription("Tear down component")
        public void run() {
            if (compExeRelatedInstances.component != null) {
                try {
                    compExecutorRef.get().executeByConsideringLimitations();
                } catch (ComponentExecutionException e) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                        ComponentState.FAILED, e));
                    return;
                } catch (ComponentException e) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                        ComponentState.FAILED, e.getMessage()));
                }
                if (finalCompState == Component.FinalComponentState.FAILED) {
                    try {
                        checkForIntermediateButNoFinalHistoryDataItemWritten();
                    } catch (ComponentExecutionException e) {
                        postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                            ComponentState.FAILED, e));
                        return;
                    }
                }
            }
            try {
                compExeRelatedInstances.compExeStorageBridge.setFinalComponentState(finalStateForDm);
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                    ComponentState.FAILED, e));
                return;
            }

            postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN, compState));
        }
    }

    /**
     * Disposes the component.
     * 
     * @author Doreen Seider
     */
    private final class AsyncDisposeTask implements Runnable {

        @Override
        @TaskDescription("Dispose component")
        public void run() {
            if (compExeRelatedInstances.component != null) {
                try {
                    synchronized (compExeRelatedInstances.component) {
                        compExeRelatedInstances.component.get().dispose();
                    }
                } catch (RuntimeException e) {
                    LOG.error("Failed to dispose "
                        + ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx),
                        e);
                    return;
                }
                disposeWorkingDirectory();
            }
            postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL));
        }
    }

    private void disposeWorkingDirectory() {
        if (compExeRelatedInstances.compExeCtx.getWorkingDirectory() != null) {
            try {
                TempFileServiceAccess.getInstance()
                    .disposeManagedTempDirOrFile(compExeRelatedInstances.compExeCtx.getWorkingDirectory());
            } catch (IOException e) {
                LOG.error(StringUtils.format("Failed to dispose working directory of %s ",
                    ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)), e);
            }
        }
    }

    /**
     * Processes {@link ComponentStateMachineEvent}s.
     * 
     * @author Doreen Seider
     */
    private interface EventProcessor {

        ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event);
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class PrepareRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.PREPARING, event)) {
                state = ComponentState.PREPARING;
                prepareAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class StartRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.WAITING_FOR_RESOURCES, event)) {
                state = ComponentState.WAITING_FOR_RESOURCES;
                synchronized (compExeRelatedInstances.component) {
                    if (compExeRelatedInstances.component.get().treatStartAsComponentRun()) {
                        compExeRelatedInstances.compExeRelatedStates.executionCount.incrementAndGet();
                    }
                }
                startAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ProcessingInputsDatumRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.WAITING_FOR_RESOURCES, event)) {
                state = ComponentState.WAITING_FOR_RESOURCES;
                processInputsAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class RunningEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, event.getNewComponentState(), event)) {
                state = event.getNewComponentState();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResetRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.WAITING_FOR_RESOURCES, event)) {
                state = ComponentState.WAITING_FOR_RESOURCES;
                resetAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class PauseRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            if (compExeRelatedInstances.compExeScheduler.isEnabled()) {
                compExeRelatedInstances.compExeScheduler.disable();
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PAUSE_REQUESTED));
                return currentState;
            } else {
                if (currentState == ComponentState.IDLING || currentState == ComponentState.IDLING_AFTER_RESET) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.IDLE_REQUESTED, currentState));
                }
                pauseWasRequested = true;
                return ComponentState.PAUSING;
            }
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResumeRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.RESUMING, event)) {
                state = ComponentState.RESUMING;
                postEvent(lastEventBeforePaused);
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class CancelRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.CANCELLING, event)) {
                state = ComponentState.CANCELLING;
                cancelAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class DisposeRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.DISPOSING, event)) {
                state = ComponentState.DISPOSING;
                disposeAsync();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class PreparationSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.PREPARED, event)) {
                currentTask = null;
                state = ComponentState.PREPARED;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResetSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.IDLING_AFTER_RESET, event)) {
                state = ComponentState.IDLING_AFTER_RESET;
                idle();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class CancelAttemptSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                if (currentState == ComponentState.CANCELLING_AFTER_FAILURE) {
                    tearDownAsync(ComponentState.FAILED);
                } else {
                    tearDownAsync(ComponentState.CANCELED);
                }
                state = ComponentState.TEARING_DOWN;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class DisposeAttemptSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.DISPOSED, event)) {
                state = ComponentState.DISPOSED;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class FinishedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                state = handleFinished();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class NewSchedulingStateEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            switch (compExeRelatedInstances.compExeScheduler.getSchedulingState()) {
            case FINISHED:
                forwardFinishToNonClosedOutputs();
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.FINISHED));
                break;
            case PROCESS_INPUT_DATA:
                requestProcessingInputDatums(compExeRelatedInstances.compExeScheduler.fetchEndpointDatums());
                break;
            case PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA:
                ComponentInterface compInterface =
                    compExeRelatedInstances.compExeCtx.getComponentDescription().getComponentInstallation()
                        .getComponentRevision().getComponentInterface();
                Map<String, EndpointDatum> endpointDatums = compExeRelatedInstances.compExeScheduler.fetchEndpointDatums();
                if (compInterface.getCanHandleNotAValueDataTypes()
                    || (compInterface.getIsLoopDriver() && wasNotAValueReceivedOnlyAtSameLoopInput(endpointDatums))) {
                    requestProcessingInputDatums(endpointDatums);
                } else {
                    // it must contain at least one of DataType 'not a value' if the state was
                    // PROCESS_INPUT_DATA_WITH_NOT_A_VALUE_DATA
                    for (EndpointDatum endpointDatum : endpointDatums.values()) {
                        if (endpointDatum.getValue().getDataType().equals(DataType.NotAValue)) {
                            forwardNotAValueData(endpointDatum);
                            break;
                        }
                    }
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.IDLE_REQUESTED));
                }
                break;
            case RESET:
                forwardInternalTD(compExeRelatedInstances.compExeScheduler.getResetDatum());
                if (!getState().equals(ComponentState.IDLING_AFTER_RESET)) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_REQUESTED));
                } else {
                    postEvent(
                        new ComponentStateMachineEvent(ComponentStateMachineEventType.IDLE_REQUESTED, ComponentState.IDLING_AFTER_RESET));
                }
                break;
            case FAILURE_FORWARD:
                forwardInternalTD(compExeRelatedInstances.compExeScheduler.getFailureDatum());
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.IDLE_REQUESTED));
                break;
            case LOOP_RESET:
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_REQUESTED));
                break;
            default:
                break;
            }
            return currentState;
        }

        private boolean wasNotAValueReceivedOnlyAtSameLoopInput(Map<String, EndpointDatum> endpointDatums) {
            EndpointDescriptionsManager inputDescManager =
                compExeRelatedInstances.compExeCtx.getComponentDescription().getInputDescriptionsManager();

            for (Entry<String, EndpointDatum> datum : endpointDatums.entrySet()) {
                if (datum.getValue().getValue().getDataType().equals(DataType.NotAValue)) {
                    if (inputDescManager.getEndpointDescription(datum.getKey()).getEndpointDefinition().getEndpointCharacter()
                        .equals(EndpointCharacter.OUTER_LOOP)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        private void forwardInternalTD(InternalTDImpl internalTD) {
            Queue<WorkflowGraphHop> hopsToTraverse = internalTD.getHopsToTraverse();
            WorkflowGraphHop currentHop = hopsToTraverse.poll();
            compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(
                currentHop.getHopOuputName(), internalTD,
                currentHop.getTargetExecutionIdentifier(), currentHop.getTargetInputName());
        }

        private void forwardFinishToNonClosedOutputs() {
            for (EndpointDescription output : compExeRelatedInstances.compExeCtx.getComponentDescription()
                .getOutputDescriptionsManager().getEndpointDescriptions()) {
                if (!compExeRelatedInstances.compCtxBridge.isOutputClosed(output.getName())) {
                    compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutput(output.getName(),
                        new InternalTDImpl(InternalTDImpl.InternalTDType.WorkflowFinish));
                }
            }
        }

        private void forwardNotAValueData(EndpointDatum nAVEndpointDatum) {
            ComponentInterface compInterface =
                compExeRelatedInstances.compExeCtx.getComponentDescription().getComponentInstallation()
                .getComponentRevision().getComponentInterface();
            for (EndpointDescription output : compExeRelatedInstances.compExeCtx.getComponentDescription()
                .getOutputDescriptionsManager().getEndpointDescriptions()) {
                if (!compInterface.getIsLoopDriver()
                    || output.getEndpointDefinition().getEndpointCharacter().equals(EndpointCharacter.OUTER_LOOP)) {
                    compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutput(output.getName(), nAVEndpointDatum.getValue());
                }
            }
            LOG.info(StringUtils.format("Component '%s' of workflow '%s' did not run because of 'not a value' "
                + "value at input '%s'", compExeRelatedInstances.compExeCtx.getInstanceName(),
                compExeRelatedInstances.compExeCtx.getWorkflowInstanceName(),
                nAVEndpointDatum.getInputName()));
        }

        private void requestProcessingInputDatums(Map<String, EndpointDatum> endpointDatums) {
            try {
                compExeRelatedInstances.compCtxBridge.setEndpointDatumsForExecution(endpointDatums);
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUT_DATUMS_REQUESTED));
            } catch (ComponentExecutionException e) {
                postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED));
            }
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class IdleRequestedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            ComponentState newCompState = event.getNewComponentState();
            if (newCompState == null) {
                newCompState = ComponentState.IDLING;
            }
            if (checkStateChange(currentState, newCompState, event)) {
                state = newCompState;
                idle();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class StartSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            if (!isCanceling(currentState)) {
                return new IdleRequestedEventProcessor().processEvent(currentState, event);
            }
            return currentState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class StartAsRunOrProcessingInputsSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState newState = currentState;
            if (!isCanceling(currentState)) {
                ConfigurationDescription compConfigDesc = compExeRelatedInstances.compExeCtx.getComponentDescription()
                    .getConfigurationDescription();
                if (ComponentExecutionUtils.isManualOutputVerificationRequired(compConfigDesc)) {
                    postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESULT_APPROVAL_REQUESTED));
                } else {
                    newState = new IdleRequestedEventProcessor().processEvent(currentState, event);
                }
            }
            return newState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class FailedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                state = handleFailure(currentState, event);
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class TearedDownEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, event.getNewComponentState(), event)) {
                handleFailureEvent(event);
                state = event.getNewComponentState();
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResultsApprovalRequestedEventProcessor implements EventProcessor {

        private static final int VERIFICATION_TOKEN_LENGTH = 32;

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            String verificationToken = IdGenerator.secureRandomHexString(VERIFICATION_TOKEN_LENGTH);
            handleVerificationTokenAsync(verificationToken);
            return ComponentState.WAITING_FOR_APPROVAL;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResultsApprovedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            latestVerificationToken = null;
            compExeRelatedInstances.compCtxBridge.flushOutputs();
            componentContext.getLog()
                .componentInfo("Result approved (was done by person responsible for the component) -> outputs sent");
            completeVerificationAsync(true);
            return currentState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResultsRejectedEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            componentContext.getLog()
                .componentWarn("Results rejected (was done by person responsible for the component) -> cancel");
            completeVerificationAsync(false);
            return currentState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResultsApprovedCompletedSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState newState = currentState;
            if (!isCanceling(currentState)) {
                newState = new IdleRequestedEventProcessor().processEvent(currentState, event);
            }
            return newState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResultsRejectedCompletedSuccessfulEventProcessor implements EventProcessor {

        @Override
        public ComponentState processEvent(ComponentState currentState, ComponentStateMachineEvent event) {
            ComponentState state = currentState;
            if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                state = ComponentState.TEARING_DOWN;
                tearDownAsync(ComponentState.RESULTS_REJECTED);
            }
            return state;
        }
    }

    private boolean isCanceling(ComponentState state) {
        return state == ComponentState.CANCELLING || state == ComponentState.CANCELLING_AFTER_FAILURE;
    }

    protected void bindComponentExecutionService(ComponentExecutionService newService) {
        ComponentStateMachine.comExeService = newService;
    }

    protected void bindComponentExecutionStatsService(ComponentExecutionStatsService newService) {
        ComponentStateMachine.compExeStatsService = newService;
    }

    protected void bindComponentExecutionRelatedInstancesFactory(ComponentExecutionRelatedInstancesFactory newService) {
        ComponentStateMachine.compExeInstancesFactory = newService;
    }
}
