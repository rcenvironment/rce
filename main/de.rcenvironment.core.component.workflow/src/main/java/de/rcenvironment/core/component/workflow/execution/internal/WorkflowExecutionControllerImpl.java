/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
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
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.datamodel.api.TimelineIntervalType;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncCallbackExceptionPolicy;
import de.rcenvironment.core.utils.common.concurrent.AsyncOrderedExecutionQueue;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * Implementation of {@link WorkflowExecutionController}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionControllerImpl implements WorkflowExecutionController {

    private static final Log LOG = LogFactory.getLog(WorkflowExecutionControllerImpl.class);

    private static final boolean VERBOSE_LOGGING = DebugSettings.getVerboseLoggingEnabled(WorkflowExecutionControllerImpl.class);

    private static DistributedNotificationService notificationService;

    private static WorkflowExecutionRelatedInstancesFactory wfExeInstancesFactory;

    private WorkflowExecutionContext wfExeCtx;

    private WorkflowExecutionStorageBridge wfExeStorageBridge;

    private WorkflowStateMachine wfStateMachine;

    private ComponentStatesChangedEntirelyVerifier compStatesEntirelyChangedVerifier;

    private ComponentsConsoleLogFileWriter compConsoleLogFileWriter;

    private ComponentLostWatcher compLostWatcher;
    
    private AsyncOrderedExecutionQueue notifSendingAsyncExecQueue =
        new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, SharedThreadPool.getInstance());

    @Deprecated
    public WorkflowExecutionControllerImpl() {}

    public WorkflowExecutionControllerImpl(WorkflowExecutionContext wfContext) {
        this.wfExeCtx = wfContext;

        wfExeStorageBridge = wfExeInstancesFactory.createWorkflowExecutionStorageBridge(wfContext);

        compConsoleLogFileWriter = wfExeInstancesFactory.createComponentConsoleLogFileWriter(wfExeStorageBridge);

        compStatesEntirelyChangedVerifier = wfExeInstancesFactory.createComponentStatesEntirelyChangedVerifier(
            wfExeCtx.getWorkflowDescription().getWorkflowNodes().size()
                - WorkflowExecutionUtils.getDisabledWorkflowNodes(wfExeCtx.getWorkflowDescription()).size());

        compLostWatcher = wfExeInstancesFactory.createComponentLostWatcher(wfExeCtx, compStatesEntirelyChangedVerifier);

        wfStateMachine = wfExeInstancesFactory.createWorkflowStateMachine(new WorkflowStateMachineContext(wfExeCtx, wfExeStorageBridge,
            compStatesEntirelyChangedVerifier, compConsoleLogFileWriter, compLostWatcher));

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

        if (newState == ComponentState.FAILED) {
            wfStateMachine.postEvent(new WorkflowStateMachineEvent(WorkflowStateMachineEventType.CANCEL_AFTER_FAILED_REQUESTED,
                errorId, errorMessage, compExeId));
        }
    }

    @Override
    public void onInputProcessed(String serializedEndpointDatum) {
        notificationService.send(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
            + ComponentConstants.PORCESSED_INPUT_NOTIFICATION_ID_SUFFIX, serializedEndpointDatum);
    }

    @Override
    public void processConsoleRows(ConsoleRow[] consoleRows) {
        for (ConsoleRow consoleRow : consoleRows) {
            notificationService.send(wfExeCtx.getExecutionIdentifier() + wfExeCtx.getNodeId().getIdString()
                + ConsoleRow.NOTIFICATION_SUFFIX, consoleRow);
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
