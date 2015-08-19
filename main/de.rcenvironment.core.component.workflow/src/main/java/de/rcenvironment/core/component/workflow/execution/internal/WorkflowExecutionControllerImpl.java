/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContextBuilder;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowBuilder;
import de.rcenvironment.core.component.execution.api.WorkflowExecutionControllerCallback;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.api.WorkflowGraphEdge;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.AbstractStateMachine;
import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Implementation of {@link WorkflowExecutionController}.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionControllerImpl implements WorkflowExecutionController, WorkflowExecutionControllerCallback {

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionControllerImpl.class);

    /**
     * After 10 minutes without heartbeat from component, the workflow will fail.
     */
    private static final int MAX_HEARTBEAT_INTERVAL_MSEC = 2 * 60 * 1000;
    
    private static MetaDataService metaDataService;
    
    private static DistributedNotificationService notificationService;

    private static ComponentExecutionService componentExecutionService;

    private static final WorkflowState[][] VALID_WORKFLOW_STATE_TRANSITIONS = new WorkflowState[][] {

        // standard lifecycle
        { WorkflowState.INIT, WorkflowState.PREPARING },
        { WorkflowState.PREPARING, WorkflowState.STARTING },
        { WorkflowState.PREPARING, WorkflowState.FINISHED },
        { WorkflowState.STARTING, WorkflowState.RUNNING },
        { WorkflowState.RUNNING, WorkflowState.FINISHED },
        { WorkflowState.FINISHED, WorkflowState.DISPOSING },
        { WorkflowState.DISPOSING, WorkflowState.DISPOSED },
        // pause and resume
        { WorkflowState.RUNNING, WorkflowState.PAUSING },
        { WorkflowState.PAUSING, WorkflowState.PAUSED },
        { WorkflowState.PAUSED, WorkflowState.RESUMING },
        { WorkflowState.RESUMING, WorkflowState.RUNNING },
        // cancel
        { WorkflowState.INIT, WorkflowState.CANCELING },
        { WorkflowState.PREPARING, WorkflowState.CANCELING },
        { WorkflowState.PREPARED, WorkflowState.CANCELING },
        { WorkflowState.RUNNING, WorkflowState.CANCELING },
        { WorkflowState.PAUSING, WorkflowState.CANCELING },
        { WorkflowState.PAUSED, WorkflowState.CANCELING },
        { WorkflowState.RESUMING, WorkflowState.CANCELING },
        { WorkflowState.CANCELING, WorkflowState.CANCELLED },
        { WorkflowState.CANCELLED, WorkflowState.DISPOSING },
        // failure
        { WorkflowState.PREPARING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.RUNNING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.PAUSING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.RESUMING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.CANCELING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.CANCELING, WorkflowState.FAILED },
        { WorkflowState.CANCELING_AFTER_FAILED, WorkflowState.FAILED },
        { WorkflowState.FAILED, WorkflowState.DISPOSING }
    };

    private final ThreadPool threadPool = SharedThreadPool.getInstance();
    
    private ScheduledFuture<?> heartbeatFuture;
    
    private Map<String, Long> componentHeartbeatTimestamps = Collections.synchronizedMap(new HashMap<String, Long>());
    
    private WorkflowExecutionStorageBridge wfDataManagementStorage;
    
    private WorkflowStateMachine stateMachine = new WorkflowStateMachine();
    
    private WorkflowExecutionContext wfExeCtx;

    private Map<String, String> executionAuthTokens;
    
    private final Map<String, NodeIdentifier> componentExecutionIds = Collections
        .synchronizedMap(new HashMap<String, NodeIdentifier>());
    
    private final Map<String, Boolean> disposeComponentImmediately = Collections
        .synchronizedMap(new HashMap<String, Boolean>());

    private final CountDownLatch workflowTerminatedLatch = new CountDownLatch(2);

    private CountDownLatch pausedComonentStateLatch = new CountDownLatch(1);

    private CountDownLatch resumedComonentStateLatch = new CountDownLatch(1);

    private final CountDownLatch disposedComonentStateLatch = new CountDownLatch(1);

    private final PreparedComponentStateListener preparedComponentStateListener = new PreparedComponentStateListener();

    private final FinishedComponentStateListener finishedComponentStateListener = new FinishedComponentStateListener();

    private final FinalComponentStateListener finalComponentStateListener = new FinalComponentStateListener();

    private PausedComponentStateListener pausedComponentStateListener = new PausedComponentStateListener();

    private final DisposedComponentStateListener disposedComponentStateListener = new DisposedComponentStateListener();

    private ResumedComponentStateListener resumedStateListener;

    private final LastConsoleRowListener lastConsoleRowListener = new LastConsoleRowListener();

    private Runnable componentHeartbeatRunnable = new Runnable() {
        @Override
        @TaskDescription("Check heartbeats of components")
        public void run() {
//            LOG.debug("Checking component heartbeats");
            long currentTimestamp = System.currentTimeMillis();
            boolean heartbeatLost = false;
            synchronized (componentHeartbeatTimestamps) {
                for (String compExeId : componentHeartbeatTimestamps.keySet()) {
                    if (currentTimestamp - componentHeartbeatTimestamps.get(compExeId) > MAX_HEARTBEAT_INTERVAL_MSEC
                        && !finalComponentStateListener.hasComponent(compExeId)) {
                        heartbeatLost = true;
                    }
                }
            }
            if (heartbeatLost) {
                stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.COMPONENT_HEARTBEAT_LOST,
                    new WorkflowExecutionException("At least one non-finished component not reachable")));                
            }
        }
    };
    
    /**
     * Available event types for the {@link WorkflowStateMachine}.
     * 
     * @author Doreen Seider
     */
    private enum WorkflowStateMachineEventType {
        // requests
        PREPARE_REQUESTED,
        START_REQUESTED,
        PAUSE_REQUESTED,
        RESUME_REQUESTED,
        RESTART_REQUESTED,
        CANCEL_REQUESTED,
        CANCEL_AFTER_FAILED_REQUESTED,
        DISPOSE_REQUESTED,
        // successful attempts
        PREPARE_ATTEMPT_SUCCESSFUL,
        START_ATTEMPT_SUCCESSFUL,
        PAUSE_ATTEMPT_SUCCESSFUL,
        RESUME_ATTEMPT_SUCCESSFUL,
        CANCEL_ATTEMPT_SUCCESSFUL,
        DISPOSE_ATTEMPT_SUCCESSFUL,
        // failed attempts
        PREPARE_ATTEMPT_FAILED,
        START_ATTEMPT_FAILED,
        PAUSE_ATTEMPT_FAILED,
        RESUME_ATTEMPT_FAILED,
        CANCEL_ATTEMPT_FAILED,
        DISPOSE_ATTEMPT_FAILED,
        FINISH_ATTEMPT_FAILED,
        PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED,
        COMPONENT_HEARTBEAT_LOST,
        // failed
        FAILED,
        // finished
        ON_COMPONENTS_FINISHED,
        FINISHED
    }

    @Deprecated
    public WorkflowExecutionControllerImpl() {}

    public WorkflowExecutionControllerImpl(WorkflowExecutionContext wfContext) {
        this.wfExeCtx = wfContext;
        this.wfDataManagementStorage = new WorkflowExecutionStorageBridge(wfContext, metaDataService);
    }

    @Override
    public void start() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_REQUESTED));
    }

    @Override
    public void pause() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_REQUESTED));
    }

    @Override
    public void resume() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_REQUESTED));
    }

    @Override
    public void restart() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESTART_REQUESTED));
    }

    @Override
    public void cancel() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_REQUESTED));
    }

    @Override
    public void dispose() {
        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_REQUESTED));
    }

    @Override
    public void setComponentExecutionAuthTokens(Map<String, String> exeAuthTokens) {
        this.executionAuthTokens = exeAuthTokens;
    }

    @Override
    public WorkflowState getState() {
        return stateMachine.getState();
    }

    /**
     * Events the {@link WorkflowStateMachine} can process.
     * 
     * @author Doreen Seider
     */
    private static final class WorkflowStateMachineEvent {

        private final WorkflowStateMachineEventType type;

        private final Throwable throwable;

        public WorkflowStateMachineEvent(WorkflowStateMachineEventType type) {
            this(type, null);
        }

        public WorkflowStateMachineEvent(WorkflowStateMachineEventType type, Throwable throwable) {
            this.type = type;
            this.throwable = throwable;
        }

        public WorkflowStateMachineEventType getType() {
            return type;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public String toString() {
            return type.name();
        }

    }

    /**
     * Workflow-specific implementation of {@link AbstractStateMachine}.
     * 
     * @author Doreen Seider
     */
    private final class WorkflowStateMachine extends AbstractFixedTransitionsStateMachine<WorkflowState, WorkflowStateMachineEvent> {

        private Future<?> currentFuture = null;

        public WorkflowStateMachine() {
            super(WorkflowState.INIT, VALID_WORKFLOW_STATE_TRANSITIONS);
        }

        private boolean checkStateChange(WorkflowState currentState, WorkflowState newState) {
            if (isStateChangeValid(currentState, newState)) {
                return true;
            } else {
                logInvalidStateChangeRequest(currentState, newState);
                return false;
            }
        }

        @Override
        protected WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) throws StateChangeException {
            WorkflowState state = null;
            switch (event.getType()) {
            case START_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.PREPARING)) {
                    state = WorkflowState.PREPARING;
                    prepareAsync();
                }
                break;
            case PREPARE_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, WorkflowState.STARTING)) {
                    currentFuture = null;
                    startAsync();
                    state = WorkflowState.STARTING;
                }
                break;
            case START_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, WorkflowState.RUNNING)) {
                    currentFuture = null;
                    state = WorkflowState.RUNNING;
                }
                break;
            case PAUSE_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.PAUSING)) {
                    pauseAsync();
                    state = WorkflowState.PAUSING;
                }
                break;
            case PAUSE_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, WorkflowState.PAUSED)) {
                    state = WorkflowState.PAUSED;
                }
                break;
            case RESUME_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.RESUMING)) {
                    resumedStateListener = new ResumedComponentStateListener(finalComponentStateListener.getComponents());
                    resumedComonentStateLatch = new CountDownLatch(1);
                    resumeAsync();
                    state = WorkflowState.RESUMING;
                }
                break;
            case RESUME_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, WorkflowState.RUNNING)) {
                    pausedComponentStateListener = new PausedComponentStateListener(finalComponentStateListener.getComponents());
                    pausedComonentStateLatch = new CountDownLatch(1);
                    resumedStateListener = null;
                    state = WorkflowState.RUNNING;
                }
                break;
            case CANCEL_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.CANCELING)) {
                    state = WorkflowState.CANCELING;
                    cancelAsync();
                }
                break;
            case CANCEL_AFTER_FAILED_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.CANCELING_AFTER_FAILED)) {
                    state = WorkflowState.CANCELING_AFTER_FAILED;
                    if (event.getThrowable() != null) {
                        LOG.error(String.format("Workflow '%s' (%s) will be cancelled as a component failed", wfExeCtx.getInstanceName(),
                            wfExeCtx.getExecutionIdentifier()), event.getThrowable());
                    }
                    cancelAsync();
                }
                break;
            case CANCEL_ATTEMPT_SUCCESSFUL:
                if (currentState == WorkflowState.CANCELING) {
                    if (checkStateChange(currentState, WorkflowState.CANCELLED)) {
                        state = WorkflowState.CANCELLED;
                    }
                } else if (currentState == WorkflowState.CANCELING_AFTER_FAILED) {
                    if (checkStateChange(currentState, WorkflowState.FAILED)) {
                        state = WorkflowState.FAILED;
                    }
                }
                break;
            case DISPOSE_REQUESTED:
                if (checkStateChange(currentState, WorkflowState.DISPOSING)) {
                    state = WorkflowState.DISPOSING;
                    disposeAsync();
                }
                break;
            case DISPOSE_ATTEMPT_FAILED:
            case DISPOSE_ATTEMPT_SUCCESSFUL:
                if (checkStateChange(currentState, WorkflowState.DISPOSED)) {
                    currentFuture = null;
                    state = WorkflowState.DISPOSED;
                }
                break;
            case ON_COMPONENTS_FINISHED:
                if (checkStateChange(currentState, WorkflowState.FINISHED)) {
                    waitForFinishAsync();
                }
                break;
            case FINISHED:
                if (checkStateChange(currentState, WorkflowState.FINISHED)) {
                    state = WorkflowState.FINISHED;
                }
                break;
            case PREPARE_ATTEMPT_FAILED:
            case START_ATTEMPT_FAILED:
            case PAUSE_ATTEMPT_FAILED:
            case RESUME_ATTEMPT_FAILED:
            case FINISH_ATTEMPT_FAILED:
            case PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED:
            case COMPONENT_HEARTBEAT_LOST:
                currentFuture = null;
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED, event.getThrowable()));
                break;
            case CANCEL_ATTEMPT_FAILED:
                if (checkStateChange(currentState, WorkflowState.FAILED)) {
                    if (event.getThrowable() != null) {
                        LOG.error(String.format("Failed to cancel workflow '%s' (%s)", wfExeCtx.getInstanceName(),
                            wfExeCtx.getExecutionIdentifier()), event.getThrowable());
                    }
                    state = WorkflowState.FAILED;
                }
                break;
            default:
                break;
            }
            return state;
        }

        private void logInvalidStateChangeRequest(WorkflowState currentState, WorkflowState requestedState) {
            LOG.debug(String.format("Ignored workflow state change request for workflow '%s' (%s) as it will cause an invalid"
                + " state transition: %s -> %s", wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier(),
                currentState, requestedState));
        }

        @Override
        protected void onStateChanged(WorkflowState oldState, WorkflowState newState) {
            LOG.debug(String.format("Workflow '%s' (%s) is now %s (previous state: %s)",
                wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier(), newState, oldState));
            sendNewWorkflowStateAsConsoleRow(newState);
            notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeCtx.getExecutionIdentifier(), newState.name());
            if (newState == WorkflowState.DISPOSED) {
                disposeNotificationBuffers();
            }
        }

        @Override
        protected void onStateChangeException(WorkflowStateMachineEvent event, StateChangeException e) {
            LOG.error(String.format("Invalid state change attempt for workflow '%s' (%s), caused by event '%s'",
                wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier(), event), e);
        }

        void prepareAsync() {
            currentFuture = threadPool.submit(new AsyncPrepareTask());
        }

        void startAsync() {
            currentFuture = threadPool.submit(new AsyncStartTask());
        }

        void cancelAsync() {
            threadPool.submit(new AsyncCancelTask());
        }

        void pauseAsync() {
            threadPool.submit(new AsyncPauseTask());
        }

        void resumeAsync() {
            currentFuture = threadPool.submit(new AsyncResumeTask());
        }

        void disposeAsync() {
            currentFuture = threadPool.submit(new AsyncDisposeTask());
        }

        void waitForFinishAsync() {
            currentFuture = threadPool.submit(new AsyncWaitForFinishTask());
        }

        /**
         * Prepares the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncPrepareTask implements Runnable {

            @Override
            @TaskDescription("Prepare workflow")
            public void run() {

                notificationService.send(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID, wfExeCtx.getExecutionIdentifier());
                initializeNotificationBuffers();

                try {
                    wfDataManagementStorage.addWorkflowExecution(wfExeCtx);
                } catch (WorkflowExecutionException e) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, e));
                    return;
                }

                if (wfExeCtx.getWorkflowDescription().getWorkflowNodes().isEmpty()) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISHED));
                    return;
                }
                
                final Map<String, ComponentExecutionContext> compExeCtxts = createComponentExecutionContexts();
                
                CallablesGroup<Throwable> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Throwable.class);

                final Long referenceTimestamp = System.currentTimeMillis();

                for (WorkflowNode wfNode : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
                    final WorkflowNode finalWfNode = wfNode;
                    final ComponentExecutionContext compExeCtx = compExeCtxts.get(wfNode.getIdentifier());
                    callablesGroup.add(new Callable<Throwable>() {

                        @Override
                        @TaskDescription("Create component execution controller and perform prepare")
                        public Exception call() throws Exception {
                            try {
                                String compExeId = componentExecutionService.init(compExeCtx,
                                    executionAuthTokens.get(finalWfNode.getIdentifier()), referenceTimestamp);
                                componentExecutionIds.put(compExeId, compExeCtx.getNodeId());
                                disposeComponentImmediately.put(compExeId, !compExeCtx.getComponentDescription().performLazyDisposal());
                                componentExecutionService.prepare(compExeId,
                                    compExeCtx.getNodeId());
                            } catch (CommunicationException | RuntimeException e) {
                                return e;
                            }
                            return null;
                        }
                    }, "Prepare component: " + compExeCtx.getExecutionIdentifier());
                }

                List<Throwable> throwables = callablesGroup.executeParallel(new AsyncExceptionListener() {

                    @Override
                    public void onAsyncException(Exception e) {
                        // should never happen
                    }
                });

                for (Throwable t : throwables) {
                    if (t != null) {
                        LOG.error(String.format("Preparing workflow '%s' failed", wfExeCtx.getInstanceName()), t);
                    }
                }

                for (Throwable t : throwables) {
                    if (t != null) {
                        postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, t));
                        return;
                    }
                }
            }
        }

        private void initializeNotificationBuffers() {
            final int bufferSize = 500;
            notificationService.setBufferSize(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ConsoleRow.NOTIFICATION_SUFFIX, bufferSize);
            notificationService.setBufferSize(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeCtx.getExecutionIdentifier(), 1);
            notificationService.setBufferSize(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX, bufferSize);
            for (WorkflowNode wfNode : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
                String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier());
                // set to 3 as the state before the component is disposing->disposed must be
                // accessible by the GUI
                notificationService.setBufferSize(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, 3);
                notificationService.setBufferSize(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId, 1);
            }
        }

        private Map<String, ComponentExecutionContext> createComponentExecutionContexts() {
            WorkflowDescription workflowDescription = wfExeCtx.getWorkflowDescription();
            WorkflowGraph workflowGraph = createWorkflowGraph(workflowDescription);
            Map<String, ComponentExecutionContext> compExeCtxs = new HashMap<>();
            for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
                compExeCtxs.put(wfNode.getIdentifier(), createComponentExecutionContext(wfNode, workflowGraph));
            }
            return compExeCtxs;
        }

        private WorkflowGraph createWorkflowGraph(WorkflowDescription workflowDescription) {
            Map<String, WorkflowGraphNode> workflowGraphNodes = new HashMap<>();
            Map<String, Set<WorkflowGraphEdge>> workflowGraphEdges = new HashMap<>();
            for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
                Map<String, String> endpointNames = new HashMap<>();
                Set<String> inputIds = new HashSet<>();
                for (EndpointDescription ep : wn.getInputDescriptionsManager().getEndpointDescriptions()) {
                    inputIds.add(ep.getIdentifier());
                    endpointNames.put(ep.getIdentifier(), ep.getName());
                }
                Set<String> outputIds = new HashSet<>();
                for (EndpointDescription ep : wn.getOutputDescriptionsManager().getEndpointDescriptions()) {
                    outputIds.add(ep.getIdentifier());
                    endpointNames.put(ep.getIdentifier(), ep.getName());
                }
                String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wn.getIdentifier());
                workflowGraphNodes.put(compExeId, new WorkflowGraphNode(compExeId, inputIds, outputIds, endpointNames,
                    wn.getComponentDescription().getComponentInstallation().getComponentRevision()
                        .getComponentInterface().getIsResetSink()));
            }
            for (Connection cn : workflowDescription.getConnections()) {
                WorkflowGraphEdge edge = new WorkflowGraphEdge(
                    wfExeCtx.getCompExeIdByWfNodeId(cn.getSourceNode().getIdentifier()),
                    cn.getOutput().getIdentifier(),
                    wfExeCtx.getCompExeIdByWfNodeId(cn.getTargetNode().getIdentifier()),
                    cn.getInput().getIdentifier());
                String edgeKey = WorkflowGraph.createEdgeKey(edge);
                if (!workflowGraphEdges.containsKey(edgeKey)) {
                    workflowGraphEdges.put(edgeKey, new HashSet<WorkflowGraphEdge>());
                }
                workflowGraphEdges.get(edgeKey).add(edge);
            }
            return new WorkflowGraph(workflowGraphNodes, workflowGraphEdges);
        }

        private ComponentExecutionContext createComponentExecutionContext(WorkflowNode wfNode, WorkflowGraph workflowGraph) {
            String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier());
            WorkflowDescription workflowDescription = wfExeCtx.getWorkflowDescription();
            ComponentExecutionContextBuilder builder = new ComponentExecutionContextBuilder();
            builder.setExecutionIdentifiers(compExeId, wfExeCtx.getExecutionIdentifier());
            builder.setInstanceNames(wfNode.getName(), wfExeCtx.getInstanceName());
            builder.setNodes(wfExeCtx.getNodeId(), wfExeCtx.getDefaultStorageNodeId());
            builder.setComponentDescription(wfNode.getComponentDescription());
            boolean isConnectedToEndpointDatumSenders = false;
            for (Connection cn : workflowDescription.getConnections()) {
                if (cn.getTargetNode().getIdentifier().equals(wfNode.getIdentifier())) {
                    isConnectedToEndpointDatumSenders = true;
                    break;
                }
            }
            Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients = new HashMap<>();
            for (Connection cn : workflowDescription.getConnections()) {
                if (cn.getSourceNode().getIdentifier().equals(wfNode.getIdentifier())) {
                    EndpointDatumRecipient endpointDatumRecipient = EndpointDatumRecipientFactory
                            .createEndpointDatumRecipient(cn.getInput().getName(),
                                wfExeCtx.getCompExeIdByWfNodeId(cn.getTargetNode().getIdentifier()),
                                cn.getTargetNode().getComponentDescription().getNode());
                    if (!endpointDatumRecipients.containsKey(cn.getOutput().getName())) {
                        endpointDatumRecipients.put(cn.getOutput().getName(), new ArrayList<EndpointDatumRecipient>());
                    }
                    endpointDatumRecipients.get(cn.getOutput().getName()).add(endpointDatumRecipient);
                }
            }
            builder.setPredecessorAndSuccessorInformation(isConnectedToEndpointDatumSenders, endpointDatumRecipients);
            builder.setWorkflowGraph(workflowGraph);
            Long compInstanceDmId = wfDataManagementStorage.getComponentInstanceDataManamagementId(
                wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier()));
            Map<String, Long> inputDataManagementIds = wfDataManagementStorage.getInputInstanceDataManamagementIds(
                wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier()));
            Map<String, Long> outputDataManagementIds = wfDataManagementStorage.getOutputInstanceDataManamagementIds(
                wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier()));
            builder.setDataManagementIds(wfDataManagementStorage.getWorkflowInstanceDataManamagementId(),
                compInstanceDmId, inputDataManagementIds, outputDataManagementIds);
            return builder.build();
        }

        /**
         * Starts the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncStartTask implements Runnable {

            @Override
            @TaskDescription("Start workflow")
            public void run() {
                sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_STARTING);

                Throwable throwable = new ParallelComponentCaller() {
                        @Override
                        public void logError(Throwable t) {
                            LOG.error(String.format("Preparing workflow '%s' (%s) failed", wfExeCtx.getInstanceName(),
                                wfExeCtx.getExecutionIdentifier()), t);
                        }
                        @Override
                        public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                            setComponentStateToFailed(compExeId);
                        }
                        @Override
                        public void callSingleComponent(String compExeId) throws CommunicationException {
                            componentExecutionService.start(compExeId, componentExecutionIds.get(compExeId));
                            onComponentHeartbeatReceived(compExeId);
                        }
                    }.callParallelAndWait();

                if (throwable == null) {
                    heartbeatFuture = threadPool.scheduleAtFixedRate(componentHeartbeatRunnable, MAX_HEARTBEAT_INTERVAL_MSEC);
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_ATTEMPT_SUCCESSFUL));
                } else {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_ATTEMPT_FAILED, throwable));
                }
            }
        }

        /**
         * Pauses the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncPauseTask implements Runnable {

            @Override
            @TaskDescription("Pause workflow")
            public void run() {

                Throwable throwable = new ParallelComponentCaller(true, true) {
                        @Override
                        public void logError(Throwable t) {
                            LOG.error(String.format("Pausing workflow '%s' (%s) failed", wfExeCtx.getInstanceName(),
                                wfExeCtx.getExecutionIdentifier()), t);
                        }
                        @Override
                        public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                            pausedComonentStateLatch.countDown();
                            setComponentStateToFailed(compExeId);
                        }
                        @Override
                        public void callSingleComponent(String compExeId) throws CommunicationException {
                            componentExecutionService.pause(compExeId, componentExecutionIds.get(compExeId));
                        }
                    }.callParallelAndWait();

                try {
                    pausedComonentStateLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.debug(String.format("Waiting for components to pause (workflow '%s' (%s)) was interrupted",
                        wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED, e));
                    return;
                }
                if (throwable == null) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_SUCCESSFUL));
                } else {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED, throwable));
                }
            }
        }

        /**
         * Resumes the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncResumeTask implements Runnable {

            @Override
            @TaskDescription("Resume workflow")
            public void run() {
                Throwable throwable = new ParallelComponentCaller(true, true) {
                        @Override
                        public void logError(Throwable t) {
                            LOG.error(String.format("Resuming workflow '%s' (%s) failed", wfExeCtx.getInstanceName(),
                                wfExeCtx.getExecutionIdentifier()), t);
                        }
                        @Override
                        public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                            resumedComonentStateLatch.countDown();
                            setComponentStateToFailed(compExeId);
                        }
                        @Override
                        public void callSingleComponent(String compExeId) throws CommunicationException {
                            componentExecutionService.resume(compExeId, componentExecutionIds.get(compExeId));
                        }
                    }.callParallelAndWait();

                try {
                    resumedComonentStateLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.debug(String.format("Waiting for components to pause (workflow '%s' (%s)) was interrupted",
                        wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED, e));
                    return;
                }
                if (throwable == null) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_SUCCESSFUL));
                } else {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED, throwable));
                }
            }
        }

        /**
         * Cancels the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncCancelTask implements Runnable {

            @Override
            @TaskDescription("Cancel workflow")
            public void run() {
                if (currentFuture != null) {
                    try {
                        currentFuture.get();
                    } catch (ExecutionException e) {
                        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                        return;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                Throwable throwable = null;
                if (!componentExecutionIds.isEmpty()) {
                    throwable = new ParallelComponentCaller(true, true) {
    
                            @Override
                            public void callSingleComponent(String compExeId) throws CommunicationException {
                                componentExecutionService.cancel(compExeId, componentExecutionIds.get(compExeId));
                            }
                            
                            @Override
                            public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                                finalComponentStateListener.addComponent(compExeId);
                                lastConsoleRowListener.addComponent(compExeId);
                                setComponentStateToFailed(compExeId);
                            }
        
                            @Override
                            public void logError(Throwable t) {
                                LOG.error(String.format("Cancelling workflow '%s' (%s) failed", wfExeCtx.getInstanceName(),
                                    wfExeCtx.getExecutionIdentifier()), t);
                            }
                            
                        }.callParallelAndWait();
    
                    int compsNotInitCount = wfExeCtx.getWorkflowDescription().getWorkflowNodes().size() - componentExecutionIds.size();
                    for (int i = 0; i < compsNotInitCount; i++) {
                        String pseudoCompExeId = String.valueOf(i);
                        finalComponentStateListener.addComponent(pseudoCompExeId);
                        lastConsoleRowListener.addComponent(pseudoCompExeId);
                    }
                    try {
                        workflowTerminatedLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.debug(String.format("Waiting for components to cancel (workflow '%s' (%s)) was interrupted",
                            wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
                        stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                        return;
                    }
                }
                try {
                    if (getState() == WorkflowState.CANCELING_AFTER_FAILED || throwable != null) {
                        wfDataManagementStorage.setWorkflowExecutionFinished(FinalWorkflowState.FAILED);
                    } else if (getState() == WorkflowState.CANCELING) {
                        wfDataManagementStorage.setWorkflowExecutionFinished(FinalWorkflowState.CANCELLED);
                    }
                } catch (WorkflowExecutionException e) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                    return;
                }
                if (throwable == null) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
                } else {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, throwable));
                }
            }
        }

        /**
         * Wait for the workflow to finish.
         * 
         * @author Doreen Seider
         */
        private final class AsyncWaitForFinishTask implements Runnable {

            @Override
            @TaskDescription("Wait for workflow to finish")
            public void run() {
                try {
                    workflowTerminatedLatch.await();
                    wfDataManagementStorage.setWorkflowExecutionFinished(FinalWorkflowState.FINISHED);
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISHED));
                } catch (InterruptedException e) {
                    LOG.error(String.format("Waiting for workflow '%s' (%s) to finish failed", wfExeCtx.getInstanceName(),
                        wfExeCtx.getExecutionIdentifier()), e);
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FAILED, e));
                } catch (WorkflowExecutionException e) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISH_ATTEMPT_FAILED, e));
                }
            }
        }

        /**
         * Disposes the workflow.
         * 
         * @author Doreen Seider
         */
        private final class AsyncDisposeTask implements Runnable {

            @Override
            @TaskDescription("Dispose workflow")
            public void run() {
                notificationService.send(WorkflowConstants.STATE_DISPOSED_NOTIFICATION_ID, wfExeCtx.getExecutionIdentifier());
                Throwable throwable = new ParallelComponentCaller(true, false) {
                        @Override
                        public void logError(Throwable t) {
                            LOG.error(String.format("Disposing workflow '%s' (%s) failed", wfExeCtx.getInstanceName(),
                                wfExeCtx.getExecutionIdentifier()), t);
                        }
                        @Override
                        public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                            disposedComonentStateLatch.countDown();
                        }
                        @Override
                        public void callSingleComponent(String compExeId) throws CommunicationException {
                            componentExecutionService.dispose(compExeId, componentExecutionIds.get(compExeId));
                        }
                    }.callParallelAndWait();

                try {
                    disposedComonentStateLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.debug("Waiting for components to dispose was interrupted");
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED, e));
                    return;
                }
                if (throwable == null) {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL));
                } else {
                    stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED, throwable));
                }
            }
        }

        private void disposeNotificationBuffers() {
            notificationService.removePublisher(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeCtx.getExecutionIdentifier());
            notificationService.removePublisher(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ConsoleRow.NOTIFICATION_SUFFIX);
            notificationService.removePublisher(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX);
            for (WorkflowNode wfNode : wfExeCtx.getWorkflowDescription().getWorkflowNodes()) {
                String compExeId = wfExeCtx.getCompExeIdByWfNodeId(wfNode.getIdentifier());
                notificationService.removePublisher(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId);
                notificationService.removePublisher(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId);
            }
        }

        /**
         * Helper class to call components in parallel.
         * 
         * @author Doreen Seider
         */
        private abstract class ParallelComponentCaller {

            private final boolean ignoreDisposedComponents;

            private final boolean ignoreComponentsInFinalState;

            public ParallelComponentCaller() {
                this(false, false);
            }

            public ParallelComponentCaller(boolean ignoreDisposedComponents, boolean ignoreComponentsInFinalState) {
                this.ignoreDisposedComponents = ignoreDisposedComponents;
                this.ignoreComponentsInFinalState = ignoreComponentsInFinalState;
            }

            public Throwable callParallelAndWait() {
                CallablesGroup<Throwable> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Throwable.class);
                for (String executionId : componentExecutionIds.keySet()) {
                    final String finalExecutionId = executionId;
                    callablesGroup.add(new Callable<Throwable>() {

                        @Override
                        @TaskDescription("Call method of workflow component")
                        public Throwable call() throws Exception {
                            try {
                                if (!ignoreDisposedComponents || !disposedComponentStateListener.hasComponent(finalExecutionId)
                                    && !ignoreComponentsInFinalState || !finalComponentStateListener.hasComponent(finalExecutionId)) {
                                    callSingleComponent(finalExecutionId);
                                }
                            } catch (CommunicationException | RuntimeException e) {
                                onErrorInSingleComponentCall(finalExecutionId, e);
                                return e;
                            }
                            return null;
                        }
                    }, "Call component: " + finalExecutionId);
                }

                List<Throwable> throwables = callablesGroup.executeParallel(new AsyncExceptionListener() {

                    @Override
                    public void onAsyncException(Exception e) {
                        // should never happen
                    }
                });

                for (Throwable t : throwables) {
                    if (t != null) {
                        logError(t);
                    }
                }

                for (Throwable t : throwables) {
                    if (t != null) {
                        return t;
                    }
                }
                return null;
            }

            public abstract void callSingleComponent(String compExeId) throws CommunicationException;

            public void onErrorInSingleComponentCall(String compExeId, Throwable t) {}
            
            public void logError(Throwable t) {}

        }
    }

    private void setComponentStateToFailed(String compExeId) {
        notificationService.send(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, ComponentState.FAILED.name());
    }
    
    @Override
    @AllowRemoteAccess
    public void onComponentStateChanged(String compExeId, ComponentState oldState, ComponentState newState,
        Integer executionCount, String executionCountOnResets) {
        onComponentStateChanged(compExeId, oldState, newState, executionCount, executionCountOnResets, null);
    }

    @Override
    @AllowRemoteAccess
    public synchronized void onComponentStateChanged(final String compExeId, ComponentState oldState, ComponentState newState,
        Integer executionCount, String executionCountOnResets, Throwable t) {
        notificationService.send(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, newState.name());
        if (resumedStateListener != null) {
            resumedStateListener.addComponent(compExeId);
        }
        switch (newState) {
        case PREPARED:
            preparedComponentStateListener.addComponent(compExeId);
            break;
        case FINISHED:
        case FINISHED_WITHOUT_EXECUTION:
            finishedComponentStateListener.addComponent(compExeId);
            break;
        case PAUSED:
            pausedComponentStateListener.addComponent(compExeId);
            break;
        case DISPOSED:
            disposedComponentStateListener.addComponent(compExeId);
            break;
        case FAILED:
            stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED, t));
            break;
        default:
            break;
        }

        if (ComponentConstants.FINAL_COMPONENT_STATES.contains(newState)) {
            finalComponentStateListener.addComponent(compExeId);
            pausedComponentStateListener.addComponent(compExeId);
            if (disposeComponentImmediately.get(compExeId)) {
                threadPool.execute(new Runnable() {
                    
                    @Override
                    @TaskDescription("Dispose component")
                    public void run() {
                        try {
                            componentExecutionService.dispose(compExeId, componentExecutionIds.get(compExeId));
                        } catch (CommunicationException e) {
                            LOG.error(String.format("Failed to dispose component %s", compExeId), e);
                        }
                    }
                });
            }
        }
        notificationService.send(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX
            + compExeId, executionCountOnResets);
    }

    @Override
    @AllowRemoteAccess
    public void onInputProcessed(String serializedEndpointDatum) {
        notificationService.send(wfExeCtx.getExecutionIdentifier()
            + wfExeCtx.getNodeId().getIdString()
            + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX, serializedEndpointDatum);
    }

    @Override
    @AllowRemoteAccess
    public void processConsoleRows(ConsoleRow[] consoleRows) {
        for (ConsoleRow row : consoleRows) {
            notificationService.send(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ConsoleRow.NOTIFICATION_SUFFIX, row);
            try {
                checkForLifecycleToolRunConsoleRow(row);
            } catch (WorkflowExecutionException e) {
                stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType
                    .PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED, e));
            }
            checkForLifecycleInfoEndConsoleRow(row);
        }
    }
    
    @Override
    @AllowRemoteAccess
    public void onComponentHeartbeatReceived(String executionIdentifier) {
        componentHeartbeatTimestamps.put(executionIdentifier, System.currentTimeMillis());
    }

    private void checkForLifecycleInfoEndConsoleRow(ConsoleRow row) {
        if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
            && row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED.name())) {
            lastConsoleRowListener.addComponent(row.getComponentIdentifier());
        }
    }
    
    private void checkForLifecycleToolRunConsoleRow(ConsoleRow row) throws WorkflowExecutionException {
        if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT) {
            if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.TOOL_STARTING.name())) {
                wfDataManagementStorage.addComponentTimelineInterval(TimelineIntervalType
                    .EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN, row.getTimestamp(), StringUtils.splitAndUnescape(row.getPayload())[1]);
            } else if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.TOOL_FINISHED.name())) {
                wfDataManagementStorage.setComponentTimelineIntervalFinished(TimelineIntervalType.EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN,
                    row.getTimestamp(), StringUtils.splitAndUnescape(row.getPayload())[1]);
            }
        }
    }

    private void sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType type) {
        sendConsoleRowAsNotification(type.name());
    }

    private void sendNewWorkflowStateAsConsoleRow(WorkflowState newState) {
        // send a LIFE_CYCLE_EVENT of subtype NEW_STATE with the new state's enum name attached
        String payload = StringUtils.escapeAndConcat(ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name(), newState.name());
        sendConsoleRowAsNotification(payload);
    }

    private void sendConsoleRowAsNotification(String payload) {
        ConsoleRowBuilder consoleRowBuilder = new ConsoleRowBuilder();
        consoleRowBuilder.setExecutionIdentifiers(wfExeCtx.getExecutionIdentifier(), "")
            .setInstanceNames(wfExeCtx.getInstanceName(), "")
            .setType(ConsoleRow.Type.LIFE_CYCLE_EVENT)
            .setPayload(payload);
        notificationService.send(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
            + ConsoleRow.NOTIFICATION_SUFFIX, consoleRowBuilder.build());
    }

    /**
     * Abstract implementation of specific component state change listeners. It accepts component
     * execution identifiers and adds them to an underlying set. If the size of the set is equal to
     * the component amount of the workflow the
     * {@link ComponentEventListener#onComponentStatesChangedEntirely()} is called.
     * 
     * @author Doreen Seider
     */
    private abstract class ComponentEventListener {

        protected Set<String> compExeIds = Collections.synchronizedSet(new HashSet<String>() {

            private static final long serialVersionUID = 6431615134151724870L;

            @Override
            public boolean add(String e) {
                boolean rv = super.add(e);
                if (size() == wfExeCtx.getWorkflowDescription().getWorkflowNodes().size()) {
                    onComponentStatesChangedEntirely();
                }
                return rv;
            };
        });

        public synchronized void addComponent(String executionIdentifier) {
            compExeIds.add(executionIdentifier);
        }

        public synchronized Set<String> getComponents() {
            return Collections.unmodifiableSet(compExeIds);
        }

        public synchronized boolean hasComponent(String executionIdentifier) {
            return compExeIds.contains(executionIdentifier);
        }

        abstract void onComponentStatesChangedEntirely();
    }

    /**
     * {@link ComponentEventListener} listening to {@link ComponentState#PREPARED}.
     * 
     * @author Doreen Seider
     */
    private class PreparedComponentStateListener extends ComponentEventListener {

        @Override
        void onComponentStatesChangedEntirely() {
            stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_SUCCESSFUL));
        }
    }

    /**
     * {@link ComponentEventListener} listening to {@link ComponentState#FINISHED}.
     * 
     * @author Doreen Seider
     */
    private class FinishedComponentStateListener extends ComponentEventListener {

        @Override
        void onComponentStatesChangedEntirely() {
            stateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.ON_COMPONENTS_FINISHED));
        }
    }

    /**
     * {@link ComponentEventListener} listening to {@link ComponentState#PAUSED}.
     * 
     * @author Doreen Seider
     */
    private class PausedComponentStateListener extends ComponentEventListener {

        public PausedComponentStateListener() {}

        public PausedComponentStateListener(Set<String> compExeIds) {
            this.compExeIds.addAll(compExeIds);
        }

        @Override
        void onComponentStatesChangedEntirely() {
            pausedComonentStateLatch.countDown();
        }
    }

    /**
     * {@link ComponentEventListener} listening to all states a component can have after it was
     * paused.
     * 
     * @author Doreen Seider
     */
    private class ResumedComponentStateListener extends ComponentEventListener {

        public ResumedComponentStateListener(Set<String> compExeIds) {
            this.compExeIds.addAll(compExeIds);
        }

        @Override
        void onComponentStatesChangedEntirely() {
            resumedComonentStateLatch.countDown();
        }
    }

    /**
     * {@link ComponentEventListener} listening to any final {@link ComponentState}.
     * 
     * @author Doreen Seider
     */
    private class FinalComponentStateListener extends ComponentEventListener {

        @Override
        void onComponentStatesChangedEntirely() {
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);                
            }
            workflowTerminatedLatch.countDown();
        }
    }

    /**
     * {@link ComponentEventListener} listening to {@link ComponentState#DISPOSED}.
     * 
     * @author Doreen Seider
     */
    private class DisposedComponentStateListener extends ComponentEventListener {

        @Override
        void onComponentStatesChangedEntirely() {
            disposedComonentStateLatch.countDown();
        }
    }

    /**
     * {@link ComponentEventListener} listening to a specific {@link ConsoleRow} sent at the end.
     * 
     * @author Doreen Seider
     */
    private class LastConsoleRowListener extends ComponentEventListener {

        @Override
        void onComponentStatesChangedEntirely() {
            sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_FINISHED);
            workflowTerminatedLatch.countDown();
        }
    }
    
    protected void bindDistributedNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }
    
    protected void bindDistributedComponentControllerService(ComponentExecutionService newService) {
        componentExecutionService = newService;
    }
    
    protected void bindMetaDataService(MetaDataService newService) {
        metaDataService = newService;
    }

}
