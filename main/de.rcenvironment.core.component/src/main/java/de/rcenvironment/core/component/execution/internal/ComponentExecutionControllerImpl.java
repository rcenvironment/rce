/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.SerializationException;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentException;
import de.rcenvironment.core.component.datamanagement.api.ComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.DefaultComponentHistoryDataItem;
import de.rcenvironment.core.component.datamanagement.api.MergedComponentHistoryDateItem;
import de.rcenvironment.core.component.execution.api.Component;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionController;
import de.rcenvironment.core.component.execution.api.ComponentExecutionControllerCallback;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.ConsoleRowBuilder;
import de.rcenvironment.core.component.execution.api.EndpointDatumSerializer;
import de.rcenvironment.core.component.execution.api.ThreadHandler;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
import de.rcenvironment.core.component.execution.api.WorkflowGraphHop;
import de.rcenvironment.core.component.execution.impl.ComponentContextImpl;
import de.rcenvironment.core.component.execution.impl.ComponentExecutionContextImpl;
import de.rcenvironment.core.component.execution.internal.ExecutionScheduler.State;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatum;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.model.endpoint.impl.EndpointDatumImpl;
import de.rcenvironment.core.datamanagement.DistributedMetaDataService;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.datamodel.api.DataTypeException;
import de.rcenvironment.core.datamodel.api.FinalComponentState;
import de.rcenvironment.core.datamodel.api.TypedDatum;
import de.rcenvironment.core.datamodel.api.TypedDatumConverter;
import de.rcenvironment.core.datamodel.api.TypedDatumService;
import de.rcenvironment.core.datamodel.types.api.NotAValueTD;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.AbstractStateMachine;
import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Implementation of {@link ComponentExecutionController}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (tweaked error handling)
 */
public class ComponentExecutionControllerImpl implements ComponentExecutionController {

    private static final int MAX_CALLBACK_FAILURES = 9;

    private static final Log LOG = LogFactory.getLog(ComponentExecutionControllerImpl.class);

    private static final int HEARTBEAT_SEND_INTERVAL_MSEC = 30 * 1000;

    private static ComponentExecutionPermitsService componentExecutionPermitService;

    private static EndpointDatumSender endpointDatumSender;

    private static TypedDatumService typedDatumService;

    private static DistributedMetaDataService metaDataService;

    private static final ComponentState[][] VALID_COMPONENT_STATE_TRANSITIONS = new ComponentState[][] {

        // normal life cycle
        { ComponentState.INIT, ComponentState.PREPARING },
        { ComponentState.PREPARING, ComponentState.PREPARED },
        { ComponentState.PREPARED, ComponentState.WAITING },
        { ComponentState.WAITING, ComponentState.STARTING },
        { ComponentState.STARTING, ComponentState.IDLING },
        { ComponentState.IDLING, ComponentState.WAITING },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.WAITING },
        { ComponentState.WAITING, ComponentState.PROCESSING_INPUTS },
        { ComponentState.PROCESSING_INPUTS, ComponentState.IDLING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING, ComponentState.RESETTING },
        { ComponentState.RESETTING, ComponentState.IDLING_AFTER_RESET },
        { ComponentState.IDLING, ComponentState.TEARING_DOWN },
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
        { ComponentState.WAITING, ComponentState.CANCELLING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.CANCELLING },
        { ComponentState.IDLING, ComponentState.CANCELLING },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.CANCELLING },
        { ComponentState.STARTING, ComponentState.CANCELLING_AFTER_INPUT_FAILURE },
        { ComponentState.WAITING, ComponentState.CANCELLING_AFTER_INPUT_FAILURE },
        { ComponentState.PROCESSING_INPUTS, ComponentState.CANCELLING_AFTER_INPUT_FAILURE },
        { ComponentState.IDLING, ComponentState.CANCELLING_AFTER_INPUT_FAILURE },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.CANCELLING_AFTER_INPUT_FAILURE },
        { ComponentState.CANCELLING, ComponentState.TEARING_DOWN },
        { ComponentState.CANCELLING_AFTER_INPUT_FAILURE, ComponentState.TEARING_DOWN },
        { ComponentState.TEARING_DOWN, ComponentState.CANCELED },
        { ComponentState.CANCELED, ComponentState.DISPOSING },
        // pausing
        { ComponentState.PREPARING, ComponentState.PAUSING },
        { ComponentState.PREPARED, ComponentState.PAUSING },
        { ComponentState.STARTING, ComponentState.PAUSING },
        { ComponentState.WAITING, ComponentState.PAUSING },
        { ComponentState.PROCESSING_INPUTS, ComponentState.PAUSING },
        { ComponentState.IDLING, ComponentState.PAUSING },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.PAUSING },
        { ComponentState.PAUSING, ComponentState.PAUSED },
        { ComponentState.PAUSED, ComponentState.CANCELLING },
        { ComponentState.PAUSED, ComponentState.RESUMING },
        { ComponentState.RESUMING, ComponentState.PREPARING },
        { ComponentState.RESUMING, ComponentState.PREPARED },
        { ComponentState.RESUMING, ComponentState.WAITING },
        { ComponentState.WAITING, ComponentState.STARTING },
        { ComponentState.RESUMING, ComponentState.IDLING },
        { ComponentState.RESUMING, ComponentState.IDLING_AFTER_RESET },
        // failures
        { ComponentState.PREPARING, ComponentState.TEARING_DOWN },
        { ComponentState.STARTING, ComponentState.TEARING_DOWN },
        { ComponentState.WAITING, ComponentState.TEARING_DOWN },
        { ComponentState.PROCESSING_INPUTS, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING, ComponentState.TEARING_DOWN },
        { ComponentState.IDLING_AFTER_RESET, ComponentState.TEARING_DOWN },
        { ComponentState.CANCELLING, ComponentState.TEARING_DOWN },
        { ComponentState.RESETTING, ComponentState.TEARING_DOWN },
        { ComponentState.TEARING_DOWN, ComponentState.FAILED },
        { ComponentState.FAILED, ComponentState.DISPOSING }
    };

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private final AsyncOrderedExecutionQueue executionQueue = new AsyncOrderedExecutionQueue(
        AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, threadPool);

    private final CountDownLatch componentTerminatedLatch = new CountDownLatch(1);

    private ScheduledFuture<?> heartbeatFuture;

    private AtomicInteger wfControllerCallbackFailureCount = new AtomicInteger(0);

    private ComponentExecutionStorageBridge compDataManagementStorage;

    private int timestampOffsetToWorkfowNode = 0;

    private ComponentExecutionContext compExeCtx;

    private ComponentContextImpl componentContext;

    private ComponentStateMachine stateMachine;

    private BlockingDeque<EndpointDatum> endpointDatumsToProcess = new LinkedBlockingDeque<>();

    private Map<String, EndpointDatum> endpointDatumsForExecution = Collections.synchronizedMap(new HashMap<String, EndpointDatum>());

    private BatchingConsoleRowsForwarder consoleRowsForwarder;

    private AtomicInteger executionCount = new AtomicInteger(1);

    private SortedSet<Integer> executionCountOnResets = new TreeSet<>();

    private ExecutionScheduler executionScheduler;

    private WorkflowExecutionControllerCallbackDelegator wfExeCtrlCallback;

    private boolean isNestedLoopComponent;

    private AtomicBoolean finalHistoryDataItemWritten = new AtomicBoolean(false);

    private AtomicBoolean intermediateHistoryDataWritten = new AtomicBoolean(false);

    private Map<String, Boolean> closedOutputs = Collections.synchronizedMap(new HashMap<String, Boolean>());

    private ComponentExecutionControllerCallback compExeCtrlCallback = new ComponentExecutionControllerCallback() {

        private final Object historyDataLock = new Object();

        private DefaultComponentHistoryDataItem defaultHistoryDataItem = new DefaultComponentHistoryDataItem();

        @Override
        public synchronized void writeOutput(String outputName, TypedDatum datumToSend) {
            EndpointDescription endpointDescription = compExeCtx.getComponentDescription().getOutputDescriptionsManager()
                .getEndpointDescription(outputName);
            Long outputDmId = null;
            try {
                outputDmId = compDataManagementStorage.addOutput(outputName, typedDatumService.getSerializer().serialize(datumToSend));
            } catch (ComponentExecutionException e) {
                throw new RuntimeException(e);
            }
            synchronized (historyDataLock) {
                // if inputs/outputs won't be a part of the history data item, following call can be removed
                defaultHistoryDataItem.addOutput(outputName, datumToSend);
            }
            if (endpointDescription.isConnected()) {
                validateOutputDataType(outputName, endpointDescription.getDataType(), datumToSend);
                writeDatumToOutput(outputName, datumToSend, outputDmId);
                if (datumToSend.getDataType().equals(DataType.NotAValue)) {
                    executionScheduler.addIndefiniteDatumSent(((NotAValueTD) datumToSend).getIdentifier());
                }
            }
            closedOutputs.put(outputName, false);
        }

        private void validateIfNestedLoopComponent(String outputName) {
            if (!isNestedLoopComponent) {
                throw new RuntimeException(getLogMessagesPrefix() + String.format(
                    "Received reset datum at outout '%s' for a non nested loop component. "
                        + "Reset datums are only allowed to send by nested loop components.", outputName));
            }
        }

        private void validateOutputDataType(String outputName, DataType dataType, TypedDatum datumToSent) {
            if (!datumToSent.getDataType().equals(DataType.NotAValue) && !datumToSent.getDataType().equals(DataType.Internal)) {
                if (datumToSent.getDataType() != dataType) {
                    TypedDatumConverter converter = typedDatumService.getConverter();
                    if (converter.isConvertibleTo(datumToSent, dataType)) {
                        try {
                            datumToSent = converter.castOrConvert(datumToSent, dataType);
                        } catch (DataTypeException e) {
                            // should not be reached because of isConvertibleTo check before
                            throw new RuntimeException(getLogMessagesPrefix() + String.format("Failed to convert "
                                + "the value for output '" + outputName + "' from type " + datumToSent.getDataType()
                                + " to required data type " + dataType));
                        }
                    } else {
                        throw new RuntimeException(getLogMessagesPrefix() + String.format("Value for output '" + outputName
                            + "' has invalid data type. Output requires " + dataType + " or a convertable one, but it is of type "
                            + datumToSent.getDataType()));
                    }
                }
            }
        }

        @Override
        public Set<String> getInputsWithDatum() {
            return new HashSet<>(endpointDatumsForExecution.keySet());
        }

        @Override
        public TypedDatum readInput(String inputName) {
            if (endpointDatumsForExecution.containsKey(inputName)) {
                final EndpointDatum endpointDatum = endpointDatumsForExecution.get(inputName);
                return endpointDatum.getValue();
            } else {
                throw new NoSuchElementException(getLogMessagesPrefix() + String.format("No datum at input '%s'", inputName));
            }
        }

        @Override
        public void printConsoleRow(String line, Type consoleRowType) {
            ConsoleRowBuilder consoleRowBuilder = new ConsoleRowBuilder(timestampOffsetToWorkfowNode);
            consoleRowBuilder.setExecutionIdentifiers(compExeCtx.getWorkflowExecutionIdentifier(), compExeCtx.getExecutionIdentifier())
                .setInstanceNames(compExeCtx.getWorkflowInstanceName(), compExeCtx.getInstanceName())
                .setType(consoleRowType);

            if (consoleRowType.equals(Type.STDOUT) || consoleRowType.equals(Type.STDERR) || consoleRowType.equals(Type.COMPONENT_OUTPUT)) {
                consoleRowBuilder.setPayload(line);
            } else {
                // TODO introduce new way to communicate timeline events and set filter active again - seid_do
                // LOG.warn(getLogMessagesPrefix() + String.format("Console row type '%s' not intended to be "
                // + "printed by components. Following line was ignored: %s", consoleRowType.name(), line));
                consoleRowBuilder.setPayload(StringUtils.escapeAndConcat(line, String.valueOf(compDataManagementStorage
                    .getComponentExecutionDataManagementId())));
            }
            consoleRowsForwarder.onConsoleRow(consoleRowBuilder.build());
        }

        @Override
        public void closeAllOutputs() {
            for (EndpointDescription output : compExeCtx.getComponentDescription()
                .getOutputDescriptionsManager().getEndpointDescriptions()) {
                closeOutput(output.getName());
            }
        }

        @Override
        public synchronized void closeOutput(String outputName) {
            if (!closedOutputs.containsKey(outputName) || !closedOutputs.get(outputName)) {
                closedOutputs.put(outputName, true);
                writeDatumToOutput(outputName, new InternalTDImpl(InternalTDImpl.InternalTDType.WorkflowFinish));
            } else {
                LOG.warn(getLogMessagesPrefix() + String.format("Output '%s' already closed. "
                    + "Ignored further closing request.", outputName));
            }
        }

        @Override
        public synchronized boolean isOutputClosed(String outputName) {
            return closedOutputs.containsKey(outputName) && closedOutputs.get(outputName);
        }

        @Override
        public void resetOutput(String outputName) {
            validateIfNestedLoopComponent(outputName);
            Set<String> resetDataIds = writeResetOutputData(outputName);
            executionScheduler.addResetDataIdsSent(resetDataIds);
        }

        @Override
        public int getExecutionCount() {
            return executionCount.get();
        }

        @Override
        public void writeIntermediateHistoryData(ComponentHistoryDataItem componentHistoryDataItem) {
            writeHistoryDataItem(componentHistoryDataItem);
            intermediateHistoryDataWritten.set(true);
        }

        @Override
        public void writeFinalHistoryDataItem(ComponentHistoryDataItem componentHistoryDataItem) {
            synchronized (historyDataLock) {
                addInputsToDefaultHistoryDataItem();
                writeHistoryDataItem(componentHistoryDataItem);
                finalHistoryDataItemWritten.set(true);
                defaultHistoryDataItem = new DefaultComponentHistoryDataItem();
            }
        }

        private void addInputsToDefaultHistoryDataItem() {
            for (Entry<String, EndpointDatum> entry : endpointDatumsForExecution.entrySet()) {
                if (entry.getValue().getDataManagementId() != null) {
                    // if inputs/outputs won't be a part of the history data item anymore, following call can be removed
                    defaultHistoryDataItem.addInput(entry.getKey(), entry.getValue().getValue());
                }
            }
        }

        private void writeHistoryDataItem(ComponentHistoryDataItem componentHistoryDataItem) {
            try {
                synchronized (defaultHistoryDataItem) {
                    ComponentHistoryDataItem historyDateItemToWrite;
                    if (componentHistoryDataItem != null) {
                        historyDateItemToWrite = new MergedComponentHistoryDateItem(componentHistoryDataItem, defaultHistoryDataItem,
                            typedDatumService.getSerializer());
                    } else {
                        defaultHistoryDataItem.setIdentifier(getDefaultHistoryDataItemIdentifier());
                        historyDateItemToWrite = defaultHistoryDataItem;
                    }
                    try {
                        compDataManagementStorage.setOrUpdateHistoryDataItem(historyDateItemToWrite.serialize(
                            typedDatumService.getSerializer()));
                    } catch (ComponentExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private String getLogMessagesPrefix() {
            return String.format("Component %s (%s) of workflow '%s (%s): ",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier());
        }

        @Override
        public Long getComponentExecutionDataManagementId() {
            return compDataManagementStorage.getComponentExecutionDataManagementId();
        }

    };

    private Runnable heartbeatRunnable = new Runnable() {

        @Override
        @TaskDescription("Send heartbeat for component")
        public void run() {
            // LOG.warn("Sending component heartbeat: " + compExeCtx.getExecutionIdentifier());
            wfExeCtrlCallback.onComponentHeartbeatReceived(compExeCtx.getExecutionIdentifier());
        }
    };

    @Deprecated
    public ComponentExecutionControllerImpl() {}

    public ComponentExecutionControllerImpl(ComponentExecutionContext executionContext,
        WorkflowExecutionControllerCallback componentExecutionEventCallback, long currentTimestampOffWorkflowNode) {
        this.compExeCtx = executionContext;
        this.stateMachine = new ComponentStateMachine();
        this.wfExeCtrlCallback = new WorkflowExecutionControllerCallbackDelegator(componentExecutionEventCallback);
        this.consoleRowsForwarder = new BatchingConsoleRowsForwarder(this.wfExeCtrlCallback);
        if (!executionContext.getWorkflowNodeId().equals(executionContext.getComponentDescription().getNode())) {
            this.timestampOffsetToWorkfowNode = (int) (currentTimestampOffWorkflowNode - System.currentTimeMillis());
        }
        this.isNestedLoopComponent = Boolean.valueOf(compExeCtx.getComponentDescription()
            .getConfigurationDescription().getConfigurationValue(ComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));
        this.compDataManagementStorage = new ComponentExecutionStorageBridge(metaDataService, executionContext,
            timestampOffsetToWorkfowNode);
        this.executionScheduler = new ExecutionScheduler(compExeCtx, endpointDatumsToProcess, stateMachine);

        heartbeatFuture = threadPool.scheduleAtFixedRate(heartbeatRunnable, HEARTBEAT_SEND_INTERVAL_MSEC);
    }

    @Override
    public void prepare() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARE_REQUESTED));
    }

    @Override
    public void start() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_REQUESTED));
    }

    @Override
    public void pause() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PAUSE_REQUESTED));
    }

    @Override
    public void resume() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESUME_REQUESTED));
    }

    @Override
    public void restart() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESTART_REQUESTED));
    }

    @Override
    public void cancel() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_REQUESTED));
    }

    @Override
    public void cancelSync(long timeoutMsec) throws InterruptedException {
        cancel();
        componentTerminatedLatch.await(timeoutMsec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_REQUESTED));
    }

    @Override
    public void onEndpointDatumReceived(final EndpointDatum endpointDatum) {
        endpointDatumsToProcess.add(endpointDatum);
    }

    @Override
    public ComponentState getState() {
        return stateMachine.getState();
    }

    @Override
    public boolean isWorkflowControllerReachable() {
        return wfControllerCallbackFailureCount.get() < MAX_CALLBACK_FAILURES;
    }

    /**
     * Available event types for the {@link ComponentStateMachine}.
     * 
     * @author Doreen Seider
     */
    protected enum ComponentStateMachineEventType {

        // requests
        PREPARE_REQUESTED,
        START_REQUESTED,
        PROCESSING_INPUT_DATUMS_REQUESTED,
        CANCEL_REQUESTED,
        DISPOSE_REQUESTED,
        PAUSE_REQUESTED,
        RESUME_REQUESTED,
        RESTART_REQUESTED,
        RESET_REQUESTED,
        IDLE_REQUESTED,

        // successful attempts
        PREPARATION_SUCCESSFUL,
        START_SUCCESSFUL,
        RESET_SUCCESSFUL,
        PROCESSING_INPUTS_SUCCESSFUL,
        CANCEL_ATTEMPT_SUCCESSFUL,
        DISPOSE_ATTEMPT_SUCCESSFUL,
        PAUSE_ATTEMPT_SUCCESSFUL,
        RESUME_ATTEMPT_SUCCESSFUL,
        RESTART_ATTEMPT_SUCCESSFUL,

        // failed attempts
        PREPARATION_FAILED,
        START_FAILED,
        RESET_FAILED,
        PROCESSING_INPUTS_FAILED,
        SCHEDULING_FAILED,
        CANCEL_ATTEMPT_FAILED,
        DISPOSE_ATTEMPT_FAILED,
        PAUSE_ATTEMPT_FAILED,
        RESUME_ATTEMPT_FAILED,
        RESTART_ATTEMPT_FAILED,

        RUNNING,
        FINISHED,
        TEARED_DOWN
    }

    /**
     * Events the {@link ComponentStateMachine} can process.
     * 
     * @author Doreen Seider
     */
    protected static final class ComponentStateMachineEvent {

        private final ComponentStateMachineEventType type;

        private Throwable throwable;

        private ComponentState newComponentState;

        public ComponentStateMachineEvent(ComponentStateMachineEventType type) {
            this.type = type;
        }

        public ComponentStateMachineEvent(ComponentStateMachineEventType type, Throwable t) {
            this(type);
            this.throwable = t;
        }

        public ComponentStateMachineEvent(ComponentStateMachineEventType type, ComponentState newComponentState) {
            this(type);
            this.newComponentState = newComponentState;
        }

        public ComponentStateMachineEvent(ComponentStateMachineEventType type, ComponentState newComponentState, Throwable t) {
            this(type, t);
            this.newComponentState = newComponentState;
        }

        public ComponentStateMachineEventType getType() {
            return type;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public ComponentState getNewComponentState() {
            return newComponentState;
        }

        @Override
        public String toString() {
            return type.name();
        }

    }

    /**
     * Component-specific implementation of {@link AbstractStateMachine}.
     * 
     * @author Doreen Seider
     */
    protected class ComponentStateMachine extends AbstractFixedTransitionsStateMachine<ComponentState, ComponentStateMachineEvent> {

        private Component component;

        private Future<?> currentFuture = null;

        private Throwable exceptionThrown = null;

        private ComponentStateMachineEvent lastEventBeforePaused;

        private boolean pauseWasRequested = false;

        public ComponentStateMachine() {
            super(ComponentState.INIT, VALID_COMPONENT_STATE_TRANSITIONS);
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
            ComponentState state = null;
            switch (event.getType()) {
            case PREPARE_REQUESTED:
                if (checkStateChange(currentState, ComponentState.PREPARING, event)) {
                    state = ComponentState.PREPARING;
                    prepareAsync();
                }
                break;
            case START_REQUESTED:
                if (checkStateChange(currentState, ComponentState.WAITING, event)) {
                    state = ComponentState.WAITING;
                    startAsync();
                }
                break;
            case PROCESSING_INPUT_DATUMS_REQUESTED:
                if (checkStateChange(currentState, ComponentState.WAITING, event)) {
                    state = ComponentState.WAITING;
                    processInputsAsync();
                }
                break;
            case RUNNING:
                if (checkStateChange(currentState, event.getNewComponentState(), event)) {
                    state = event.getNewComponentState();
                }
                break;
            case RESET_REQUESTED:
                if (checkStateChange(currentState, ComponentState.RESETTING, event)) {
                    state = ComponentState.RESETTING;
                    resetAsync();
                }
                break;
            case PAUSE_REQUESTED:
                switch (currentState) {
                case IDLING:
                case IDLING_AFTER_RESET:
                    cancelIdlingTaskAsync(currentFuture);
                    break;
                default:
                    break;
                }
                state = ComponentState.PAUSING;
                pauseWasRequested = true;
                break;
            case RESUME_REQUESTED:
                if (checkStateChange(currentState, ComponentState.RESUMING, event)) {
                    state = ComponentState.RESUMING;
                    stateMachine.postEvent(lastEventBeforePaused);
                }
                break;
            case CANCEL_REQUESTED:
                if (checkStateChange(currentState, ComponentState.CANCELLING, event)) {
                    state = ComponentState.CANCELLING;
                    cancelAsync(currentState);
                }
                break;
            case DISPOSE_REQUESTED:
                if (checkStateChange(currentState, ComponentState.DISPOSING, event)) {
                    state = ComponentState.DISPOSING;
                    disposeAsync();
                }
                break;
            case PREPARATION_SUCCESSFUL:
                if (checkStateChange(currentState, ComponentState.PREPARED, event)) {
                    currentFuture = null;
                    state = ComponentState.PREPARED;
                }
                break;
            case START_SUCCESSFUL:
            case PROCESSING_INPUTS_SUCCESSFUL:
            case IDLE_REQUESTED:
                if (checkStateChange(currentState, ComponentState.IDLING, event)) {
                    state = ComponentState.IDLING;
                    checkExecutionDemandAsync();
                }
                break;
            case RESET_SUCCESSFUL:
                if (checkStateChange(currentState, ComponentState.IDLING_AFTER_RESET, event)) {
                    state = ComponentState.IDLING_AFTER_RESET;
                    checkExecutionDemandAsync();
                }
                break;
            case CANCEL_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                    if (currentState == ComponentState.CANCELLING_AFTER_INPUT_FAILURE) {
                        tearDownAsync(ComponentState.FAILED);
                    } else {
                        tearDownAsync(ComponentState.CANCELED);
                    }
                    state = ComponentState.TEARING_DOWN;
                }
                break;
            case DISPOSE_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, ComponentState.DISPOSED, event)) {
                    state = ComponentState.DISPOSED;
                    heartbeatFuture.cancel(true);
                }
                break;
            case FINISHED:
                if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                    state = handleFinished();
                }
                break;
            case START_FAILED:
            case RESET_FAILED:
            case PROCESSING_INPUTS_FAILED:
            case PREPARATION_FAILED:
            case SCHEDULING_FAILED:
            case PAUSE_ATTEMPT_FAILED:
            case CANCEL_ATTEMPT_FAILED:
                if (checkStateChange(currentState, ComponentState.TEARING_DOWN, event)) {
                    state = handleFailure(currentState, event);
                }
                break;
            case TEARED_DOWN:
                if (checkStateChange(currentState, event.getNewComponentState(), event)) {
                    if (event.getThrowable() != null) {
                        handleExceptionOfComponentStateMachineEvent(event);
                    }
                    state = event.getNewComponentState();
                }
                break;
            default:
                break;
            }
            return state;
        }

        private void handleExceptionOfComponentStateMachineEvent(ComponentStateMachineEvent event) {
            // only log if component and workflow controller doesn't run on the same node to avoid duplicate log messages/stack
            // traces
            if (!compExeCtx.getNodeId().equals(compExeCtx.getWorkflowNodeId())) {
                LOG.error(String.format("Executing component '%s' (%s) failed", compExeCtx.getInstanceName(),
                    compExeCtx.getExecutionIdentifier()), event.getThrowable());
            }
            exceptionThrown = event.getThrowable();
        }

        private ComponentState handleFailure(ComponentState currentState, ComponentStateMachineEvent event) {

            handleExceptionOfComponentStateMachineEvent(event);

            switch (event.getType()) {
            case SCHEDULING_FAILED:
                cancelAsync(currentState);
                return ComponentState.CANCELLING_AFTER_INPUT_FAILURE;
            default:
                currentFuture = null;
                tearDownAsync(ComponentState.FAILED);
                return ComponentState.TEARING_DOWN;
            }
        }

        private ComponentState handleFinished() {
            if (executionCount.get() == 0) {
                tearDownAsync(ComponentState.FINISHED_WITHOUT_EXECUTION);
            } else {
                tearDownAsync(ComponentState.FINISHED);
            }
            return ComponentState.TEARING_DOWN;
        }

        private void logInvalidStateChangeRequest(ComponentState currentState, ComponentState requestedState,
            ComponentStateMachineEvent event) {
            LOG.debug(String.format("Ignored component state change request for component '%s' (%s) of workflow '%s' (%s) "
                + "as it will cause an invalid state transition: %s -> %s; event was: %s",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier(), currentState, requestedState,
                event.getType().name()));
        }

        @Override
        protected void onStateChanged(ComponentState oldState, ComponentState newState) {
            LOG.debug(String.format("Component '%s' (%s) of workflow '%s' (%s) is now %s (previous state: %s)",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(), compExeCtx.getWorkflowInstanceName(),
                compExeCtx.getWorkflowExecutionIdentifier(), newState, oldState));
            try {
                if (newState.equals(ComponentState.FAILED)) {
                    try {
                        wfExeCtrlCallback.onComponentStateChanged(compExeCtx.getExecutionIdentifier(), oldState, newState,
                            executionCount.get(), getExecutionCountsOnResetAsString(), exceptionThrown);
                    } catch (UndeclaredThrowableException e) {
                        if (e.getCause() instanceof CommunicationException && e.getCause().getCause() instanceof SerializationException) {
                            wfExeCtrlCallback.onComponentStateChanged(compExeCtx.getExecutionIdentifier(), oldState, newState,
                                executionCount.get(), getExecutionCountsOnResetAsString(),
                                new ComponentExecutionException(exceptionThrown.toString()));
                        }
                    }
                } else {
                    wfExeCtrlCallback.onComponentStateChanged(compExeCtx.getExecutionIdentifier(), oldState, newState,
                        executionCount.get(),
                        getExecutionCountsOnResetAsString());
                }
                if (ComponentConstants.FINAL_COMPONENT_STATES.contains(newState)) {
                    sendStateAsConsoleRow(WorkflowLifecyleEventType.COMPONENT_TERMINATED);
                } else if (newState == ComponentState.STARTING) {
                    sendStateAsConsoleRow(WorkflowLifecyleEventType.COMPONENT_STARTING);
                }
            } catch (UndeclaredThrowableException e) {
                if (e.getCause() instanceof CommunicationException) {
                    LOG.error("Failed to report state change to workflow controller: " + e.getCause().toString());
                } else {
                    throw e;
                }
            } finally {
                if (ComponentConstants.FINAL_COMPONENT_STATES.contains(newState)) {
                    componentTerminatedLatch.countDown();
                }
            }
        }

        private String getExecutionCountsOnResetAsString() {
            List<String> counts = new ArrayList<>();
            synchronized (executionCountOnResets) {
                for (Integer countOnReset : executionCountOnResets) {
                    counts.add(String.valueOf(countOnReset));
                }
                if (!executionCountOnResets.contains(executionCount.get())) {
                    counts.add(String.valueOf(executionCount.get()));
                }
            }
            return StringUtils.escapeAndConcat(counts);
        }

        private void sendStateAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType type) {
            ConsoleRowBuilder consoleRowBuilder = new ConsoleRowBuilder();
            consoleRowBuilder.setExecutionIdentifiers(compExeCtx.getWorkflowExecutionIdentifier(), compExeCtx.getExecutionIdentifier())
                .setInstanceNames(compExeCtx.getWorkflowInstanceName(), compExeCtx.getInstanceName())
                .setType(ConsoleRow.Type.LIFE_CYCLE_EVENT)
                .setPayload(type.name());
            ConsoleRow row = consoleRowBuilder.build();
            consoleRowsForwarder.onConsoleRow(row);
        }

        @Override
        protected void onStateChangeException(ComponentStateMachineEvent event, StateChangeException e) {
            LOG.error(String.format("Invalid state change for component '%s' (%s) of workflow '%s' (%s) attempt, caused by event '%s'",
                compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(), compExeCtx.getWorkflowInstanceName(),
                compExeCtx.getWorkflowExecutionIdentifier(), event), e);
        }

        private void prepareAsync() {
            currentFuture = threadPool.submit(new AsyncPrepareTask());
        }

        private void startAsync() {
            currentFuture = threadPool.submit(new AsyncStartTask());
        }

        private void checkForIntermediateButNoFinalHistoryDataItemWritten() throws ComponentExecutionException {
            if (!finalHistoryDataItemWritten.get()
                && Boolean.valueOf(componentContext.getConfigurationValue(ComponentConstants.CONFIG_KEY_STORE_DATA_ITEM))) {
                if (intermediateHistoryDataWritten.get()) {
                    LOG.warn(String.format("No final history data item was written for component '%s' (%s) of workflow '%s' (%s)"
                        + " even if intermediate ones were.",
                        compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                        compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier()));
                }
            }
            intermediateHistoryDataWritten.set(false);
            finalHistoryDataItemWritten.set(false);
        }

        private void checkExecutionDemandAsync() {
            currentFuture = threadPool.submit(new AsyncIdlingAndCheckForExecutableOrFinishedTask());
        }

        private void processInputsAsync() {
            currentFuture = threadPool.submit(new AsyncProcessInputsTask());
        }

        private void resetAsync() {
            currentFuture = threadPool.submit(new AsyncResetTask());
        }

        private void cancelAsync(ComponentState currentState) {
            threadPool.submit(new AsyncCancelTask(currentFuture, currentState));
        }

        private void cancelIdlingTaskAsync(Future<?> future) {
            threadPool.submit(new AsyncPauseIdlingTask(future));
        }

        private void tearDownAsync(ComponentState intendedFinalState) {
            currentFuture = threadPool.submit(new AsyncTearDownTask(intendedFinalState));
        }

        private void disposeAsync() {
            currentFuture = threadPool.submit(new AsyncDisposeTask());
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
                try {
                    executionScheduler.initialize(compExeCtx);
                    component = createNewComponentInstance();
                    ((ComponentExecutionContextImpl) compExeCtx).setWorkingDirectory(createWorkingDirectory());
                    componentContext = new ComponentContextImpl(compExeCtx, compExeCtrlCallback);
                    component.setComponentContext(componentContext);
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARATION_SUCCESSFUL));
                } catch (ComponentExecutionException | RuntimeException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PREPARATION_FAILED, e));
                }
            }

            private Component createNewComponentInstance() throws ComponentExecutionException {
                final String message = String.format("Failed to instantiate component '%s' (%s) of workflow '%s' (%s)",
                    compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                    compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier());
                try {
                    return (Component) Class.forName(compExeCtx.getComponentDescription().getClassName()).getConstructor().newInstance();
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

            @Override
            @TaskDescription("Start component")
            public void run() {
                try {
                    executionScheduler.start();
                    boolean treatStartAsRun = component.treatStartAsComponentRun();
                    new ComponentExecuter(ComponentState.STARTING,
                        ComponentStateMachineEventType.START_FAILED, treatStartAsRun) {

                        @Override
                        public void execute() throws ComponentException {
                            component.start();
                        }
                    }.executeByConsideringLimitations();
                    if (treatStartAsRun) {
                        checkForIntermediateButNoFinalHistoryDataItemWritten();
                    } else {
                        executionCount.decrementAndGet();
                    }
                    providedInitValuesToInputs();
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_SUCCESSFUL));
                } catch (ComponentExecutionException | ComponentException | RuntimeException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.START_FAILED, e));
                }
            }
        }

        private void providedInitValuesToInputs() {
            for (EndpointDescription endpointDescription : compExeCtx.getComponentDescription().getInputDescriptionsManager()
                .getEndpointDescriptions()) {
                if (endpointDescription.getMetaData().containsKey(ComponentConstants.INPUT_METADATA_KEY_INIT_VALUE)) {
                    TypedDatum value = typedDatumService.getSerializer().deserialize(endpointDescription.getMetaData()
                        .get(ComponentConstants.INPUT_METADATA_KEY_INIT_VALUE));
                    EndpointDatumImpl endpointDatum = new EndpointDatumImpl();
                    endpointDatum.setEndpointDatumRecipient(EndpointDatumRecipientFactory.createEndpointDatumRecipient(
                        endpointDescription.getName(), compExeCtx.getExecutionIdentifier(), compExeCtx.getNodeId()));
                    endpointDatum.setWorkflowExecutionIdentifier(compExeCtx.getWorkflowExecutionIdentifier());
                    endpointDatum.setWorkfowNodeId(compExeCtx.getWorkflowNodeId());
                    endpointDatum.setValue(value);
                    onEndpointDatumReceived(endpointDatum);
                }
            }
        }

        private File createWorkingDirectory() throws ComponentExecutionException {
            try {
                return TempFileServiceAccess.getInstance().createManagedTempDir("cmp-" + compExeCtx.getExecutionIdentifier());
            } catch (IOException e) {
                throw new ComponentExecutionException(String.format("Failed to create working directory for component '%s' "
                    + "(%s) of workflow '%s' (%s)", compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                    compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier()), e);
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
                    component.reset();
                    synchronized (executionCountOnResets) {
                        executionCountOnResets.add(executionCount.get());
                    }
                    if (compDataManagementStorage.getComponentExecutionDataManagementId() != null) {
                        try {
                            compDataManagementStorage.setComponentExecutionFinished();
                        } catch (ComponentExecutionException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_FAILED, e));
                            return;
                        }
                    }
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_SUCCESSFUL));
                } catch (ComponentException | RuntimeException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_FAILED, e));
                }
            }
        }

        /**
         * Component is idling. It waits for inputs and checks for execution readiness, finished, or failed.
         * 
         * @author Doreen Seider
         */
        private final class AsyncIdlingAndCheckForExecutableOrFinishedTask implements Runnable {

            @Override
            @TaskDescription("Component idling - checking for executable, finished, or failed")
            public void run() {
                if (!compExeCtx.isConnectedToEndpointDatumSenders()) {
                    compExeCtrlCallback.closeAllOutputs();
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.FINISHED));
                } else {
                    boolean isIdling = true;
                    while (isIdling) {
                        State schedulingState;
                        try {
                            schedulingState = executionScheduler.getSchedulingState();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (ComponentExecutionException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.SCHEDULING_FAILED, e));
                            return;
                        }
                        switch (schedulingState) {
                        case FINISHED:
                            isIdling = false;
                            forwardFinishToNonClosedOutputs();
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.FINISHED));
                            break;
                        case PROCESS_INPUT_DATA:
                            requestProcessingInputDatums();
                            isIdling = false;
                            break;
                        case PROCESS_INPUT_DATA_WITH_INDEFINITE_DATA:
                            if (compExeCtx.getComponentDescription().getComponentInstallation().getComponentRevision()
                                .getComponentInterface().getCanHandleIndefiniteInputDataTypes()) {
                                requestProcessingInputDatums();
                                isIdling = false;
                            } else {
                                forwardIndefiniteData();
                            }
                            break;
                        case RESET:
                            InternalTDImpl resetDatum = executionScheduler.getResetDatum();
                            Queue<WorkflowGraphHop> hopsToTraverse = resetDatum.getHopsToTraverse();
                            WorkflowGraphHop currentHop = hopsToTraverse.poll();
                            writeResetOutputDatum(currentHop.getHopOuputName(), resetDatum,
                                currentHop.getTargetExecutionIdentifier(), currentHop.getTargetInputName());
                            if (!getState().equals(ComponentState.IDLING_AFTER_RESET)) {
                                isIdling = false;
                                stateMachine.postEvent(new ComponentStateMachineEvent(
                                    ComponentStateMachineEventType.RESET_REQUESTED));
                            }
                            break;
                        case LOOP_RESET:
                            isIdling = false;
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RESET_REQUESTED));
                            break;
                        case IDLING:
                        default:
                            break;
                        }
                    }
                }
            }

            private void forwardFinishToNonClosedOutputs() {
                for (EndpointDescription output : compExeCtx.getComponentDescription()
                    .getOutputDescriptionsManager().getEndpointDescriptions()) {
                    if (!closedOutputs.containsKey(output.getName()) || !closedOutputs.get(output.getName())) {
                        writeDatumToOutput(output.getName(), new InternalTDImpl(InternalTDImpl
                            .InternalTDType.WorkflowFinish));
                    }
                }
            }

            private void forwardIndefiniteData() {
                EndpointDatum indefiniteEndpointDatum = null;
                // it must contain at least one of DataType Indefinite if the state was
                // PROCESS_INPUT_DATUMS_WITH_INDEFINITE_DATUM
                for (EndpointDatum endpointDatum : executionScheduler.getEndpointDatums().values()) {
                    if (endpointDatum.getValue().getDataType().equals(DataType.NotAValue)) {
                        indefiniteEndpointDatum = endpointDatum;
                        break;
                    }
                }
                for (EndpointDescription output : compExeCtx.getComponentDescription().getOutputDescriptionsManager()
                    .getEndpointDescriptions()) {
                    writeDatumToOutput(output.getName(), indefiniteEndpointDatum.getValue());
                    LOG.info(String.format("Component '%s' of workflow '%s' did not run because of indefinite "
                        + "value at input '%s'", compExeCtx.getInstanceName(), compExeCtx.getWorkflowInstanceName(),
                        indefiniteEndpointDatum.getInputName()));
                }
            }

            private void requestProcessingInputDatums() {
                endpointDatumsForExecution.clear();
                endpointDatumsForExecution.putAll(executionScheduler.getEndpointDatums());
                stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUT_DATUMS_REQUESTED));
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
                    executionCount.incrementAndGet();
                    new ComponentExecuter(ComponentState.PROCESSING_INPUTS, ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED, true) {

                        @Override
                        public void execute() throws ComponentException {
                            component.processInputs();
                        }
                    }.executeByConsideringLimitations();
                    checkForIntermediateButNoFinalHistoryDataItemWritten();
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_SUCCESSFUL));
                } catch (ComponentException | ComponentExecutionException | RuntimeException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PROCESSING_INPUTS_FAILED, e));
                }
            }
        }

        /**
         * Pauses the idling task. Pause means cancelling it and requesting idling anew. It will be performed if component gets resumed.
         * 
         * @author Doreen Seider
         */
        private final class AsyncPauseIdlingTask implements Runnable {

            private final Future<?> idlingFuture;

            public AsyncPauseIdlingTask(Future<?> future) {
                this.idlingFuture = future;
            }

            @Override
            @TaskDescription("Cancel idling task")
            public void run() {
                idlingFuture.cancel(true);
                try {
                    idlingFuture.get();
                } catch (InterruptedException e) {
                    LOG.debug(String.format("Ignored interuption request for component '%s' (%s) of workflow '%s' (%s)",
                        compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                        compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier()), e);
                } catch (ExecutionException e) {
                    LOG.error(String.format("Failed to execute task 'waiting for inputs' for component '%s' (%s) of workflow '%s' (%s)",
                        compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                        compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier()), e);
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.PAUSE_ATTEMPT_FAILED));
                } catch (CancellationException e) {
                    // intended
                    e = null;
                }
                stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.IDLE_REQUESTED));
            }
        }

        /**
         * Cancels the component.
         * 
         * @author Doreen Seider
         */
        private final class AsyncCancelTask implements Runnable {

            private final Future<?> future;

            private final ComponentState currentState;

            public AsyncCancelTask(Future<?> future, ComponentState currentState) {
                this.future = future;
                this.currentState = currentState;
            }

            @Override
            @TaskDescription("Tear down component")
            public void run() {
                if (future != null) {
                    switch (currentState) {
                    case STARTING:
                        try {
                            component.onStartInterrupted(new ThreadHandler(future));
                        } catch (RuntimeException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED));
                            return;
                        }
                        break;
                    case PROCESSING_INPUTS:
                        try {
                            component.onProcessInputsInterrupted(new ThreadHandler(future));
                        } catch (RuntimeException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED));
                            return;
                        }
                        break;
                    case IDLING:
                    case IDLING_AFTER_RESET:
                        future.cancel(true);
                        break;
                    default:
                        break;
                    }
                    try {
                        future.get();
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED));
                    } catch (ExecutionException e) {
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_FAILED));
                    } catch (CancellationException e) {
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
                    }
                } else {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
                }
            }
        }

        /**
         * Tears down the component.
         * 
         * @author Doreen Seider
         */
        private final class AsyncTearDownTask implements Runnable {

            private final ComponentState intendedFinalState;

            public AsyncTearDownTask(ComponentState intendedFinalState) {
                this.intendedFinalState = intendedFinalState;
            }

            @Override
            @TaskDescription("Forward finish state")
            public void run() {
                try {
                    synchronized (stateMachine) {
                        executionScheduler.stop();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to stop execution scheduler", e);
                }

                Component.FinalComponentState finalStateForComp = null;
                FinalComponentState finalStateForDm = null;
                switch (intendedFinalState) {
                case FINISHED_WITHOUT_EXECUTION:
                    finalStateForComp = Component.FinalComponentState.FINISHED;
                    finalStateForDm = FinalComponentState.FINISHED_WITHOUT_EXECUTION;
                    break;
                case FINISHED:
                    finalStateForComp = Component.FinalComponentState.FINISHED;
                    finalStateForDm = FinalComponentState.FINISHED;
                    break;
                case FAILED:
                    finalStateForComp = Component.FinalComponentState.FAILED;
                    finalStateForDm = FinalComponentState.FAILED;
                    break;
                case CANCELED:
                    finalStateForComp = Component.FinalComponentState.CANCELLED;
                    finalStateForDm = FinalComponentState.CANCELLED;
                    break;
                default:
                    break;
                }
                if (component != null) {
                    try {
                        component.tearDown(finalStateForComp);
                    } catch (RuntimeException e) {
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                            ComponentState.FAILED, e));
                        return;
                    }
                    if (finalStateForComp == Component.FinalComponentState.FAILED) {
                        try {
                            checkForIntermediateButNoFinalHistoryDataItemWritten();
                        } catch (ComponentExecutionException e) {
                            stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                                ComponentState.FAILED, e));
                            return;
                        }
                    }
                }
                try {
                    compDataManagementStorage.setFinalComponentState(finalStateForDm);
                } catch (ComponentExecutionException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN,
                        ComponentState.FAILED, e));
                    return;
                }

                stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.TEARED_DOWN, intendedFinalState));
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
                if (component != null) {
                    try {
                        component.dispose();
                    } catch (RuntimeException e) {
                        stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_ATTEMPT_FAILED));
                        return;
                    }
                    disposeWorkingDirectory();
                }
                stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL));
            }
        }

        private void disposeWorkingDirectory() {
            if (compExeCtx.getWorkingDirectory() != null) {
                try {
                    TempFileServiceAccess.getInstance().disposeManagedTempDirOrFile(compExeCtx.getWorkingDirectory());
                } catch (IOException e) {
                    LOG.error(String.format("Failed to dispose working directory of component '%s' "
                        + "(%s) of workflow '%s' (%s)", compExeCtx.getInstanceName(), compExeCtx.getExecutionIdentifier(),
                        compExeCtx.getWorkflowInstanceName(), compExeCtx.getWorkflowExecutionIdentifier()), e);
                }
            }
        }
    }

    private String getDefaultHistoryDataItemIdentifier() {
        return compExeCtx.getComponentDescription().getIdentifier().replace(ComponentConstants.ID_SEPARATOR
            + compExeCtx.getComponentDescription().getVersion(), "");
    }

    protected void writeDatumToOutput(String outputName, TypedDatum datumToSend) {
        writeDatumToOutput(outputName, datumToSend, null);
    }

    protected void writeDatumToOutput(String outputName, TypedDatum datumToSend, Long outputDmId) {
        if (compExeCtx.getComponentDescription().getOutputDescriptionsManager()
            .getEndpointDescription(outputName).isConnected()) {
            for (EndpointDatumRecipient recipient : compExeCtx.getEndpointDatumRecipients().get(outputName)) {
                EndpointDatumImpl endpointDatum = new EndpointDatumImpl();
                endpointDatum.setEndpointDatumRecipient(recipient);
                endpointDatum.setWorkflowExecutionIdentifier(compExeCtx.getWorkflowExecutionIdentifier());
                endpointDatum.setWorkfowNodeId(compExeCtx.getWorkflowNodeId());
                endpointDatum.setDataManagementId(outputDmId);
                endpointDatum.setValue(datumToSend);
                endpointDatumSender.sendEndpointDatumOrderedAsync(endpointDatum);
            }
        }
    }

    private Set<String> writeResetOutputData(String outputName) {
        Set<String> resetDataIds = new HashSet<>();
        for (Queue<WorkflowGraphHop> hops : compExeCtx.getWorkflowGraph()
            .getHopsToTraverseWhenResetting(compExeCtx.getExecutionIdentifier()).get(outputName)) {
            WorkflowGraphHop firstHop = hops.poll();
            InternalTDImpl resetDatum = new InternalTDImpl(InternalTDImpl.InternalTDType.NestedLoopReset, hops);
            resetDataIds.add(resetDatum.getIdentifier());
            writeResetOutputDatum(outputName, resetDatum, firstHop.getTargetExecutionIdentifier(), firstHop.getTargetInputName());
        }
        return resetDataIds;
    }

    private void writeResetOutputDatum(String outputName, InternalTDImpl datumToSent, String inputCompExeId, String inputName) {
        EndpointDescription endpointDescription = compExeCtx.getComponentDescription().getOutputDescriptionsManager()
            .getEndpointDescription(outputName);
        if (endpointDescription.isConnected()) {
            for (EndpointDatumRecipient recipient : compExeCtx.getEndpointDatumRecipients().get(outputName)) {
                if (recipient.getInputName().equals(inputName)
                    && recipient.getInputComponentExecutionIdentifier().equals(inputCompExeId)) {
                    EndpointDatumImpl endpointDatum = new EndpointDatumImpl();
                    endpointDatum.setEndpointDatumRecipient(recipient);
                    endpointDatum.setWorkflowExecutionIdentifier(compExeCtx.getWorkflowExecutionIdentifier());
                    endpointDatum.setWorkfowNodeId(compExeCtx.getWorkflowNodeId());
                    endpointDatum.setValue(datumToSent);
                    endpointDatumSender.sendEndpointDatumOrderedAsync(endpointDatum);
                }
            }
        }
    }

    protected void bindComponentExecutionPermitsService(ComponentExecutionPermitsService newService) {
        ComponentExecutionControllerImpl.componentExecutionPermitService = newService;
    }

    protected void bindEndpointDatumSender(EndpointDatumSender newService) {
        ComponentExecutionControllerImpl.endpointDatumSender = newService;
    }

    protected void bindTypedDatumService(TypedDatumService newService) {
        ComponentExecutionControllerImpl.typedDatumService = newService;
    }

    protected void bindDistributedMetaDataService(DistributedMetaDataService newService) {
        ComponentExecutionControllerImpl.metaDataService = newService;
    }

    /**
     * Wrapper class which calls {@link Component#start(de.rcenvironment.core.component.execution.api.ComponentContext)} and
     * {@link Component#processInputs()} by considering optional limitations concering the amount of parallel executions.
     * 
     * @author Doreen Seider
     */
    private abstract class ComponentExecuter {

        private final ComponentState newComponentState;

        private final ComponentStateMachineEventType failureComponentStateMachineEventType;

        private final boolean treatAsRun;

        public ComponentExecuter(ComponentState newComponentState,
            ComponentStateMachineEventType failureComponentStateMachineEventType, boolean treatAsRun) {
            this.newComponentState = newComponentState;
            this.failureComponentStateMachineEventType = failureComponentStateMachineEventType;
            this.treatAsRun = treatAsRun;
        }

        public void executeByConsideringLimitations() throws ComponentException, ComponentExecutionException {

            if (treatAsRun) {
                try {
                    componentExecutionPermitService.acquire(compExeCtx.getComponentDescription().getIdentifier(),
                        compExeCtx.getExecutionIdentifier()).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stateMachine.postEvent(new ComponentStateMachineEvent(failureComponentStateMachineEventType, e));
                } catch (ExecutionException e) {
                    stateMachine.postEvent(new ComponentStateMachineEvent(failureComponentStateMachineEventType, e));
                }
            }
            try {
                stateMachine.postEvent(new ComponentStateMachineEvent(ComponentStateMachineEventType.RUNNING, newComponentState));
                if (treatAsRun) {
                    compDataManagementStorage.addComponentExecution(compExeCtx, executionCount.get());
                    storeInputs();
                }
                execute();
                if (treatAsRun && !executionScheduler.isLoopResetRequested()) {
                    compDataManagementStorage.setComponentExecutionFinished();
                }
            } finally {
                if (treatAsRun) {
                    componentExecutionPermitService.release(compExeCtx.getComponentDescription().getIdentifier());
                }
            }
        }

        private void storeInputs() throws ComponentExecutionException {

            for (final Entry<String, EndpointDatum> entry : endpointDatumsForExecution.entrySet()) {
                if (entry.getValue().getDataManagementId() != null) {
                    compDataManagementStorage.addInput(entry.getKey(), entry.getValue().getDataManagementId());
                    executionQueue.enqueue(new Runnable() {

                        @Override
                        public void run() {
                            wfExeCtrlCallback.onInputProcessed(EndpointDatumSerializer.serializeEndpointDatum(entry.getValue()));
                        }
                    });
                }
            }
        }

        public abstract void execute() throws ComponentException;
    }

    /**
     * {@link WorkflowExecutionControllerCallback} which delegates the method calls if the workflow execution controller is reachable.
     * Prevent constantly recurring failures and stack traces.
     * 
     * TODO should be improve to reduce boiler-plate code
     * 
     * @author Doreen Seider
     */
    private class WorkflowExecutionControllerCallbackDelegator implements WorkflowExecutionControllerCallback {

        private final WorkflowExecutionControllerCallback callback;

        public WorkflowExecutionControllerCallbackDelegator(WorkflowExecutionControllerCallback callback) {
            this.callback = callback;
        }

        @Override
        public void processConsoleRows(ConsoleRow[] consoleRows) {
            if (isWorkflowControllerReachable()) {
                try {
                    callback.processConsoleRows(consoleRows);
                    wfControllerCallbackFailureCount.set(0);
                } catch (UndeclaredThrowableException e) {
                    handleWorkflowCallbackFailure(e.getCause());
                } catch (RuntimeException e) {
                    handleWorkflowCallbackFailure(e);
                }
            }
        }

        @Override
        public void onComponentStateChanged(String compExeId, ComponentState oldState, ComponentState newState, Integer count,
            String countOnResets) {
            if (isWorkflowControllerReachable()) {
                try {
                    callback.onComponentStateChanged(compExeId, oldState, newState, count, countOnResets);
                    wfControllerCallbackFailureCount.set(0);
                } catch (UndeclaredThrowableException e) {
                    handleWorkflowCallbackFailure(e.getCause());
                } catch (RuntimeException e) {
                    handleWorkflowCallbackFailure(e);
                }
            }
        }

        @Override
        public void onComponentStateChanged(String compExeId, ComponentState oldState, ComponentState newState, Integer count,
            String countOnResets, Throwable th) {
            if (isWorkflowControllerReachable()) {
                try {
                    callback.onComponentStateChanged(compExeId, oldState, newState, count, countOnResets, th);
                    wfControllerCallbackFailureCount.set(0);
                } catch (UndeclaredThrowableException e) {
                    handleWorkflowCallbackFailure(e.getCause());
                } catch (RuntimeException e) {
                    handleWorkflowCallbackFailure(e);
                }
            }
        }

        @Override
        public void onInputProcessed(String serializedEndpointDatum) {
            if (isWorkflowControllerReachable()) {
                try {
                    callback.onInputProcessed(serializedEndpointDatum);
                    wfControllerCallbackFailureCount.set(0);
                } catch (UndeclaredThrowableException e) {
                    handleWorkflowCallbackFailure(e.getCause());
                } catch (RuntimeException e) {
                    handleWorkflowCallbackFailure(e);
                }
            }
        }

        @Override
        public void onComponentHeartbeatReceived(String executionIdentifier) {
            if (isWorkflowControllerReachable()) {
                try {
                    callback.onComponentHeartbeatReceived(executionIdentifier);
                    wfControllerCallbackFailureCount.set(0);
                } catch (UndeclaredThrowableException e) {
                    handleHeartbeatSentFailure(e.getCause());
                } catch (RuntimeException e) {
                    handleHeartbeatSentFailure(e);
                }
            }
        }

        private void handleWorkflowCallbackFailure(Throwable e) {
            int failureCount = wfControllerCallbackFailureCount.incrementAndGet();
            String message = String.format("Callback from local component '%s' to workflow controller ('%s') failed",
                compExeCtx.getExecutionIdentifier(), compExeCtx.getWorkflowExecutionIdentifier());
            registerCallbackFailureEvent(message, failureCount, e);
        }

        private void handleHeartbeatSentFailure(Throwable e) {
            int failureCount = wfControllerCallbackFailureCount.addAndGet(2); // TODO review: keep increased heartbeat weight?
            String message = String.format("Heartbeat callback from local component '%s' to workflow controller ('%s') failed",
                compExeCtx.getExecutionIdentifier(), compExeCtx.getWorkflowExecutionIdentifier());
            registerCallbackFailureEvent(message, failureCount, e);
        }

        private void registerCallbackFailureEvent(String message, int failureCount, Throwable cause) {
            if (failureCount >= MAX_CALLBACK_FAILURES) {
                LOG.error(message + "; maximum number of callback failures (" + MAX_CALLBACK_FAILURES
                    + ") exceeded, considering the controller unreachable", cause);
            } else {
                LOG.warn(message + "; failure count is " + failureCount + " (threshold: " + MAX_CALLBACK_FAILURES
                    + ")", cause);
            }

        }
    }

}
