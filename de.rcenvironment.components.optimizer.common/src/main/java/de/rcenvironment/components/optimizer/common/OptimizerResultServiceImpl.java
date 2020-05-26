/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;
import java.util.List;

import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link OptimizerResultService}.
 * 
 * @author Christian Weiss
 * @author Sascha Zur
 */
public class OptimizerResultServiceImpl implements OptimizerResultService {

    private NotificationService notificationService;

    private DistributedNotificationService distributedNotificationService;

    protected void bindNotificationService(final NotificationService newNotificationService) {
        notificationService = newNotificationService;
    }

    protected void bindDistributedNotificationService(final DistributedNotificationService newDistrNotificationService) {
        distributedNotificationService = newDistrNotificationService;
    }

    @Override
    public OptimizerPublisher createPublisher(final String identifier,
        final String title, final ResultStructure structure) {
        final ResultSet study = new ResultSet(identifier, title, structure);
        final OptimizerPublisher studyPublisher = new OptimizerPublisherImpl(study, notificationService);
        final String notificationId = StringUtils.format(OptimizerUtils.STRUCTURE_PATTERN, study.getIdentifier());
        notificationService.setBufferSize(notificationId, 1);
        notificationService.send(notificationId, new Serializable[] { study.getStructure(), title });
        return studyPublisher;
    }

    @Override
    public OptimizerReceiver createReceiver(final String identifier,
        final ResolvableNodeId platform) throws RemoteOperationException {
        final String notificationId = StringUtils.format(OptimizerUtils.STRUCTURE_PATTERN,
            identifier);
        if (distributedNotificationService != null && distributedNotificationService
            .getNotifications(notificationId, platform) != null) {
            final List<Notification> notifications = distributedNotificationService
                .getNotifications(notificationId, platform).get(notificationId);
            if (notifications != null && notifications.size() > 0) {
                final Notification studyNotification = notifications
                    .get(notifications.size() - 1);
                final Serializable[] notificationContent = (Serializable[]) studyNotification.getBody();
                final ResultStructure structure = (ResultStructure) notificationContent[0];
                final String title = (String) notificationContent[1];
                final ResultSet study = new ResultSet(identifier, title, structure);
                final OptimizerReceiver studyReceiver = new OptimizerReceiverImpl(study,
                    platform, distributedNotificationService);
                return studyReceiver;
            }
        }
        return null;
    }

}
