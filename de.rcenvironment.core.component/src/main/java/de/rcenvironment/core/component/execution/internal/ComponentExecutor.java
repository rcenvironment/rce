/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.util.Map;
import java.util.Map.Entry;
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
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.execution.api.WorkflowGraphPath;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.datamodel.api.DataModelConstants;
import de.rcenvironment.core.datamodel.api.FinalComponentRunState;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Wrapper class that calls life cycle methods of components by considering optional limitations concerning the amount of parallel
 * executions.
 * 
 * @author Doreen Seider
 * 
 * Note: This class eroded over time as new functionality was added, like the support for canceling component runs or manual output
 * verification. With each new functionality added, it felt like working a bit "against" this class which showed to me that this
 * class or how it is coupled to the calling context doesn't satisfy the demand of having an extensible and maintainable class here
 * anymore. --seid_do
 * 
 */
public class ComponentExecutor {

    protected static final int DEAFULT_WAIT_INTERVAL_AFTER_CANCELLED_MSEC = 60000;

    protected static final int DEAFULT_WAIT_INTERVAL_NOT_RUN_MSEC = 120000;

    protected static int waitIntervalAfterCacelledCalledMSec = DEAFULT_WAIT_INTERVAL_AFTER_CANCELLED_MSEC;

    protected static int waitIntervalNotRunMSec = DEAFULT_WAIT_INTERVAL_NOT_RUN_MSEC;

    private static final Log LOG = LogFactory.getLog(ComponentExecutor.class);

    /**
     * Type of component execution to be performed.
     * 
     * @author Doreen Seider
     */
    protected enum ComponentExecutionType {

        StartAsInit(ComponentState.STARTING, ComponentStateMachineEventType.START_FAILED),
        StartAsRun(ComponentState.STARTING, ComponentStateMachineEventType.START_FAILED),
        ProcessInputs(ComponentState.PROCESSING_INPUTS, ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED),
        Reset(ComponentState.RESETTING),
        TearDown(),
        HandleVerificationToken(),
        CompleteVerification();

        private final ComponentState compStateOnSuccess;

        private final ComponentStateMachineEventType compStateMachineEventTypeOnFailure;

        private Component.FinalComponentState finalCompStateAfterTearedDown;
        
        private FinalComponentRunState finalCompRunState;

        private String verificationToken;

        ComponentExecutionType() {
            this.compStateOnSuccess = null;
            this.compStateMachineEventTypeOnFailure = null;
        }

        ComponentExecutionType(ComponentState compStateOnSuccess) {
            this.compStateOnSuccess = compStateOnSuccess;
            this.compStateMachineEventTypeOnFailure = null;
        }

        ComponentExecutionType(ComponentState compStateOnSuccess, ComponentStateMachineEventType compStateMachineEventTypeOnFailure) {
            this.compStateOnSuccess = compStateOnSuccess;
            this.compStateMachineEventTypeOnFailure = compStateMachineEventTypeOnFailure;
        }

        protected void setFinalComponentStateAfterTearedDown(Component.FinalComponentState finalState) {
            this.finalCompStateAfterTearedDown = finalState;
        }

        protected void setFinalComponentStateAfterRun(FinalComponentRunState finalState) {
            this.finalCompRunState = finalState;
        }
        
        protected void setVerificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
        }

    }

    private static TypedDatumService typedDatumService;

    private static ComponentExecutionPermitsService componentExecutionPermitService;

    private static EndpointDatumSerializer endpointDatumSerializer;

    private static ComponentExecutionStatsService compExeStatsService;

    private static boolean sendInputsProcessedToWfCtrl;

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private final AsyncOrderedExecutionQueue executionQueue = ConcurrencyUtils.getFactory().createAsyncOrderedExecutionQueue(
        AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private ComponentExecutionRelatedInstances compExeRelatedInstances;

    private ComponentExecutionType compExeType;

    private boolean isVerificationRequired;

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
        this.isVerificationRequired = ComponentExecutionUtils.isManualOutputVerificationRequired(compExeRelatedInstances.compExeCtx
            .getComponentDescription().getConfigurationDescription());
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
                    .postEvent(new ComponentStateMachineEvent(compExeType.compStateMachineEventTypeOnFailure, e));
            } catch (ExecutionException | InterruptedException e) {
                compExeRelatedInstances.compStateMachine
                    .postEvent(new ComponentStateMachineEvent(compExeType.compStateMachineEventTypeOnFailure, e));
            }
        }
    }

    protected void performExecutionAndReleasePermission() throws ComponentExecutionException, ComponentException {
        try {
            FinalComponentRunState finalState = FinalComponentRunState.FAILED;
            if (isCancelled.get()) {
                compExeRelatedInstances.compExeRelatedStates.isComponentCancelled.set(true);
                finalState = FinalComponentRunState.CANCELLED;
                return;
            }
            if (!isTearDown) {
                if (compExeType.compStateOnSuccess != null) {
                    compExeRelatedInstances.compStateMachine
                        .postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RUNNING, compExeType.compStateOnSuccess));
                }
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
                if (compExeType.finalCompRunState == null) {
                    finalState = FinalComponentRunState.FINISHED;                    
                } else {
                    finalState = compExeType.finalCompRunState;
                }
                if (compExeRelatedInstances.compExeRelatedStates.compHasSentConsoleRowLogMessages.get()) {
                    writesCompRunRelatedDataToDM = true;
                    prepareDmForComponentRunRelatedDataIfNeeded();
                }
            } catch (ComponentException e) {
                writesCompRunRelatedDataToDM = true;
                prepareDmForComponentRunRelatedDataIfNeeded();
                handleComponentExecutionFailure(e);
                finalState = FinalComponentRunState.FAILED;

            } finally {
                if (treatAsRun) {
                    compExeStatsService.addStatsAtComponentRunTermination(compExeRelatedInstances.compExeCtx);
                }
                // TODO improve condition when the component run is considered as done from a data management perspective
                if (treatAsRun && !compExeRelatedInstances.compExeScheduler.isLoopResetRequested() && !isVerificationRequired) {
                    finishExecutionFromDataManagementPerpective(finalState);
                } else if (compExeType.equals(ComponentExecutionType.Reset)
                    && (compExeRelatedInstances.compExeStorageBridge.hasUnfinishedComponentExecution() || writesCompRunRelatedDataToDM)) {
                    finishExecutionFromDataManagementPerpective(finalState);
                } else if (compExeType.equals(ComponentExecutionType.CompleteVerification)) {
                    finishExecutionFromDataManagementPerpective(finalState);
                } else if (writesCompRunRelatedDataToDM && !compExeRelatedInstances.compExeScheduler.isLoopResetRequested()
                    && !compExeType.equals(ComponentExecutionType.HandleVerificationToken) && !isVerificationRequired) {
                    finishExecutionFromDataManagementPerpective(finalState);
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

    private void finishExecutionFromDataManagementPerpective(FinalComponentRunState finalState) throws ComponentExecutionException {
        compExeRelatedInstances.consoleRowsSender.sendLogFileWriteTriggerAsConsoleRow();
        compExeRelatedInstances.compExeStorageBridge.setComponentExecutionFinished(finalState);
    }

    private void awaitExecution() throws ComponentException {
        ComponentException exception = null;
        try {
            if (compExeType == ComponentExecutionType.StartAsInit || compExeType == ComponentExecutionType.StartAsRun
                || compExeType == ComponentExecutionType.ProcessInputs || compExeType == ComponentExecutionType.HandleVerificationToken) {
                try {
                    exception = executeTask.get().get();
                } catch (CancellationException e) {
                    LOG.debug(StringUtils.format("Task was cancelled that was executing '%s' of %s", compExeType.name(),
                        ComponentExecutionUtils.getStringWithInfoAboutComponentAndWorkflowLowerCase(compExeRelatedInstances.compExeCtx)));
                }
            } else {
                try {
                    exception = executeTask.get().get(waitIntervalNotRunMSec, TimeUnit.MILLISECONDS);
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
            if (!executionLatch.await(waitIntervalAfterCacelledCalledMSec, TimeUnit.MILLISECONDS)) {
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
                // These kinds of signals are only executed after successful preparation of the component, hence we do not require null
                // checks on component.get()
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
            // The remaining events may be processed before the preparation of the component was complete, i.e., before
            // compExeRelatedInstance#component has been set to contain some valid reference. Hence, we are required to check for that
            // reference being null.
        } else if (compExeType == ComponentExecutionType.Reset) {
            final Component component = compExeRelatedInstances.component.get();
            if (component != null) {
                component.reset();
            }
        } else if (compExeType == ComponentExecutionType.TearDown) {
            final Component component = compExeRelatedInstances.component.get();
            if (component != null) {
                component.tearDown(compExeType.finalCompStateAfterTearedDown);
            }
        } else if (compExeType == ComponentExecutionType.HandleVerificationToken) {
            final Component component = compExeRelatedInstances.component.get();
            if (component != null) {
                component.handleVerificationToken(compExeType.verificationToken);
            }
        } else if (compExeType == ComponentExecutionType.CompleteVerification) {
            final Component component = compExeRelatedInstances.component.get();
            if (component != null) {
                compExeRelatedInstances.component.get().completeStartOrProcessInputsAfterVerificationDone();
            }
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
        compExeRelatedInstances.consoleRowsSender.sendLogMessageAsConsoleRow(Type.COMPONENT_ERROR, errorConsoleRow,
            compExeRelatedInstances.compExeRelatedStates.executionCount.get());

        WorkflowGraphNode loopDriver = compExeRelatedInstances.compExeCtx.getWorkflowGraph()
            .getLoopDriver(new ComponentExecutionIdentifier(compExeRelatedInstances.compExeCtx.getExecutionIdentifier()));
        if (loopDriver != null && loopDriver.isDrivingFaultTolerantLoop()) {
            writeFailureOutputData();
        } else {
            throw new ComponentException(errId);
        }
    }

    private void writeFailureOutputData() throws ComponentExecutionException {
        Map<String, Set<WorkflowGraphPath>> hopsToTraverseOnFailure =
            compExeRelatedInstances.compExeCtx.getWorkflowGraph()
                .getHopsToTraverseOnFailure(new ComponentExecutionIdentifier(compExeRelatedInstances.compExeCtx.getExecutionIdentifier()));
        for (String outputName : hopsToTraverseOnFailure.keySet()) {
            for (WorkflowGraphPath hops : hopsToTraverseOnFailure.get(outputName)) {
                WorkflowGraphHop firstHop = hops.poll();
                NotAValueTD notAValue = typedDatumService.getFactory().createNotAValue(
                    UUID.randomUUID().toString(), NotAValueTD.Cause.Failure);
                Long outputDmId = compExeRelatedInstances.compExeStorageBridge.addOutput(outputName,
                    typedDatumService.getSerializer().serialize(notAValue));
                compExeRelatedInstances.compExeScheduler.addNotAValueDatumSent(notAValue.getIdentifier());
                InternalTDImpl failureDatum = new InternalTDImpl(InternalTDImpl.InternalTDType.FailureInLoop,
                    notAValue.getIdentifier(), hops, String.valueOf(outputDmId));
                compExeRelatedInstances.typedDatumToOutputWriter.writeTypedDatumToOutputConsideringOnlyCertainInputs(outputName,
                    failureDatum,
                    firstHop.getTargetExecutionIdentifier().toString(), firstHop.getTargetInputName());
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
