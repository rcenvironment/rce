/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.spi.WorkflowStateChangeListener;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;

/**
 * Subscriber for {@link WorkflowState} notifications.
 * 
 * @author Doreen Seider
 */
public class WorkflowStateNotificationSubscriber extends DefaultNotificationSubscriber {

    private static final long serialVersionUID = 421042056359014273L;

    private transient WorkflowStateChangeListener listener;

    public WorkflowStateNotificationSubscriber(WorkflowStateChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public Class<?> getInterface() {
        return NotificationSubscriber.class;
    }

    @Override
    public void processNotification(Notification notification) {
        if (notification.getHeader().getNotificationIdentifier().equals(WorkflowConstants.NEW_WORKFLOW_NOTIFICATION_ID)) {
            listener.onNewWorkflowState((String) notification.getBody(), null);
        } else if (WorkflowState.isWorkflowStateValidAndUserReadable((String) notification.getBody())) {
            String workflowId = extractWorkflowIdFromNotificationId(notification);
            listener.onNewWorkflowState(workflowId, WorkflowState.valueOf((String) notification.getBody()));
        }
    }

    private String extractWorkflowIdFromNotificationId(Notification notification) {
        String topic = notification.getHeader().getNotificationIdentifier();
        return topic.replace(WorkflowConstants.STATE_NOTIFICATION_ID, "");
    }

}
