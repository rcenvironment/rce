/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.spi.MultipleWorkflowsStateChangeListener;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Subscriber for {@link WorkflowState} notifications.
 * 
 * @author Doreen Seider
 */
public class WorkflowStateNotificationSubscriber extends DefaultNotificationSubscriber {

    private static final long serialVersionUID = 421042056359014273L;

    private static final transient long IS_ALIVE_CHECK_INTERVAL_MSEC = 20000;

    private final transient boolean considersMultipleWorkflows;

    private transient MultipleWorkflowsStateChangeListener multiWfStateChangeListener;

    private transient SingleWorkflowStateChangeListener singleWfStateChangeListener;

    private transient String singleWfExecutionId;

    private transient volatile long latestIsAliveReceived = 0;

    private transient ScheduledFuture<?> isWorkflowAliveCheckTask = null;
    
    private AtomicBoolean isStopped = new AtomicBoolean(false);

    public WorkflowStateNotificationSubscriber(MultipleWorkflowsStateChangeListener listener) {
        this.multiWfStateChangeListener = listener;
        considersMultipleWorkflows = true;
    }

    public WorkflowStateNotificationSubscriber(SingleWorkflowStateChangeListener listener, String wfExecutionId) {
        this.singleWfStateChangeListener = listener;
        this.singleWfExecutionId = wfExecutionId;
        considersMultipleWorkflows = false;
    }

    @Override
    public Class<?> getInterface() {
        return NotificationSubscriber.class;
    }

    @Override
    public void processNotification(Notification notification) {
        if (notification.getHeader().getNotificationIdentifier().equals(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID)) {
            onWorkflowStateChanged((String) notification.getBody(), WorkflowState.INIT);
        } else if (WorkflowState.isWorkflowStateValid((String) notification.getBody())) {
            WorkflowState workflowState = WorkflowState.valueOf((String) notification.getBody());
            String wfExecutionId = extractWorkflowIdFromNotificationId(notification);
            onWorkflowStateChanged(wfExecutionId, workflowState);
        }
    }

    private void onWorkflowStateChanged(String wfExecutionId, WorkflowState newWorkflowState) {
        if (!newWorkflowState.equals(WorkflowState.IS_ALIVE)) {
            if (considersMultipleWorkflows) {
                multiWfStateChangeListener.onWorkflowStateChanged(wfExecutionId, newWorkflowState);
            } else {
                singleWfStateChangeListener.onWorkflowStateChanged(newWorkflowState);
            }
        }
        if (!considersMultipleWorkflows) {
            if (FinalWorkflowState.isFinalWorkflowState(newWorkflowState)) {
                stopCheckingForWorkflowNotAlive();
            } else if (!newWorkflowState.equals(WorkflowState.DISPOSING)
                && !newWorkflowState.equals(WorkflowState.DISPOSED)) {
                startCheckingForWorkflowNotAlive();
            }
            if (newWorkflowState.equals(WorkflowState.IS_ALIVE)) {
                latestIsAliveReceived = System.currentTimeMillis();
            }
        }
    }

    private String extractWorkflowIdFromNotificationId(Notification notification) {
        String topic = notification.getHeader().getNotificationIdentifier();
        return topic.replace(WorkflowConstants.STATE_NOTIFICATION_ID, "");
    }

    /**
     * Starts to check, that {@link WorkflowState#IS_ALIVE} is received continuously. If messages
     * stop, {@link SingleWorkflowsStateChangeListener#onWorkflowNotAliveAnymore()} is called.
     */
    private synchronized void startCheckingForWorkflowNotAlive() {
        if (isWorkflowAliveCheckTask == null) {
            latestIsAliveReceived = System.currentTimeMillis();
            isWorkflowAliveCheckTask = ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedRate(new Runnable() {

                @TaskDescription("Check workflow is alive")
                @Override
                public void run() {
                    if (!isStopped.get()) {
                        if (System.currentTimeMillis() - latestIsAliveReceived > IS_ALIVE_CHECK_INTERVAL_MSEC) {
                            isStopped.set(true);
                            String errorMessage = StringUtils.format(
                                "Receiving 'is alive' message from workflow '%s' stopped. Most likely, "
                                + "because the network connection to the workflow host node was interrupted",
                                singleWfExecutionId);
                            singleWfStateChangeListener.onWorkflowNotAliveAnymore(errorMessage);
                            ConcurrencyUtils.getAsyncTaskService().submit(new Runnable() {
    
                                @TaskDescription("Stop checking workflow is alive")
                                @Override
                                public void run() {
                                    stopCheckingForWorkflowNotAlive();
                                }
                            });
                        }
                    }
                }
            }, IS_ALIVE_CHECK_INTERVAL_MSEC);
        }
    }

    /**
     * Stops to check, that {@link WorkflowState#IS_ALIVE} is received continuously.
     */
    private synchronized void stopCheckingForWorkflowNotAlive() {
        if (isWorkflowAliveCheckTask != null) {
            isWorkflowAliveCheckTask.cancel(false);
        }
    }

}
