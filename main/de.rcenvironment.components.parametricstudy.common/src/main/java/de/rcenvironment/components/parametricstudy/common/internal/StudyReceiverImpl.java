/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.parametricstudy.common.internal;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyReceiver;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;

/**
 * Implementation of {@link StudyReceiver}.
 * 
 * @author Christian Weiss.
 */
public final class StudyReceiverImpl implements StudyReceiver {

    private static final long serialVersionUID = -6079120096252508794L;

    private static final int MINUS_ONE = -1;

    private final Study study;

    private final NodeIdentifier platform;

    private NotificationSubscriber notificationSubscriber;

    private DistributedNotificationService notificationService;

    public StudyReceiverImpl(final Study study, final NodeIdentifier platform,
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
        } catch (CommunicationException e) {
            LogFactory.getLog(getClass()).error("Failed to subscribe for Parametric Study data source: " + e.getMessage());
            return; // preserve the "old" RTE behavior for now
        }
        // process missed notifications
        if (missedNumber > MINUS_ONE) {
            if (missedNumber > StudyPublisher.BUFFER_SIZE - 1) {
                missedNumber = new Long(StudyPublisher.BUFFER_SIZE - 1);
            }
            final List<Notification> missedNotifications = notificationService
                .getNotifications(notificationId, platform)
                .get(notificationId)
                .subList(0, missedNumber.intValue() + 1);
            // note: passing the list of missed notifications as a single batch since 5.0.0 - misc_ro
            notificationSubscriber.receiveBatchedNotifications(missedNotifications);
        }
    }

}
