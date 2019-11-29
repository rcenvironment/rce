/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.common.internal;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyReceiver;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link StudyReceiver}.
 * 
 * @author Christian Weiss.
 */
public final class StudyReceiverImpl implements StudyReceiver {

    private static final long serialVersionUID = -6079120096252508794L;

    private static final int MINUS_ONE = -1;

    private final Study study;

    private final ResolvableNodeId platform;

    private NotificationSubscriber notificationSubscriber;

    private DistributedNotificationService notificationService;

    public StudyReceiverImpl(final Study study, final ResolvableNodeId platform,
        DistributedNotificationService notificationService) {
        this.study = study;
        this.platform = platform;
        this.notificationService = notificationService;
    }

    @Override
    public Study getStudy() {
        return study;
    }

    @Override
    public void setNotificationSubscriber(final NotificationSubscriber notificationSubscriber) {
        this.notificationSubscriber = notificationSubscriber;
    }

    @Override
    public void initialize() {
        final String notificationId = ParametricStudyUtils.createDataIdentifier(study);
        Long missedNumber;
        try {
            missedNumber = notificationService.subscribe(
                notificationId, notificationSubscriber, platform).get(notificationId);
        } catch (RemoteOperationException e) {
            LogFactory.getLog(getClass()).error("Failed to subscribe for Parametric Study data source: " + e.getMessage());
            return; // preserve the "old" RTE behavior for now
        }
        // process missed notifications
        if (missedNumber > MINUS_ONE) {
            if (missedNumber > StudyPublisher.BUFFER_SIZE - 1) {
                missedNumber = new Long(StudyPublisher.BUFFER_SIZE - 1);
            }
            try {
                final List<Notification> missedNotifications = notificationService
                    .getNotifications(notificationId, platform)
                    .get(notificationId)
                    .subList(0, missedNumber.intValue() + 1);
                notificationSubscriber.receiveBatchedNotifications(missedNotifications);
            } catch (RemoteOperationException e) {
                LogFactory.getLog(getClass()).warn("Failed to send notifications: " + e.toString());
            }
        }
    }

}
