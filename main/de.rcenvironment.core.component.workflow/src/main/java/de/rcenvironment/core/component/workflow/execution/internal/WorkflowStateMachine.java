/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContextBuilder;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.ConsoleRowBuilder;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.api.WorkflowGraphEdge;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.concurrent.ThreadPool;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.AbstractStateMachine;
import de.rcenvironment.core.utils.incubator.StateChangeException;

/**
 * Workflow-specific implementation of {@link AbstractStateMachine}.
 * 
 * @author Doreen Seider
 */
public class WorkflowStateMachine extends AbstractFixedTransitionsStateMachine<WorkflowState, WorkflowStateMachineEvent>
    implements ComponentStatesChangedEntirelyListener {

    private static final long WAIT_INTERVAL_TERMINATED_SEC = 60;
    
    private static final String CAUSE_WAITING_TIME_ELAPSED_SEC = "; cause: waiting time (%d seconds) elapsed";
    
    private static final String CAUSE_WAITING_TIME_ELAPSED_HRS = "; cause: waiting time (%d hours) elapsed";
    
    private static final Log LOG = LogFactory.getLog(WorkflowStateMachine.class);

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
        { WorkflowState.PAUSING, WorkflowState.FINISHED },
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
    
    private static CommunicationService communicationService;
    
    private static DistributedNotificationService notificationService;
    
    private static ComponentExecutionService componentExecutionService;
    
    private static WorkflowExecutionStatsService wfExeStatsService;
    
    // set visibility to protected for test purposes
    protected final Map<WorkflowStateMachineEventType, EventProcessor> eventProcessors = new HashMap<>();

    private String wfNameAndIdMessagePart;

    private final ThreadPool threadPool = SharedThreadPool.getInstance();

    private WorkflowStateMachineContext wfStateMachineCtx;
    
    private WorkflowDescription fullWorkflowDescription;

    private Map<String, String> executionAuthTokens;
    
    private ScheduledFuture<?> heartbeatFuture;
    
    private final Map<String, NodeIdentifier> componentNodeIds = Collections
        .synchronizedMap(new HashMap<String, NodeIdentifier>());
    
    private final Map<String, String> componentInstanceNames = Collections
        .synchronizedMap(new HashMap<String, String>());
    
    private final CountDownLatch workflowTerminatedLatch = new CountDownLatch(2);

    private CountDownLatch pausedComonentStateLatch = new CountDownLatch(1);

    private CountDownLatch resumedComonentStateLatch = new CountDownLatch(1);

    private final CountDownLatch disposedComponentStateLatch = new CountDownLatch(1);
    
    private Future<?> currentTask = null;

    @Deprecated
    public WorkflowStateMachine() {
        super(WorkflowState.INIT, VALID_WORKFLOW_STATE_TRANSITIONS);
    }
    
    public WorkflowStateMachine(WorkflowStateMachineContext wfStateMachineCtx) {
        super(WorkflowState.INIT, VALID_WORKFLOW_STATE_TRANSITIONS);
        this.wfStateMachineCtx = wfStateMachineCtx;
        this.fullWorkflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().clone();
        WorkflowExecutionUtils
            .removeDisabledWorkflowNodesWithoutNotify(wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription());
        this.wfNameAndIdMessagePart = String.format("'%s' (%s)", wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName(),
            wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
        
        initializeEventProcessors();
    }
    
    // set visibility to protected for test purposes
    protected void initializeEventProcessors() {
        eventProcessors.put(WorkflowStateMachineEventType.START_REQUESTED, new StartRequestedEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.PREPARE_ATTEMPT_SUCCESSFUL, new PrepareAttemptSuccessfulEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.START_ATTEMPT_SUCCESSFUL, new StartAttemptSuccessfulEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.PAUSE_REQUESTED, new PauseRequestedEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.PAUSE_ATTEMPT_SUCCESSFUL, new PauseAttemptSuccessfulEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.RESUME_REQUESTED, new ResumeRequestedEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.RESUME_ATTEMPT_SUCCESSFUL, new ResumeAttemptSuccessfulEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_REQUESTED, new CancelRequestedEventProcessor());
        CancelAfterFailedRequestedEventProcessor cancelAfterFailedRequestedEventProcessor = new CancelAfterFailedRequestedEventProcessor();
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_AFTER_COMPONENT_LOST_REQUESTED, cancelAfterFailedRequestedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED, cancelAfterFailedRequestedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL, new CancelAttemptSuccessufulEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.DISPOSE_REQUESTED, new DisposeRequestedEventProcessor());
        DisposeAttemptSuccessfulOrFailedEventProcessor disposeAttemptSuccessfulOrFailedEventProcessor =
            new DisposeAttemptSuccessfulOrFailedEventProcessor();
        eventProcessors.put(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL, disposeAttemptSuccessfulOrFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED, disposeAttemptSuccessfulOrFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.ON_COMPONENTS_FINISHED, new OnComponentsFinishedEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.FINISHED, new FinishedEventProcessor());
        PrepareStartPauseResumeFinishTimelineAttemptFailedEventProcessor variousAttemptsFailedEventProcessor =
            new PrepareStartPauseResumeFinishTimelineAttemptFailedEventProcessor();
        eventProcessors.put(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.START_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.FINISH_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED, variousAttemptsFailedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.COMPONENT_HEARTBEAT_LOST, new ComponentHeartbeatLostEventProcessor());
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, new CancelAttemptFailedEventProcessor());
    }

    @Override
    protected WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) throws StateChangeException {
        return eventProcessors.get(event.getType()).processEvent(currentState, event);
    }
    
    public void setComponentExecutionAuthTokens(Map<String, String> exeAuthTokens) {
        this.executionAuthTokens = exeAuthTokens;
    }
    
    private boolean checkStateChange(WorkflowState currentState, WorkflowState newState) {
        if (isStateChangeValid(currentState, newState)) {
            return true;
        } else {
            logInvalidStateChangeRequest(currentState, newState);
            return false;
        }
    }

    private void handleFailure(WorkflowStateMachineEvent event) {
        if (event.getThrowable() != null) {
            String errorMessagePrefix = StringUtils.format("Workflow %s failed and will be cancelled", wfNameAndIdMessagePart);
            String errorId = LogUtils.logExceptionAsSingleLineAndAssignUniqueMarker(LOG, errorMessagePrefix, event.getThrowable());
            String errorMessage = ComponentUtils.createErrorLogMessage(event.getThrowable(), errorId);
            storeAndSendErrorLogMessage(ConsoleRow.Type.WORKFLOW_ERROR, errorMessage, "", "");
        } else {
            final NodeIdentifier componentNode = componentNodeIds.get(event.getComponentExecutionId());
            final String componentName = componentInstanceNames.get(event.getComponentExecutionId());
            String errorMessagePrefix = StringUtils.format(
                "Workflow %s will be cancelled because component '%s' on %s failed",
                wfNameAndIdMessagePart, componentName, componentNode);
            if (event.getErrorMessage() != null) {
                String errorMessage = ComponentUtils.createErrorLogMessage(event.getErrorMessage(), event.getErrorId());
                storeAndSendErrorLogMessage(ConsoleRow.Type.COMPONENT_ERROR, errorMessage, event.getComponentExecutionId(),
                    componentName);
            }
            LOG.error(StringUtils.format("%s: for more information, look for the error marker %s in the log files of %s",
                errorMessagePrefix, event.getErrorId(), componentNode));
        }
    }

    private void logInvalidStateChangeRequest(WorkflowState currentState, WorkflowState requestedState) {
        LOG.debug(StringUtils.format("Ignored workflow state change request for workflow %s as it will cause an invalid"
            + " state transition: %s -> %s", wfNameAndIdMessagePart, currentState, requestedState));
    }

    @Override
    protected void onStateChanged(WorkflowState oldState, WorkflowState newState) {
        LOG.debug(StringUtils.format("Workflow %s is now %s (previous state: %s)", wfNameAndIdMessagePart, newState, oldState));
        sendNewWorkflowStateAsConsoleRow(newState);
        notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID
            + wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier(),
            newState.name());
        if (newState == WorkflowState.DISPOSED) {
            disposeNotificationBuffers();
        }
        if (newState.equals(WorkflowState.FINISHED) || newState.equals(WorkflowState.CANCELLED)
            || newState.equals(WorkflowState.FAILED)) {
            wfExeStatsService.addStatsAtWorkflowTermination(wfStateMachineCtx.getWorkflowExecutionContext(), newState);
            synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
                if (heartbeatFuture != null && !heartbeatFuture.isCancelled()) {
                    heartbeatFuture.cancel(false);
                }
            }
            sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_FINISHED);
        }
    }

    @Override
    protected void onStateChangeException(WorkflowStateMachineEvent event, StateChangeException e) {
        LOG.error(StringUtils.format("Invalid state change attempt for workflow %s, caused by event '%s'",
            wfNameAndIdMessagePart, event), e);
    }

    void prepareAsync() {
        currentTask = threadPool.submit(new AsyncPrepareTask());
    }

    void startAsync() {
        currentTask = threadPool.submit(new AsyncStartTask());
    }

    void cancelAsync() {
        threadPool.submit(new AsyncCancelTask(currentTask));
    }

    void pauseAsync() {
        threadPool.submit(new AsyncPauseTask());
    }

    void resumeAsync() {
        currentTask = threadPool.submit(new AsyncResumeTask());
    }

    void disposeAsync() {
        currentTask = threadPool.submit(new AsyncDisposeTask());
    }

    void waitForFinishAsync() {
        currentTask = threadPool.submit(new AsyncWaitForFinishTask());
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

            wfExeStatsService.addStatsAtWorkflowStart(wfStateMachineCtx.getWorkflowExecutionContext());

            notificationService.send(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID,
                wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
            initializeNotificationBuffers();

            initializeConsoleLogWriting();

            try {
                wfStateMachineCtx.getWorkflowExecutionStorageBridge().addWorkflowExecution(
                    wfStateMachineCtx.getWorkflowExecutionContext(), fullWorkflowDescription);
            } catch (WorkflowExecutionException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, e));
                return;
            } finally {
                fullWorkflowDescription = null;
            }

            if (wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes().isEmpty()) {
                onComponentStatesChangedCompletelyToPrepared();
            }

            final Map<String, ComponentExecutionContext> compExeCtxts = createComponentExecutionContexts();

            try {
                checkForUnreachableComponentNodes(compExeCtxts);
            } catch (WorkflowExecutionException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, e));
                return;
            }

            CallablesGroup<Throwable> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Throwable.class);

            final Long referenceTimestamp = System.currentTimeMillis();

            for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
                final WorkflowNode finalWfNode = wfNode;
                final ComponentExecutionContext compExeCtx = compExeCtxts.get(wfNode.getIdentifier());
                callablesGroup.add(new Callable<Throwable>() {

                    @Override
                    @TaskDescription("Create component execution controller and perform prepare")
                    public Exception call() throws Exception {
                        try {
                            String compExeId = componentExecutionService.init(compExeCtx,
                                executionAuthTokens.get(finalWfNode.getIdentifier()), referenceTimestamp);
                            componentNodeIds.put(compExeId, compExeCtx.getNodeId());
                            componentInstanceNames.put(compExeId, compExeCtx.getInstanceName());
                            initializeComponentConsoleLogWriting(compExeId);
                            componentExecutionService.prepare(compExeId, compExeCtx.getNodeId());
                            LOG.debug(StringUtils.format("Created component '%s' (%s) on node %s",
                                compExeCtx.getInstanceName(), compExeId, compExeCtx.getNodeId()));
                        } catch (RemoteOperationException | RuntimeException e) {
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
                    LOG.error(StringUtils.format("Failed to prepare workflow %s", wfNameAndIdMessagePart), t);
                }
            }

            for (Throwable t : throwables) {
                if (t != null) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED,
                        new Throwable("Failed to prepare workflow", t)));
                    return;
                }
            }

            LOG.debug(StringUtils.format("Workflow %s is prepared (%d component(s))", wfNameAndIdMessagePart, compExeCtxts.size()));
        }
    }

    private void checkForUnreachableComponentNodes(Map<String, ComponentExecutionContext> compExeCtxts)
        throws WorkflowExecutionException {

        Map<String, ComponentExecutionContext> notReachableCompExeIds = new HashMap<>();

        Set<NodeIdentifier> reachableNodes = communicationService.getReachableNodes();
        for (ComponentExecutionContext compExeCtx : compExeCtxts.values()) {
            // stored for logging purposes, see createMessageListingComponents; should be improved
            componentInstanceNames.put(compExeCtx.getExecutionIdentifier(), compExeCtx.getInstanceName());
            componentNodeIds.put(compExeCtx.getExecutionIdentifier(), compExeCtx.getNodeId());
            if (!reachableNodes.contains(compExeCtx.getNodeId())) {
                notReachableCompExeIds.put(compExeCtx.getExecutionIdentifier(), compExeCtx);
            }
        }

        try {
            if (!notReachableCompExeIds.isEmpty()) {
                for (ComponentExecutionContext compExeCtx : compExeCtxts.values()) {
                    String compExeId = compExeCtx.getExecutionIdentifier();
                    wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().accounceComponentInAnyFinalState(compExeId);
                    wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().announceLastConsoleRow(compExeId);
                    if (reachableNodes.contains(compExeCtx.getNodeId())) {
                        sendComponentStateCanceled(compExeId);
                    } else {
                        sendComponentStateFailed(compExeId);
                    }
                }
                throw new WorkflowExecutionException("Failed to execute workflow because component node(s) not reachable: "
                    + createMessageListingComponents(notReachableCompExeIds.keySet()));
            }
        } finally {
            componentInstanceNames.clear();
            componentNodeIds.clear();
        }
    }

    private void initializeComponentConsoleLogWriting(String compExeId) {
        try {
            wfStateMachineCtx.getComponentsConsoleLogFileWriter().initializeComponentLogFile(compExeId);
        } catch (IOException e) {
            LOG.error(StringUtils.format("Failed to initialize console log file writers for workflow %s", wfNameAndIdMessagePart), e);
        }
    }

    private void initializeConsoleLogWriting() {
        try {
            wfStateMachineCtx.getComponentsConsoleLogFileWriter().initializeWorkflowLogFile();
        } catch (IOException e) {
            LOG.error(StringUtils.format("Failed to initialize console log file writer for workflow %s", wfNameAndIdMessagePart), e);
        }
    }

    private void initializeNotificationBuffers() {
        final int bufferSize = 500;
        notificationService.setBufferSize(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString() + ConsoleRow.NOTIFICATION_SUFFIX, bufferSize);
        notificationService.setBufferSize(WorkflowConstants.STATE_NOTIFICATION_ID
            + wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier(), 1);
        notificationService.setBufferSize(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString()
            + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX, bufferSize);
        for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
            String compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(wfNode.getIdentifier());
            // set to 3 as the state before the component is disposing->disposed must be
            // accessible by the GUI
            notificationService.setBufferSize(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, 3);
            notificationService.setBufferSize(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId, 1);
        }
    }

    private Map<String, ComponentExecutionContext> createComponentExecutionContexts() {
        WorkflowDescription workflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription();
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
            String compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(wn.getIdentifier());
            workflowGraphNodes.put(compExeId, new WorkflowGraphNode(compExeId, inputIds, outputIds, endpointNames,
                wn.getComponentDescription().getComponentInstallation().getComponentRevision()
                    .getComponentInterface().getIsLoopDriver(),
                isDrivingFaultTolerantNode(wn)));
        }
        for (Connection cn : workflowDescription.getConnections()) {
            WorkflowGraphEdge edge = new WorkflowGraphEdge(
                wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(cn.getSourceNode().getIdentifier()),
                cn.getOutput().getIdentifier(), cn.getOutput().getMetaDataValue(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE),
                wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(cn.getTargetNode().getIdentifier()),
                cn.getInput().getIdentifier(), cn.getInput().getMetaDataValue(LoopComponentConstants.META_KEY_LOOP_ENDPOINT_TYPE));
            String edgeKey = WorkflowGraph.createEdgeKey(edge);
            if (!workflowGraphEdges.containsKey(edgeKey)) {
                workflowGraphEdges.put(edgeKey, new HashSet<WorkflowGraphEdge>());
            }
            workflowGraphEdges.get(edgeKey).add(edge);
        }
        return new WorkflowGraph(workflowGraphNodes, workflowGraphEdges);
    }

    private boolean isDrivingFaultTolerantNode(WorkflowNode wn) {
        ConfigurationDescription configDesc = wn.getComponentDescription().getConfigurationDescription();
        LoopBehaviorInCaseOfFailure behavior = LoopBehaviorInCaseOfFailure
            .fromString(configDesc.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE));
        return behavior.equals(LoopBehaviorInCaseOfFailure.Discard);
    }

    private ComponentExecutionContext createComponentExecutionContext(WorkflowNode wfNode, WorkflowGraph workflowGraph) {
        String compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(wfNode.getIdentifier());
        WorkflowDescription workflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription();
        ComponentExecutionContextBuilder builder = new ComponentExecutionContextBuilder();
        builder.setExecutionIdentifiers(compExeId, wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
        builder.setInstanceNames(wfNode.getName(), wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName());
        builder.setNodes(wfStateMachineCtx.getWorkflowExecutionContext().getNodeId(),
            wfStateMachineCtx.getWorkflowExecutionContext().getDefaultStorageNodeId());
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
                        wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(cn.getTargetNode().getIdentifier()),
                        cn.getTargetNode().getName(), cn.getTargetNode().getComponentDescription().getNode());
                if (!endpointDatumRecipients.containsKey(cn.getOutput().getName())) {
                    endpointDatumRecipients.put(cn.getOutput().getName(), new ArrayList<EndpointDatumRecipient>());
                }
                endpointDatumRecipients.get(cn.getOutput().getName()).add(endpointDatumRecipient);
            }
        }
        builder.setPredecessorAndSuccessorInformation(isConnectedToEndpointDatumSenders, endpointDatumRecipients);
        builder.setWorkflowGraph(workflowGraph);
        Long compInstanceDmId = wfStateMachineCtx.getWorkflowExecutionStorageBridge().getComponentInstanceDataManamagementId(
            wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(wfNode.getIdentifier()));
        Map<String, Long> inputDataManagementIds = wfStateMachineCtx.getWorkflowExecutionStorageBridge()
            .getInputInstanceDataManamagementIds(wfStateMachineCtx.getWorkflowExecutionContext()
                .getCompExeIdByWfNodeId(wfNode.getIdentifier()));
        Map<String, Long> outputDataManagementIds = wfStateMachineCtx.getWorkflowExecutionStorageBridge()
            .getOutputInstanceDataManamagementIds(wfStateMachineCtx.getWorkflowExecutionContext()
                .getCompExeIdByWfNodeId(wfNode.getIdentifier()));
        builder.setDataManagementIds(wfStateMachineCtx.getWorkflowExecutionStorageBridge().getWorkflowInstanceDataManamagementId(),
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

            ParallelComponentCaller ppc = new ParallelComponentCaller(componentNodeIds.keySet(),
                wfStateMachineCtx.getWorkflowExecutionContext()) {

                @Override
                public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                    sendComponentStateFailed(compExeId);
                }

                @Override
                public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                    componentExecutionService.start(compExeId, componentNodeIds.get(compExeId));
                    wfStateMachineCtx.getComponentLostWatcher().announceComponentHeartbeat(compExeId);
                }

                @Override
                public String getMethodToCallAsString() {
                    return "start";
                }
            }; // TODO direct method invocation causes an false positive checkstyle error
            Throwable throwable = ppc.callParallelAndWait();

            if (throwable == null) {
                synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
                    heartbeatFuture = threadPool.scheduleAtFixedRate(wfStateMachineCtx.getComponentLostWatcher(),
                        ComponentLostWatcher.DEFAULT_MAX_HEA7RTBEAT_INTERVAL_MSEC);
                }
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_ATTEMPT_SUCCESSFUL));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_ATTEMPT_FAILED, throwable));
            }

            if (wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes().isEmpty()) {
                onComponentStatesChangedCompletelyToFinished();
                onComponentStatesChangedCompletelyToAnyFinalState();
                onLastConsoleRowsReceived();
                disposedComponentStateLatch.countDown();
            }
        }
    }

    /**
     * Pauses the workflow.
     * 
     * @author Doreen Seider
     */
    private final class AsyncPauseTask implements Runnable {

        private static final int WAIT_INTERVAL_PAUSE_HRS = 10;

        @Override
        @TaskDescription("Pause workflow")
        public void run() {

            ParallelComponentCaller ppc = new ParallelComponentCaller(getComponentsToConsider(true),
                wfStateMachineCtx.getWorkflowExecutionContext()) {

                @Override
                public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                    pausedComonentStateLatch.countDown();
                    sendComponentStateFailed(compExeId);
                }

                @Override
                public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                    componentExecutionService.pause(compExeId, componentNodeIds.get(compExeId));
                }

                @Override
                public String getMethodToCallAsString() {
                    return "pause";
                }
            }; // TODO direct method invocation causes an false positive checkstyle error
            Throwable throwable = ppc.callParallelAndWait();

            try {
                // TODO review waiting time (note: when pausing a workflow, the components' runs are not interrupted or paused)
                boolean timeNotElapsed = pausedComonentStateLatch.await(WAIT_INTERVAL_PAUSE_HRS, TimeUnit.HOURS);
                if (!timeNotElapsed) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED,
                        new WorkflowExecutionException(
                            StringUtils.format("Waiting for workflow %s to pause failed" + CAUSE_WAITING_TIME_ELAPSED_HRS,
                                wfNameAndIdMessagePart, WAIT_INTERVAL_PAUSE_HRS))));
                    return;
                }
            } catch (InterruptedException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED,
                    new WorkflowExecutionException(StringUtils.format("Waiting for components to pause (workflow %s) was interrupted",
                        wfNameAndIdMessagePart), e)));
                return;
            }
            if (throwable == null) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_SUCCESSFUL));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_ATTEMPT_FAILED, throwable));
            }
        }
        
    }

    /**
     * Resumes the workflow.
     * 
     * @author Doreen Seider
     */
    private final class AsyncResumeTask implements Runnable {

        private static final int WAIT_INTERVAL_CANCEL_SEC = 60;

        @Override
        @TaskDescription("Resume workflow")
        public void run() {
            ParallelComponentCaller ppc = new ParallelComponentCaller(getComponentsToConsider(true),
                wfStateMachineCtx.getWorkflowExecutionContext()) {

                @Override
                public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                    resumedComonentStateLatch.countDown();
                    sendComponentStateFailed(compExeId);
                }

                @Override
                public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                    componentExecutionService.resume(compExeId, componentNodeIds.get(compExeId));
                }

                @Override
                public String getMethodToCallAsString() {
                    return "resume";
                }
            }; // TODO direct method invocation causes an false positive checkstyle error
            Throwable throwable = ppc.callParallelAndWait();

            try {
                boolean timeNotElapsed = resumedComonentStateLatch.await(WAIT_INTERVAL_CANCEL_SEC, TimeUnit.SECONDS);
                if (!timeNotElapsed) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED,
                        new WorkflowExecutionException(
                            StringUtils.format("Waiting for workflow %s to resume failed" + CAUSE_WAITING_TIME_ELAPSED_SEC,
                                wfNameAndIdMessagePart, WAIT_INTERVAL_CANCEL_SEC))));
                    return;
                }
            } catch (InterruptedException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED,
                    new WorkflowExecutionException(StringUtils.format("Waiting for components to resume (workflow %s) was interrupted",
                        wfNameAndIdMessagePart), e)));
                return;
            }
            if (throwable == null) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_SUCCESSFUL));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_ATTEMPT_FAILED, throwable));
            }
        }
    }

    /**
     * Cancels the workflow.
     * 
     * @author Doreen Seider
     */
    private final class AsyncCancelTask implements Runnable {

        private static final int WAIT_INTERVAL_CANCEL_SEC = 60;
        
        private final Future<?> future;

        protected AsyncCancelTask(Future<?> future) {
            this.future = future;
        }

        @Override
        @TaskDescription("Cancel workflow")
        public void run() {
            if (future != null) {
                try {
                    future.get(WAIT_INTERVAL_CANCEL_SEC, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                    return;
                } catch (TimeoutException | InterruptedException e) {
                    future.cancel(true);
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                    return;
                }
            }

            Throwable throwable = null;
            if (!componentNodeIds.isEmpty()) {
                throwable = new ParallelComponentCaller(getComponentsToConsider(true), wfStateMachineCtx.getWorkflowExecutionContext()) {

                    @Override
                    public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                        componentExecutionService.cancel(compExeId, componentNodeIds.get(compExeId));
                    }

                    @Override
                    public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                        wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().accounceComponentInAnyFinalState(compExeId);
                        wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().announceLastConsoleRow(compExeId);
                        sendComponentStateFailed(compExeId);
                    }

                    @Override
                    public String getMethodToCallAsString() {
                        return "cancel";
                    }
                }.callParallelAndWait();

                int compsNotInitCount = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes().size()
                    - componentNodeIds.size();
                for (int i = 0; i < compsNotInitCount; i++) {
                    String pseudoCompExeId = String.valueOf(i);
                    wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().accounceComponentInAnyFinalState(pseudoCompExeId);
                    wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().announceLastConsoleRow(pseudoCompExeId);
                }
                try {
                    boolean timeNotElapsed = workflowTerminatedLatch.await(WAIT_INTERVAL_TERMINATED_SEC, TimeUnit.SECONDS);
                    if (!timeNotElapsed) {
                        postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED,
                            new WorkflowExecutionException(
                                StringUtils.format("Waiting for workflow %s to cancel failed" + CAUSE_WAITING_TIME_ELAPSED_SEC,
                                    wfNameAndIdMessagePart, WAIT_INTERVAL_CANCEL_SEC))));
                        return;
                    }
                } catch (InterruptedException e) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED,
                        new WorkflowExecutionException(StringUtils.format("Waiting for components to cancel (workflow %s) was interrupted",
                            wfNameAndIdMessagePart), e)));
                    return;
                }
            }
            try {
                flushAndDisposeComponentLogFiles();
                if (getState() == WorkflowState.CANCELING_AFTER_FAILED || throwable != null) {
                    wfStateMachineCtx.getWorkflowExecutionStorageBridge().setWorkflowExecutionFinished(FinalWorkflowState.FAILED);
                } else if (getState() == WorkflowState.CANCELING) {
                    wfStateMachineCtx.getWorkflowExecutionStorageBridge().setWorkflowExecutionFinished(FinalWorkflowState.CANCELLED);
                }
            } catch (WorkflowExecutionException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, e));
                return;
            }
            if (throwable == null) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_SUCCESSFUL));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_ATTEMPT_FAILED, throwable));
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
                boolean timeNotElapsed = workflowTerminatedLatch.await(WAIT_INTERVAL_TERMINATED_SEC, TimeUnit.SECONDS);
                if (timeNotElapsed) {
                    workflowExecutionFinished();
                } else {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISH_ATTEMPT_FAILED,
                        new WorkflowExecutionException(StringUtils.format("Waiting for workflow %s to finish failed",
                            wfNameAndIdMessagePart) + " cause: waiting time elapsed")));
                }
            } catch (InterruptedException e) {
                LOG.error(StringUtils.format("Waiting for workflow %s to finish failed", wfNameAndIdMessagePart)
                    + "cause: waiting interrupted", e);
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISH_ATTEMPT_FAILED, e));
            }
        }
    }

    private void workflowExecutionFinished() {
        if (!wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes().isEmpty()) {
            flushAndDisposeComponentLogFiles();
        }
        try {
            wfStateMachineCtx.getWorkflowExecutionStorageBridge().setWorkflowExecutionFinished(FinalWorkflowState.FINISHED);
            postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISHED));
        } catch (WorkflowExecutionException e) {
            postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.FINISH_ATTEMPT_FAILED, e));
        }
    }

    private void flushAndDisposeComponentLogFiles() {
        wfStateMachineCtx.getComponentsConsoleLogFileWriter().addWorkflowConsoleRow(createConsoleRowForWorkflowLifeCycleEvent(
            WorkflowLifecyleEventType.WORKFLOW_LOG_FINISHED.name()));
        wfStateMachineCtx.getComponentsConsoleLogFileWriter().flushAndDisposeLogFiles();
    }

    /**
     * Disposes the workflow.
     * 
     * @author Doreen Seider
     */
    private final class AsyncDisposeTask implements Runnable {

        private static final int WAIT_INTERVAL_DISPOSE_SEC = 60;

        @Override
        @TaskDescription("Dispose workflow")
        public void run() {
            notificationService.send(WorkflowConstants.STATE_DISPOSED_NOTIFICATION_ID, wfStateMachineCtx.getWorkflowExecutionContext()
                .getExecutionIdentifier());
            ParallelComponentCaller ppc = new ParallelComponentCaller(getComponentsToConsider(false, true), 
                wfStateMachineCtx.getWorkflowExecutionContext()) {

                @Override
                public void onErrorInSingleComponentCall(String compExeId, Throwable t) {
                    wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().announceComponentState(compExeId,
                        ComponentState.DISPOSED);
                }

                @Override
                public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                    componentExecutionService.dispose(compExeId, componentNodeIds.get(compExeId));
                }

                @Override
                public String getMethodToCallAsString() {
                    return "dispose";
                }
            }; // TODO direct method invocation causes an false positive checkstyle error
            Throwable throwable = ppc.callParallelAndWait();

            try {
                boolean timeNotElapsed = disposedComponentStateLatch.await(WAIT_INTERVAL_DISPOSE_SEC, TimeUnit.SECONDS);
                if (!timeNotElapsed) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED,
                        new WorkflowExecutionException(
                            StringUtils.format("Waiting for workflow %s to dispose failed" + CAUSE_WAITING_TIME_ELAPSED_SEC,
                                wfNameAndIdMessagePart, WAIT_INTERVAL_DISPOSE_SEC))));
                }
            } catch (InterruptedException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED,
                    new WorkflowExecutionException(StringUtils.format("Waiting for components to dispose (workflow %s) was interrupted",
                        wfNameAndIdMessagePart), e)));
                return;
            }
            if (throwable == null) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_SUCCESSFUL));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_ATTEMPT_FAILED, throwable));
            }
        }
    }

    private void disposeNotificationBuffers() {
        notificationService.removePublisher(WorkflowConstants.STATE_NOTIFICATION_ID
            + wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
        notificationService.removePublisher(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString()
            + ConsoleRow.NOTIFICATION_SUFFIX);
        notificationService.removePublisher(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString()
            + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX);
        for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
            String compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNodeId(wfNode.getIdentifier());
            notificationService.removePublisher(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId);
            notificationService.removePublisher(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId);
        }
    }
    
    private Set<String> getComponentsToConsider(boolean ignoreCompsInFinalState, boolean ignoreDisposedComps) {
        
        Set<String> compsToConsider = new HashSet<>(componentNodeIds.keySet());
        if (ignoreCompsInFinalState) {
            compsToConsider.removeAll(wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().getComponentsInFinalState());
        } else if (ignoreDisposedComps) {
            compsToConsider.removeAll(wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().getDisposedComponents());
        }
        return compsToConsider;
    }
    
    private String createMessageListingComponents(Set<String> compExeIds) {
        String message = "";
        for (String compExeId : compExeIds) {
            if (!message.isEmpty()) {
                message += ", ";
            }
            message += StringUtils.format("'%s' (%s) at %s", componentInstanceNames.get(compExeId),
                compExeId, componentNodeIds.get(compExeId));
        }
        return message;
    }
    
    private void sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType type) {
        ConsoleRow consoleRow = createConsoleRowForWorkflowLifeCycleEvent(type.name());
        sendConsoleRowAsNotification(consoleRow);
    }

    private void sendNewWorkflowStateAsConsoleRow(WorkflowState newState) {
        // send a LIFE_CYCLE_EVENT of subtype NEW_STATE with the new state's enum name attached
        String payload = StringUtils.escapeAndConcat(ConsoleRow.WorkflowLifecyleEventType.NEW_STATE.name(), newState.name());
        sendConsoleRowAsNotification(createConsoleRowForWorkflowLifeCycleEvent(payload));
    }

    private void sendConsoleRowAsNotification(ConsoleRow consoleRow) {
        notificationService.send(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString()
            + ConsoleRow.NOTIFICATION_SUFFIX, consoleRow);
    }
    
    private ConsoleRow createConsoleRowForWorkflowLifeCycleEvent(String payload) {
        return createConsoleRow(ConsoleRow.Type.LIFE_CYCLE_EVENT, payload, "", "");
    }
    
    private ConsoleRow createConsoleRow(Type type, String payload, String compExeId, String compInstanceName) {
        ConsoleRowBuilder consoleRowBuilder = new ConsoleRowBuilder();
        consoleRowBuilder.setExecutionIdentifiers(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier(), compExeId)
            .setInstanceNames(wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName(), compInstanceName)
            .setType(type)
            .setPayload(payload);
        return consoleRowBuilder.build();
    }

    private void storeAndSendErrorLogMessage(Type type, String message, String compExeId, String compInstanceName) {
        ConsoleRow consoleRow = createConsoleRow(type, message, compExeId, compInstanceName);
        wfStateMachineCtx.getComponentsConsoleLogFileWriter().addWorkflowConsoleRow(consoleRow);
        notificationService.send(wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()
            + wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getIdString() + ConsoleRow.NOTIFICATION_SUFFIX, consoleRow);
    }
    
    @Override
    public void onComponentStatesChangedCompletelyToPrepared() {
        postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_SUCCESSFUL));
    }

    @Override
    public void onComponentStatesChangedCompletelyToPaused() {
        pausedComonentStateLatch.countDown();
    }

    @Override
    public void onComponentStatesChangedCompletelyToResumed() {
        resumedComonentStateLatch.countDown();
    }

    @Override
    public void onComponentStatesChangedCompletelyToFinished() {
        postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.ON_COMPONENTS_FINISHED));
    }

    @Override
    public void onComponentStatesChangedCompletelyToDisposed() {
        disposedComponentStateLatch.countDown();
    }

    @Override
    public void onComponentStatesChangedCompletelyToAnyFinalState() {
        synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
            if (heartbeatFuture != null && !heartbeatFuture.isCancelled()) {
                heartbeatFuture.cancel(false);
            }
        }
        workflowTerminatedLatch.countDown();
    }

    @Override
    public void onLastConsoleRowsReceived() {
        sendLifeCycleEventAsConsoleRow(ConsoleRow.WorkflowLifecyleEventType.WORKFLOW_FINISHED);
        workflowTerminatedLatch.countDown();
    }
    
    private Set<String> getComponentsToConsider(boolean ignoreCompsInFinalState) {
        return getComponentsToConsider(ignoreCompsInFinalState, true);
    }
    
    private void sendComponentStateCanceled(String compExeId) {
        notificationService.send(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, ComponentState.CANCELED.name());
    }
    
    private void sendComponentStateFailed(String compExeId) {
        notificationService.send(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, ComponentState.FAILED.name());
    }
    
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }
    
    protected void bindComponentExecutionService(ComponentExecutionService newService) {
        componentExecutionService = newService;
    }

    protected void bindWorkflowExecutionStatsService(WorkflowExecutionStatsService newService) {
        wfExeStatsService = newService;
    }

    protected void bindDistributedNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    @Override
    public void onComponentsLost(Set<String> compExeIdsLost) {
        for (String compExeIdLost : compExeIdsLost) {
            sendComponentStateFailed(compExeIdLost);
        }
        postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.COMPONENT_HEARTBEAT_LOST,
            new WorkflowExecutionException(StringUtils.format("Component(s) not reachable (anymore): "
                + createMessageListingComponents(compExeIdsLost)))));
    }
    
    /**
     * Processes {@link WorkflowStateMachineEventType}s.
     * 
     * @author Doreen Seider
     */
    private interface EventProcessor {
        WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event);
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class StartRequestedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.PREPARING)) {
                state = WorkflowState.PREPARING;
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
    private class PrepareAttemptSuccessfulEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.STARTING)) {
                currentTask = null;
                startAsync();
                state = WorkflowState.STARTING;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class StartAttemptSuccessfulEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.RUNNING)) {
                currentTask = null;
                state = WorkflowState.RUNNING;
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
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.PAUSING)) {
                wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().enablePausedComponentStateVerification();
                pausedComonentStateLatch = new CountDownLatch(1);
                pauseAsync();
                state = WorkflowState.PAUSING;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class PauseAttemptSuccessfulEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.PAUSED)) {
                state = WorkflowState.PAUSED;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResumeRequestedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.RESUMING)) {
                wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().enableResumedComponentStateVerification();
                resumedComonentStateLatch = new CountDownLatch(1);
                resumeAsync();
                state = WorkflowState.RESUMING;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ResumeAttemptSuccessfulEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.RUNNING)) {
                state = WorkflowState.RUNNING;
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
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.CANCELING)) {
                state = WorkflowState.CANCELING;
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
    private class CancelAfterFailedRequestedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            handleFailure(event);
            if (currentState != WorkflowState.CANCELING && currentState != WorkflowState.CANCELING_AFTER_FAILED) {
                cancelAsync();
            }
            return WorkflowState.CANCELING_AFTER_FAILED;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class CancelAttemptSuccessufulEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (currentState == WorkflowState.CANCELING) {
                if (checkStateChange(currentState, WorkflowState.CANCELLED)) {
                    state = WorkflowState.CANCELLED;
                }
            } else if (currentState == WorkflowState.CANCELING_AFTER_FAILED) {
                if (checkStateChange(currentState, WorkflowState.FAILED)) {
                    state = WorkflowState.FAILED;
                }
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
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.DISPOSING)) {
                state = WorkflowState.DISPOSING;
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
    private class DisposeAttemptSuccessfulOrFailedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.DISPOSED)) {
                currentTask = null;
                state = WorkflowState.DISPOSED;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class OnComponentsFinishedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            if (checkStateChange(currentState, WorkflowState.FINISHED)) {
                waitForFinishAsync();
            }
            return currentState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class FinishedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.FINISHED)) {
                state = WorkflowState.FINISHED;
            }
            return state;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class PrepareStartPauseResumeFinishTimelineAttemptFailedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            currentTask = null;
            if (event.getThrowable() != null) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED,
                    event.getThrowable()));
            } else {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED,
                    event.getErrorId(), event.getErrorMessage(), event.getComponentExecutionId()));
            }
            return currentState;
        }
    }
    
    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class ComponentHeartbeatLostEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            currentTask = null;
            wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier().declareLostComponentsAsBeingInFinalStateAndDisposed();
            postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_COMPONENT_LOST_REQUESTED,
                event.getThrowable()));
            return currentState;
        }
    }

    /**
     * Specific implementation of {@link EventProcessor}.
     * 
     * @author Doreen Seider
     */
    private class CancelAttemptFailedEventProcessor implements EventProcessor {
        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, WorkflowState.FAILED)) {
                if (event.getThrowable() != null) {
                    LOG.error(StringUtils.format("Failed to cancel workflow %s", wfNameAndIdMessagePart), event.getThrowable());
                }
                flushAndDisposeComponentLogFiles();
                try {
                    wfStateMachineCtx.getWorkflowExecutionStorageBridge().setWorkflowExecutionFinished(FinalWorkflowState.FAILED);
                } catch (WorkflowExecutionException e) {
                    LOG.error(StringUtils.format("Failed to set final state of workflow %s (%s)",
                        wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName(),
                        wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()));
                }
                state = WorkflowState.FAILED;
            }
            return state;
        }
    }
    
}


