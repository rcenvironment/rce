/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.internal;

import java.util.function.Consumer;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;

final class WorkflowStateNotificationHandler extends DefaultNotificationSubscriber {

    private static final long serialVersionUID = 8010635011182816802L;

    private final Consumer<WorkflowState> workflowFinishedNotifier;

    private final Runnable heartbeat;

    private final String workflowExecutionIdentifier;

    WorkflowStateNotificationHandler(String workflowExecutionIdentifier, Runnable heartbeat,
        Consumer<WorkflowState> workflowFinishedNotifier) {
        this.heartbeat = heartbeat;
        this.workflowFinishedNotifier = workflowFinishedNotifier;
        this.workflowExecutionIdentifier = workflowExecutionIdentifier;
    }

    @Override
    public Class<?> getInterface() {
        return NotificationSubscriber.class;
    }

    @Override
    protected void processNotification(Notification notification) {
        if (!notification.getHeader().getNotificationIdentifier()
            .equals(WorkflowConstants.STATE_NOTIFICATION_ID + workflowExecutionIdentifier)) {
            return;
        }

        if (!(notification.getBody() instanceof String)) {
            return;
        }

        final String notificationBody = (String) notification.getBody();

        if (!WorkflowState.isWorkflowStateValid(notificationBody)) {
            return;
        }

        final WorkflowState newWorkflowState = WorkflowState.valueOf(notificationBody);

        if (WorkflowState.IS_ALIVE.equals(newWorkflowState)) {
            this.heartbeat.run();
        }

        if (!FinalWorkflowState.isFinalWorkflowState(newWorkflowState)) {
            return;
        }

        this.workflowFinishedNotifier.accept(newWorkflowState);
    }
}
