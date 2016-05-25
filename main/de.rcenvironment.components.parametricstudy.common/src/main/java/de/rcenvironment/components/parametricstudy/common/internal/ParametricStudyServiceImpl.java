/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.common.internal;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.parametricstudy.common.ParametricStudyService;
import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyReceiver;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link ParametricStudyService}.
 * @author Christian Weiss
 */
public class ParametricStudyServiceImpl implements ParametricStudyService {

    private NotificationService notificationService;

    private DistributedNotificationService distributedNotificationService;

    protected void bindNotificationService(final NotificationService newNotificationService) {
        notificationService = newNotificationService;
    }

    protected void bindDistributedNotificationService(final DistributedNotificationService newDistrNotificationService) {
        distributedNotificationService = newDistrNotificationService;
    }

    @Override
    public StudyPublisher createPublisher(final String identifier, final String title, final StudyStructure structure) {
        final Study study = new Study(identifier, title, structure);
        final StudyPublisher studyPublisher = new StudyPublisherImpl(study, notificationService);
        final String notificationId = StringUtils.format(ParametricStudyUtils.STRUCTURE_PATTERN, study.getIdentifier());
        notificationService.setBufferSize(notificationId, 1);
        notificationService.send(notificationId, new Serializable[] { study.getStructure(), title});
        return studyPublisher;
    }

    @Override
    public StudyReceiver createReceiver(final String identifier, final NodeIdentifier node) {
        final String notificationId = StringUtils.format(ParametricStudyUtils.STRUCTURE_PATTERN,
                identifier);
        List<Notification> notifications;
        try {
            notifications = distributedNotificationService
                    .getNotifications(notificationId, node).get(notificationId);
            if (notifications.size() > 0) {
                final Notification studyNotification = notifications
                    .get(notifications.size() - 1);
                final Serializable[] notificationContent = (Serializable[]) studyNotification.getBody();
                final StudyStructure structure = (StudyStructure) notificationContent[0];
                final String title = (String) notificationContent[1];
                final Study study = new Study(identifier, title, structure);
                final StudyReceiver studyReceiver = new StudyReceiverImpl(study,
                    node, distributedNotificationService);
                return studyReceiver;
            }
        } catch (RemoteOperationException e) {
            LogFactory.getLog(getClass()).error("Failed to get remote notifications.");
        }
        return null;
    }

}
