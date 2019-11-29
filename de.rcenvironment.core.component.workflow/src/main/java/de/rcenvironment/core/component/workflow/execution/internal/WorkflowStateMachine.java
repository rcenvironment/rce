/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.LoopComponentConstants;
import de.rcenvironment.core.component.api.LoopComponentConstants.LoopBehaviorInCaseOfFailure;
import de.rcenvironment.core.component.execution.api.ComponentControllerRoutingMap;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContextBuilder;
import de.rcenvironment.core.component.execution.api.ComponentExecutionException;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.ComponentExecutionService;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRow.Type;
import de.rcenvironment.core.component.execution.api.ConsoleRow.WorkflowLifecyleEventType;
import de.rcenvironment.core.component.execution.api.ConsoleRowBuilder;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.execution.api.EndpointDatumDispatchService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.WorkflowGraph;
import de.rcenvironment.core.component.execution.api.WorkflowGraphEdge;
import de.rcenvironment.core.component.execution.api.WorkflowGraphNode;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipient;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDatumRecipientFactory;
import de.rcenvironment.core.component.model.endpoint.api.EndpointDescription;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.internal.WorkflowExecutionStorageBridge.DataManagementIdsHolder;
import de.rcenvironment.core.component.workflow.model.api.Connection;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.datamodel.api.FinalWorkflowState;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.LogUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.AbstractFixedTransitionsStateMachine;
import de.rcenvironment.core.utils.incubator.AbstractStateMachine;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.core.utils.incubator.StateChangeException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Workflow-specific implementation of {@link AbstractStateMachine}.
 * 
 * Note: I wouldn't trust the workflow state transition graph blindly but I consider it as sufficient for now. I'm not happy with the
 * communication to the components and the resulting control flow including waiting for certain callbacks, etc. See note in
 * {@link ComponentStatesChangedEntirelyListener}. --seid_do
 * 
 * @author Doreen Seider
 * @author Robert Mischke
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
        { WorkflowState.PREPARING, WorkflowState.CANCELING },
        // TODO review: allow STARTING -> CANCELING, too?
        { WorkflowState.RUNNING, WorkflowState.CANCELING },
        { WorkflowState.PAUSED, WorkflowState.CANCELING },
        { WorkflowState.CANCELING, WorkflowState.CANCELLED },
        { WorkflowState.CANCELLED, WorkflowState.DISPOSING },
        // failure
        { WorkflowState.PREPARING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.STARTING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.RUNNING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.PAUSING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.RESUMING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.CANCELING, WorkflowState.CANCELING_AFTER_FAILED },
        { WorkflowState.CANCELING, WorkflowState.FAILED },
        { WorkflowState.CANCELING_AFTER_FAILED, WorkflowState.FAILED },
        { WorkflowState.RUNNING, WorkflowState.CANCELING_AFTER_RESULTS_REJECTED },
        { WorkflowState.CANCELING_AFTER_RESULTS_REJECTED, WorkflowState.RESULTS_REJECTED },
        { WorkflowState.RESULTS_REJECTED, WorkflowState.DISPOSING },
        { WorkflowState.FAILED, WorkflowState.DISPOSING }
    };

    // set visibility to protected for test purposes
    protected final Map<WorkflowStateMachineEventType, EventProcessor> eventProcessors = new HashMap<>();

    private final CommunicationService communicationService;

    private final DistributedNotificationService notificationService;

    private final ComponentExecutionService componentExecutionService;

    private final WorkflowExecutionStatsService wfExeStatsService;

    private final EndpointDatumDispatchService endpointDatumDispatchService;

    private String wfNameAndIdMessagePart;

    // a map of reliable RPC streams or simple network ids for executing method calls
    private final ComponentControllerRoutingMap componentControllerCommandDestinations = new ComponentControllerRoutingMap();

    private final AsyncTaskService threadPool = ConcurrencyUtils.getAsyncTaskService();

    private WorkflowStateMachineContext wfStateMachineCtx;

    private WorkflowDescription fullWorkflowDescription;

    private Map<String, String> executionAuthTokens;

    private ScheduledFuture<?> heartbeatFuture;

    private ScheduledFuture<?> compRestartDetectionFuture;

    // map from Component Executor identifier to corresponding Node ID
    private final Map<String, LogicalNodeId> componentNodeIds = Collections
        .synchronizedMap(new HashMap<String, LogicalNodeId>());

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
        communicationService = null;
        notificationService = null;
        componentExecutionService = null;
        wfExeStatsService = null;
        endpointDatumDispatchService = null;
    }

    public WorkflowStateMachine(WorkflowStateMachineContext wfStateMachineCtx) {
        super(WorkflowState.INIT, VALID_WORKFLOW_STATE_TRANSITIONS);
        this.wfStateMachineCtx = wfStateMachineCtx;
        this.fullWorkflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().clone();
        WorkflowExecutionUtils
            .removeDisabledWorkflowNodesWithoutNotify(wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription());
        this.wfNameAndIdMessagePart = StringUtils.format("'%s' (%s)", wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName(),
            wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());

        final ServiceRegistryAccess serviceRegistryAccess = wfStateMachineCtx.getServiceRegistryAccess();

        communicationService = serviceRegistryAccess.getService(CommunicationService.class);
        notificationService = serviceRegistryAccess.getService(DistributedNotificationService.class);
        componentExecutionService = serviceRegistryAccess.getService(ComponentExecutionService.class);
        wfExeStatsService = serviceRegistryAccess.getService(WorkflowExecutionStatsService.class);
        endpointDatumDispatchService = serviceRegistryAccess.getService(EndpointDatumDispatchService.class);

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
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_REQUESTED, new CancelRequestedEventProcessor(WorkflowState.CANCELING));
        CancelRequestedEventProcessor cancelRequestedEventProcessor =
            new CancelRequestedEventProcessor(WorkflowState.CANCELING_AFTER_FAILED);
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_AFTER_COMPONENT_LOST_REQUESTED, cancelRequestedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED, cancelRequestedEventProcessor);
        eventProcessors.put(WorkflowStateMachineEventType.CANCEL_AFTER_RESULTS_REJECTED_REQUESTED,
            new CancelRequestedEventProcessor(WorkflowState.CANCELING_AFTER_RESULTS_REJECTED));
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
        eventProcessors.put(WorkflowStateMachineEventType.VERIFICATION_ATTEMPT_FAILED, variousAttemptsFailedEventProcessor);
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
            final LogicalNodeId componentNode = componentNodeIds.get(event.getComponentExecutionId());
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
        LOG.debug(StringUtils.format("Ignored workflow state change request for workflow %s as it would cause an invalid"
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
        if (WorkflowConstants.FINAL_WORKFLOW_STATES.contains(newState)) {
            wfExeStatsService.addStatsAtWorkflowTermination(wfStateMachineCtx.getWorkflowExecutionContext(), newState);
            // TODO use better synchronization object
            synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
                if (heartbeatFuture != null && !heartbeatFuture.isCancelled()) {
                    heartbeatFuture.cancel(false);
                }
                if (compRestartDetectionFuture != null && !compRestartDetectionFuture.isCancelled()) {
                    compRestartDetectionFuture.cancel(false);
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

            final WorkflowExecutionContext workflowExecutionContext = wfStateMachineCtx.getWorkflowExecutionContext();
            wfExeStatsService.addStatsAtWorkflowStart(workflowExecutionContext);

            notificationService.send(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID,
                wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
            initializeNotificationBuffers();

            initializeConsoleLogWriting();

            DataManagementIdsHolder dmIds;
            try {
                dmIds = wfStateMachineCtx.getWorkflowExecutionStorageBridge().addWorkflowExecution(
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

            final Map<WorkflowNodeIdentifier, ComponentExecutionContext> compExeCtxts;
            try {
                compExeCtxts = createComponentExecutionContexts(dmIds);
                checkForUnreachableComponentNodes(compExeCtxts);
                wfStateMachineCtx.getNodeRestartWatcher().initialize(compExeCtxts.values());
            } catch (WorkflowExecutionException e) {
                postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED, e));
                return;
            }

            CallablesGroup<Throwable> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Throwable.class);

            final Long referenceTimestamp = System.currentTimeMillis();

            ComponentControllerRoutingMap endpointDatumForwardingMap = new ComponentControllerRoutingMap();

            for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
                final WorkflowNode finalWfNode = wfNode;
                final WorkflowNodeIdentifier workflowNodeId = wfNode.getIdentifierAsObject();
                final ComponentExecutionContext compExeCtx = compExeCtxts.get(workflowNodeId);
                callablesGroup.add(new Callable<Throwable>() {

                    @Override
                    @TaskDescription("Create component execution controller and perform prepare")
                    public Exception call() {
                        try {
                            LOG.debug("Spawning component controller for workflow node id " + workflowNodeId
                                + " mapped to component execution id " + compExeCtx.getExecutionIdentifier());
                            // create reliable RPC streams for performing method calls on this component controller
                            // TODO (p1) exclude local controllers, as rRPC will be ignored for them anyway
                            // TODO (p2) optimize this by only creating one stream per remote node
                            final ReliableRPCStreamHandle reliableWfCtrlToCompCtrlRPCStream =
                                communicationService.createReliableRPCStream(compExeCtx.getNodeId());
                            // trigger creation of the remote component controller
                            String compExeId = componentExecutionService.init(compExeCtx,
                                executionAuthTokens.get(finalWfNode.getIdentifierAsObject().toString()), referenceTimestamp);
                            // store node id and rRPC stream handle
                            componentNodeIds.put(compExeId, compExeCtx.getNodeId());
                            componentControllerCommandDestinations.setNetworkDestinationForComponentController(compExeId,
                                reliableWfCtrlToCompCtrlRPCStream);
                            // create rRPC streams for forwarding endpoint datums to component controllers if necessary;
                            // rRPC streams are fairly "cheap" if they are never used, so just create one each in advance
                            final ReliableRPCStreamHandle endpointForwardingStream =
                                communicationService.createReliableRPCStream(compExeCtx.getNodeId());
                            endpointDatumForwardingMap.setNetworkDestinationForComponentController(compExeId, endpointForwardingStream);
                            // general setup
                            componentInstanceNames.put(compExeId, compExeCtx.getInstanceName());
                            initializeComponentConsoleLogWriting(compExeId);
                            componentExecutionService.prepare(compExeId, compExeCtx.getNodeId());
                            LOG.debug(StringUtils.format("Created component '%s' (%s) on node %s",
                                compExeCtx.getInstanceName(), compExeId, compExeCtx.getNodeId()));
                        } catch (RemoteOperationException | RuntimeException | ExecutionControllerException
                            | ComponentExecutionException e) {
                            // wrap the caught exception into a new one with a more user-friendly message
                            final String errorMessage = StringUtils.format(
                                "Failed to initialize component execution of '%s' on %s: %s", compExeCtx.getInstanceName(),
                                compExeCtx.getNodeId(), e.toString());
                            LOG.debug(errorMessage);
                            return new ComponentExecutionException(errorMessage); // TODO is this the most fitting exception type?
                        }
                        return null;
                    }
                }, "Prepare component: " + compExeCtx.getExecutionIdentifier());
            }

            // set the forwarding map so the dispatch service will know how to handle forwarding requests
            endpointDatumDispatchService.registerComponentControllerForwardingMap(
                workflowExecutionContext.getWorkflowExecutionHandle().getIdentifier(), endpointDatumForwardingMap);

            List<Throwable> throwables = callablesGroup.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    // should never happen
                }
            });

            for (Throwable t : throwables) {
                if (t != null) {
                    if (t instanceof ComponentExecutionException) {
                        // log without stacktrace
                        LOG.error(StringUtils.format("Failed to prepare workflow %s: %s", wfNameAndIdMessagePart, t.toString()));
                    } else {
                        LOG.error(StringUtils.format("Failed to prepare workflow %s", wfNameAndIdMessagePart), t);
                    }
                }
            }

            for (Throwable t : throwables) {
                if (t != null) {
                    postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PREPARE_ATTEMPT_FAILED,
                        new Throwable("Failed to prepare workflow: " + t.getMessage())));
                    return;
                }
            }

            LOG.debug(StringUtils.format("Workflow %s is prepared (%d component(s))", wfNameAndIdMessagePart, compExeCtxts.size()));
        }
    }

    // TODO only the values of the map given as parameter is needed in the method; therefore only these should be provided and not the
    // complete map
    private void checkForUnreachableComponentNodes(Map<WorkflowNodeIdentifier, ComponentExecutionContext> compExeCtxts)
        throws WorkflowExecutionException {

        Map<String, ComponentExecutionContext> notReachableCompExeIds = new HashMap<>();

        Set<LogicalNodeId> reachableNodes = communicationService.getReachableLogicalNodes();
        for (ComponentExecutionContext compExeCtx : compExeCtxts.values()) {
            // stored for logging purposes, see createMessageListingComponents; should be improved
            componentInstanceNames.put(compExeCtx.getExecutionIdentifier(), compExeCtx.getInstanceName());
            componentNodeIds.put(compExeCtx.getExecutionIdentifier(), compExeCtx.getNodeId());
            // TODO (p2) simply checking the reachability of the equivalent default logical node id for now; improve when needed
            if (!reachableNodes.contains(compExeCtx.getNodeId().convertToDefaultLogicalNodeId())) {
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
            // TODO (p3) this seems odd: the maps are always cleared here, only to be filled again later? -- misc_ro
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
        notificationService.setBufferSize(ConsoleRowUtils.composeConsoleNotificationId(wfStateMachineCtx.getWorkflowExecutionContext()
            .getNodeId(), wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()), bufferSize);
        notificationService.setBufferSize(WorkflowConstants.STATE_NOTIFICATION_ID
            + wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier(), 1);
        notificationService.setBufferSize(StringUtils.format(ComponentConstants.NOTIFICATION_ID_PREFIX_PROCESSED_INPUT + "%s:%s",
            wfStateMachineCtx.getWorkflowExecutionContext().getNodeId().getLogicalNodeIdString(),
            wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()), bufferSize);
        for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
            ComponentExecutionIdentifier compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wfNode);
            // set to 3 as the state before the component is disposing->disposed must be
            // accessible by the GUI
            notificationService.setBufferSize(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId.toString(), 3);
            notificationService.setBufferSize(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId.toString(), 1);
        }
    }

    /**
     * Creates a {@link ComponentExecutionContext} for each Workflow Node in the Workflow Description.
     */
    private Map<WorkflowNodeIdentifier, ComponentExecutionContext> createComponentExecutionContexts(DataManagementIdsHolder dmIds)
        throws WorkflowExecutionException {
        WorkflowDescription workflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription();
        WorkflowGraph workflowGraph = createWorkflowGraph(workflowDescription);
        Map<WorkflowNodeIdentifier, ComponentExecutionContext> compExeCtxs = new HashMap<>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            // TODO why should the component execution context have access to the workflow graph?
            compExeCtxs.put(wfNode.getIdentifierAsObject(), createComponentExecutionContext(wfNode, workflowGraph, dmIds));
        }
        return compExeCtxs;
    }

    // The WorkflowGraph does only contain information about the execution locations indirectly via the ComponentExecutionIdentifiers. In
    // the beginning, these are not associate with concrete execution locations.
    private WorkflowGraph createWorkflowGraph(WorkflowDescription workflowDescription) throws WorkflowExecutionException {
        // map from a workflow node's component executor identifier to its WorkflowGraphNode
        Map<ComponentExecutionIdentifier, WorkflowGraphNode> workflowGraphNodes = new HashMap<>();
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            // TODO move the creation of these internal data structures inside the WorkflowGraph as soon as the WorkflowGraph has been moved
            // into the workflow package
            // map from an endpoint's identifier to its name
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
            ComponentExecutionIdentifier compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wn);
            workflowGraphNodes.put(compExeId, new WorkflowGraphNode(wn.getIdentifier(), compExeId, inputIds, outputIds, endpointNames,
                wn.getComponentDescription().getComponentInstallation().getComponentInterface().getIsLoopDriver(),
                isDrivingFaultTolerantNode(wn), wn.getName()));
        }
        Set<WorkflowGraphEdge> workflowGraphEdges = new HashSet<>();
        for (Connection cn : workflowDescription.getConnections()) {
            WorkflowGraphEdge edge = new WorkflowGraphEdge(
                wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(cn.getSourceNode()),
                cn.getOutput().getIdentifier(), cn.getOutput().getEndpointDefinition().getEndpointCharacter(),
                wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(cn.getTargetNode()),
                cn.getInput().getIdentifier(), cn.getInput().getEndpointDefinition().getEndpointCharacter());
            workflowGraphEdges.add(edge);
        }
        WorkflowGraph workflowGraph = new WorkflowGraph(workflowGraphNodes, workflowGraphEdges);
        validatedNestedLoopDriverConfiguration(workflowDescription, workflowGraph);
        return workflowGraph;
    }

    private void validatedNestedLoopDriverConfiguration(WorkflowDescription workflowDescription, WorkflowGraph workflowGraph)
        throws WorkflowExecutionException {
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            boolean isDriverComp = wn.getComponentDescription().getComponentInstallation().getComponentInterface().getIsLoopDriver();
            if (isDriverComp) {
                ComponentExecutionIdentifier compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wn);
                Boolean nestedLoop = Boolean.valueOf(wn.getConfigurationDescription()
                    .getConfigurationValue(LoopComponentConstants.CONFIG_KEY_IS_NESTED_LOOP));
                try {
                    if (nestedLoop && workflowGraph.getLoopDriver(compExeId) == null) {
                        storeAndSendErrorLogMessage(ConsoleRow.Type.WORKFLOW_ERROR,
                            StringUtils.format(
                                "Potential configuration error: '%s' is configured as a nested loop driver component but doesn't seem "
                                    + "to be part of a loop driven by an outer loop driver component",
                                wn.getComponentDescription().getName()),
                            "", "");
                    } else if (!nestedLoop && workflowGraph.getLoopDriver(compExeId) != null) {
                        storeAndSendErrorLogMessage(ConsoleRow.Type.WORKFLOW_ERROR,
                            StringUtils.format("Potential configuration error: '%s' is part of a loop driven by an outer loop driver "
                                + "component but is not configured as a nested loop driver component",
                                wn.getComponentDescription().getName()),
                            "", "");
                    }
                } catch (ComponentExecutionException e) {
                    throw new WorkflowExecutionException("Wokflow logic invalid", e);
                }
            }
        }
    }

    // TODO move this function inside the WorkflowGraph as soon as the WorkflowGraph has been moved into the workflow package
    private boolean isDrivingFaultTolerantNode(WorkflowNode wn) {
        ConfigurationDescription configDesc = wn.getComponentDescription().getConfigurationDescription();
        LoopBehaviorInCaseOfFailure behavior = LoopBehaviorInCaseOfFailure
            .fromString(configDesc.getConfigurationValue(LoopComponentConstants.CONFIG_KEY_LOOP_FAULT_TOLERANCE_COMP_FAILURE));
        return behavior.equals(LoopBehaviorInCaseOfFailure.Discard);
    }

    private ComponentExecutionContext createComponentExecutionContext(WorkflowNode wfNode, WorkflowGraph workflowGraph,
        DataManagementIdsHolder dmIds) throws WorkflowExecutionException {
        ComponentExecutionIdentifier compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wfNode);
        WorkflowDescription workflowDescription = wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription();
        ComponentExecutionContextBuilder builder = new ComponentExecutionContextBuilder();
        builder.setExecutionIdentifiers(compExeId.toString(), wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier());
        builder.setInstanceNames(wfNode.getName(), wfStateMachineCtx.getWorkflowExecutionContext().getInstanceName());
        builder.setComponentDescription(wfNode.getComponentDescription());

        // set actual locations (node ids) of controller and storage
        builder.setNodes(wfStateMachineCtx.getWorkflowExecutionContext().getNodeId(),
            wfStateMachineCtx.getWorkflowExecutionContext().getStorageNodeId());

        // does this wfNode has some predecessors?
        boolean isConnectedToEndpointDatumSenders = false;
        for (Connection cn : workflowDescription.getConnections()) {
            if (cn.getTargetNode().equals(wfNode)) {
                isConnectedToEndpointDatumSenders = true;
                break;
            }
        }
        // For each output of the wfNode, this map contains a list of EndpointDatumRecipients that are connected to this output.
        Map<String, List<EndpointDatumRecipient>> endpointDatumRecipients = new HashMap<>();
        for (Connection cn : workflowDescription.getConnections()) {
            if (cn.getSourceNode().equals(wfNode)) {
                EndpointDatumRecipient endpointDatumRecipient = EndpointDatumRecipientFactory
                    .createEndpointDatumRecipient(cn.getInput().getName(),
                        wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(cn.getTargetNode()).toString(),
                        cn.getTargetNode().getName(), cn.getTargetNode().getComponentDescription().getNode());
                if (!endpointDatumRecipients.containsKey(cn.getOutput().getName())) {
                    endpointDatumRecipients.put(cn.getOutput().getName(), new ArrayList<EndpointDatumRecipient>());
                }
                endpointDatumRecipients.get(cn.getOutput().getName()).add(endpointDatumRecipient);
            }
        }
        builder.setPredecessorAndSuccessorInformation(isConnectedToEndpointDatumSenders, endpointDatumRecipients);
        builder.setWorkflowGraph(workflowGraph);
        ComponentExecutionIdentifier cei = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wfNode);
        Long compInstanceDmId = dmIds.compInstDmIds.get(cei);
        Map<String, Long> inputDataManagementIds = dmIds.inputDmIds.get(cei);
        Map<String, Long> outputDataManagementIds = dmIds.outputDmIds.get(cei);
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
                    componentExecutionService.start(compExeId,
                        componentControllerCommandDestinations.getNetworkDestinationForComponentController(compExeId));
                    wfStateMachineCtx.getComponentLostWatcher().announceComponentHeartbeat(compExeId);
                }

                @Override
                public String getMethodToCallAsString() {
                    return "start";
                }
            }; // TODO direct method invocation causes an false positive checkstyle error
            Throwable throwable = ppc.callParallelAndWait();

            if (throwable == null) {
                // TODO use better synchronization object
                synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
                    heartbeatFuture = threadPool.scheduleAtFixedInterval("Peridically check for heartbeat messages from components",
                        wfStateMachineCtx.getComponentLostWatcher(), ComponentDisconnectWatcher.DEFAULT_TEST_INTERVAL_MSEC);
                    compRestartDetectionFuture = threadPool.scheduleAtFixedInterval(
                        "Periodically check for restarts of nodes running workflow components", wfStateMachineCtx.getNodeRestartWatcher(),
                        NodeRestartWatcher.DEFAULT_TEST_INTERVAL_MSEC);
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
                    componentExecutionService.pause(compExeId,
                        componentControllerCommandDestinations.getNetworkDestinationForComponentController(compExeId));
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
                    componentExecutionService.resume(compExeId,
                        componentControllerCommandDestinations.getNetworkDestinationForComponentController(compExeId));
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

        private static final int WAIT_INTERVAL_CANCEL_SEC = 90;

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
            if (!componentNodeIds.isEmpty()) { // TODO what does this check? probably whether the component was instantiated yet
                throwable = new ParallelComponentCaller(getComponentsToConsider(true), wfStateMachineCtx.getWorkflowExecutionContext()) {

                    @Override
                    public void callSingleComponent(String compExeId) throws ExecutionControllerException, RemoteOperationException {
                        try {
                            // Special case: As canceling should only be attempted for a limited time instead of retried indefinitely, the
                            // component controller's node id is used instead of the usual rRPC channel here. This allows the normal network
                            // exceptions to occur in case of failure, which are then handled by the code as in RCE 8. -- misc_ro
                            componentExecutionService.cancel(compExeId, componentNodeIds.get(compExeId));
                        } catch (ExecutionControllerException e) {
                            LOG.debug(StringUtils.format("Failed to cancel component(s) of %s; cause: %s",
                                getMethodToCallAsString(), e.toString()));
                        }
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
                } else if (getState() == WorkflowState.CANCELING_AFTER_RESULTS_REJECTED) {
                    wfStateMachineCtx.getWorkflowExecutionStorageBridge()
                        .setWorkflowExecutionFinished(FinalWorkflowState.RESULTS_REJECTED);
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
            final WorkflowExecutionContext workflowExecutionContext = wfStateMachineCtx.getWorkflowExecutionContext();

            endpointDatumDispatchService.unregisterComponentControllerForwardingMap(
                workflowExecutionContext.getWorkflowExecutionHandle().getIdentifier());

            notificationService.send(WorkflowConstants.STATE_DISPOSED_NOTIFICATION_ID, workflowExecutionContext
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
                    // see comment in AsyncCancelTask on the use of the direct node id
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
        notificationService.removePublisher(ConsoleRowUtils.composeConsoleNotificationId(wfStateMachineCtx.getWorkflowExecutionContext()
            .getNodeId(), wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()));
        notificationService.removePublisher(ConsoleRowUtils.composeConsoleNotificationId(wfStateMachineCtx.getWorkflowExecutionContext()
            .getNodeId(), wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()));
        for (WorkflowNode wfNode : wfStateMachineCtx.getWorkflowExecutionContext().getWorkflowDescription().getWorkflowNodes()) {
            ComponentExecutionIdentifier compExeId = wfStateMachineCtx.getWorkflowExecutionContext().getCompExeIdByWfNode(wfNode);
            notificationService.removePublisher(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId.toString());
            notificationService.removePublisher(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId.toString());
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
        notificationService.send(ConsoleRowUtils.composeConsoleNotificationId(wfStateMachineCtx.getWorkflowExecutionContext().getNodeId(),
            wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()), consoleRow);
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
        notificationService.send(ConsoleRowUtils.composeConsoleNotificationId(wfStateMachineCtx.getWorkflowExecutionContext().getNodeId(),
            wfStateMachineCtx.getWorkflowExecutionContext().getExecutionIdentifier()), consoleRow);
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
        // TODO use better synchronization object
        synchronized (wfStateMachineCtx.getComponentStatesChangedEntirelyVerifier()) {
            if (heartbeatFuture != null && !heartbeatFuture.isCancelled()) {
                heartbeatFuture.cancel(false);
            }
            if (compRestartDetectionFuture != null && !compRestartDetectionFuture.isCancelled()) {
                compRestartDetectionFuture.cancel(false);
            }
        }
        workflowTerminatedLatch.countDown();
    }

    @Override
    public void onLastConsoleRowsReceived() {
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

        private final WorkflowState cancelingWfState;

        CancelRequestedEventProcessor(WorkflowState cancelingWfState) {
            this.cancelingWfState = cancelingWfState;
        }

        @Override
        public WorkflowState processEvent(WorkflowState currentState, WorkflowStateMachineEvent event) {
            WorkflowState state = currentState;
            if (checkStateChange(currentState, cancelingWfState)) {
                state = cancelingWfState;
                if (cancelingWfState.equals(WorkflowState.CANCELING_AFTER_FAILED)) {
                    handleFailure(event);
                }
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
            } else if (currentState == WorkflowState.CANCELING_AFTER_RESULTS_REJECTED) {
                if (checkStateChange(currentState, WorkflowState.RESULTS_REJECTED)) {
                    state = WorkflowState.RESULTS_REJECTED;
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
