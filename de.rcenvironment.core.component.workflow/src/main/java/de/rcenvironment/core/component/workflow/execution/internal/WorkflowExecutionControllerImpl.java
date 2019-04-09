/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentState;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;

/**
 * Implementation of {@link WorkflowExecutionController}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionControllerImpl implements WorkflowExecutionController {

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionControllerImpl.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    private static DistributedNotificationService notificationService;

    private static WorkflowExecutionRelatedInstancesFactory wfExeInstancesFactory;

    private WorkflowExecutionContext wfExeCtx;

    private WorkflowExecutionStorageBridge wfExeStorageBridge;

    private WorkflowStateMachine wfStateMachine;

    private ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier;

    private ComponentsConsoleLogFileWriter compConsoleLogFileWriter;

    private ComponentDisconnectWatcher compLostWatcher;

    private AsyncOrderedExecutionQueue notifSendingAsyncExecQueue =
        ConcurrencyUtils.getFactory().createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

    private NodeRestartWatcher nodeRestartWatcher;

    @Deprecated
    public WorkflowExecutionControllerImpl() {}

    public WorkflowExecutionControllerImpl(WorkflowExecutionContext wfContext, ServiceRegistryAccess serviceRegistryAccess) {
        this.wfExeCtx = wfContext;

        // TODO why not move all this initialization into the WorkflowStateMachineContext constructor? -- misc_ro

        wfExeStorageBridge = wfExeInstancesFactory.createWorkflowExecutionStorageBridge(wfContext);

        compConsoleLogFileWriter = wfExeInstancesFactory.createComponentConsoleLogFileWriter(wfExeStorageBridge);

        compStatesEntirelyChangedVerifier = wfExeInstancesFactory.createComponentStatesEntirelyChangedVerifier(
            wfExeCtx.getWorkflowDescription().getWorkflowNodes().size()
                - WorkflowExecutionUtils.getDisabledWorkflowNodes(wfExeCtx.getWorkflowDescription()).size());

        compLostWatcher = wfExeInstancesFactory.createComponentLostWatcher(wfExeCtx, compStatesEntirelyChangedVerifier);

        nodeRestartWatcher =
            wfExeInstancesFactory.createNodeRestartWatcher(wfExeCtx, compStatesEntirelyChangedVerifier, serviceRegistryAccess);

        final WorkflowStateMachineContext wfStateMachineCtx = new WorkflowStateMachineContext(wfExeCtx, wfExeStorageBridge,
            compStatesEntirelyChangedVerifier, compConsoleLogFileWriter, compLostWatcher, nodeRestartWatcher, serviceRegistryAccess);

        wfStateMachine = wfExeInstancesFactory.createWorkflowStateMachine(wfStateMachineCtx);

        compStatesEntirelyChangedVerifier.addListener(wfStateMachine);
    }

    @Override
    public void start() {
        wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.START_REQUESTED));
    }

    @Override
    public void pause() {
        wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PAUSE_REQUESTED));
    }

    @Override
    public void resume() {
        wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.RESUME_REQUESTED));
    }

    @Override
    public void restart() {
        throw new UnsupportedOperationException("Restarting workflows not yet implemented");
    }

    @Override
    public void cancel() {
        wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_REQUESTED));
    }

    @Override
    public void dispose() {
        wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.DISPOSE_REQUESTED));
    }

    @Override
    public void setComponentExecutionAuthTokens(Map<String, String> exeAuthTokens) {
        wfStateMachine.setComponentExecutionAuthTokens(exeAuthTokens);
    }

    @Override
    public WorkflowState getState() {
        return wfStateMachine.getState();
    }

    @Override
    public Long getDataManagementId() {
        return wfExeStorageBridge.getWorkflowInstanceDataManamagementId();
    }

    @Override
    public void onComponentStateChanged(String compExeId, ComponentState newState,
        Integer executionCount, String executionCountOnResets) {
        onComponentStateChanged(compExeId, newState, executionCount, executionCountOnResets, null, null);
    }

    @Override
    public void onComponentStateChanged(String compExeId, ComponentState newState, Integer executionCount, String executionCountOnResets,
        String errorId) {
        onComponentStateChanged(compExeId, newState, executionCount, executionCountOnResets, errorId, null);
    }

    @Override
    public synchronized void onComponentStateChanged(final String compExeId, final ComponentState newState,
        Integer executionCount, final String executionCountOnResets, String errorId, String errorMessage) {

        notifSendingAsyncExecQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                notificationService.send(ComponentConstants.STATE_NOTIFICATION_ID_PREFIX + compExeId, newState.name());
                notificationService.send(ComponentConstants.ITERATION_COUNT_NOTIFICATION_ID_PREFIX + compExeId, executionCountOnResets);
            }
        });

        compStatesEntirelyChangedVerifier.announceComponentState(compExeId, newState);

        if (ComponentConstants.FAILED_COMPONENT_STATES.contains(newState)) {
            WorkflowStateMachineEventType eventType = WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED;
            if (newState == ComponentState.RESULTS_REJECTED) {
                eventType = WorkflowStateMachineEventType.CANCEL_AFTER_RESULTS_REJECTED_REQUESTED;
            }
            wfStateMachine.postEvent(new WorkflowStateMachineEvent(eventType, errorId, errorMessage, compExeId));

        }
    }

    @Override
    public void onInputProcessed(String serializedEndpointDatum) {
        notificationService.send(StringUtils.format(ComponentConstants.NOTIFICATION_ID_PREFIX_PROCESSED_INPUT + "%s:%s", wfExeCtx
            .getNodeId().getLogicalNodeIdString(), wfExeCtx.getExecutionIdentifier()), serializedEndpointDatum);
    }

    @Override
    public void processConsoleRows(ConsoleRow[] consoleRows) {
        for (ConsoleRow consoleRow : consoleRows) {
            notificationService.send(ConsoleRowUtils.composeConsoleNotificationId(wfExeCtx.getNodeId(), wfExeCtx.getExecutionIdentifier()),
                consoleRow);
            try {
                checkForLifecycleToolRunConsoleRow(consoleRow);
            } catch (WorkflowExecutionException e) {
                wfStateMachine
                    .postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.PROCESS_COMPONENT_TIMELINE_EVENTS_FAILED, e));
            }
            compConsoleLogFileWriter.addComponentConsoleRow(consoleRow);

            checkForLifecycleInfoEndConsoleRow(consoleRow);
        }
    }

    @Override
    public void onComponentHeartbeatReceived(String compExecutionId) {
        if (VERBOSE_LOGGING) {
            LOG.debug(StringUtils.format("Received hearbeat from component (%s) for workflow '%s' (%s)",
                compExecutionId, wfExeCtx.getInstanceName(), wfExeCtx.getExecutionIdentifier()));
        }
        compLostWatcher.announceComponentHeartbeat(compExecutionId);
    }

    private void checkForLifecycleInfoEndConsoleRow(ConsoleRow row) {
        if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT
            && row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.COMPONENT_TERMINATED.name())) {
            compStatesEntirelyChangedVerifier.announceLastConsoleRow(row.getComponentIdentifier());
        }
    }

    private void checkForLifecycleToolRunConsoleRow(ConsoleRow row) throws WorkflowExecutionException {
        if (row.getType() == ConsoleRow.Type.LIFE_CYCLE_EVENT) {
            if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.TOOL_STARTING.name())) {
                wfExeStorageBridge.addComponentTimelineInterval(TimelineIntervalType.EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN,
                    row.getTimestamp(), StringUtils.splitAndUnescape(row.getPayload())[1]);
            } else if (row.getPayload().startsWith(ConsoleRow.WorkflowLifecyleEventType.TOOL_FINISHED.name())) {
                wfExeStorageBridge.setComponentTimelineIntervalFinished(TimelineIntervalType.EXTERNAL_TOOL_RUN_IN_COMPONENT_RUN,
                    row.getTimestamp(), StringUtils.splitAndUnescape(row.getPayload())[1]);
            }
        }
    }

    protected void bindDistributedNotificationService(DistributedNotificationService newService) {
        WorkflowExecutionControllerImpl.notificationService = newService;
    }

    protected void bindWorkflowExecutionRelatedInstancesFactory(WorkflowExecutionRelatedInstancesFactory newService) {
        WorkflowExecutionControllerImpl.wfExeInstancesFactory = newService;
    }

}
