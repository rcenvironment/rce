/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.common;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link OptimizerReceiver}.
 * 
 * @author Christian Weiss.
 */
public final class OptimizerReceiverImpl implements OptimizerReceiver {

    private static final long serialVersionUID = -6079120096252508794L;

    private static final int MINUS_ONE = -1;

    private final ResultSet study;

    private final NodeIdentifier platform;

    private NotificationSubscriber notificationSubscriber;

    private DistributedNotificationService notificationService;

    public OptimizerReceiverImpl(final ResultSet study, final NodeIdentifier platform,
        DistributedNotificationService notificationService) {
        this.study = study;
        this.platform = platform;
        this.notificationService = notificationService;
    }

    @Override
    public ResultSet getStudy() {
        return study;
    }

    @Override
    public void setNotificationSubscriber(final NotificationSubscriber notificationSubscriber) {
        this.notificationSubscriber = notificationSubscriber;
    }

    @Override
    public void initialize() {
        final String notificationId = OptimizerUtils.createDataIdentifier(study);
        Long missedNumber;
        try {
            missedNumber = notificationService.subscribe(
                notificationId, notificationSubscriber, platform).get(notificationId);
        } catch (RemoteOperationException e) {
            LogFactory.getLog(getClass()).error("Failed to set up subscription for Optimizer data: " + e.getMessage());
            return; // preserve the "old" RTE behavior for now
        }
        // process missed notifications
        if (missedNumber > MINUS_ONE) {
            if (missedNumber > OptimizerPublisher.BUFFER_SIZE - 1) {
                missedNumber = new Long(OptimizerPublisher.BUFFER_SIZE - 1);
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
