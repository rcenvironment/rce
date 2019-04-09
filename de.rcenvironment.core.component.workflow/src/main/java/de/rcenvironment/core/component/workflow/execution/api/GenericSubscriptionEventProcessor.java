/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchProcessor;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;

/**
 * Subscriber implementation that attempts to "catch up" with notifications that were sent before the subscription, but are still buffered
 * on the sender's side.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * 
 * Note: Consider it as broken. Not because of the code here but because of the {@link NotificationService} and a missing, reliable
 * concept for event streams. --seid_do
 */
public abstract class GenericSubscriptionEventProcessor extends DefaultNotificationSubscriber {

    private static final long serialVersionUID = 3619909997095130853L;

    // configure BatchAggregator for high capacity but low latency to limit the rate of generated update events - misc_ro
    private static final int LOCAL_NOTIFICATION_BATCH_SIZE_LIMIT = 2000;

    private static final int LOCAL_NOTIFICATION_BATCH_TIME_LIMIT_MSEC = 200;

    // protected final transient List<Notification> notificationsToProcess = new LinkedList<Notification>();

    private final transient Map<String, Long> lastMissedNotifications = new HashMap<String, Long>();

    private final transient Map<String, Boolean> catchingUpWithMissedNotifications = new HashMap<String, Boolean>();

    private final transient Map<String, Deque<Notification>> queuedNotifications = new HashMap<String, Deque<Notification>>();

    private final transient BatchAggregator<Notification> batchAggregator;

    // private final transient Log log = LogFactory.getLog(getClass());

    public GenericSubscriptionEventProcessor() {
        final ConcurrencyUtilsFactory factory = ConcurrencyUtils.getFactory();
        batchAggregator = factory.createBatchAggregator(LOCAL_NOTIFICATION_BATCH_SIZE_LIMIT,
            LOCAL_NOTIFICATION_BATCH_TIME_LIMIT_MSEC, new BatchProcessor<Notification>() {

                @Override
                public void processBatch(List<Notification> batch) {
                    processCollectedNotifications(batch);
                }

            });
    }

    @Override
    protected void processNotification(Notification notification) {

        // FIXME catching up missing notifications certainly doesn't work -> rework!
        // currently, models are initiated very early and thus, it "probably" has no effects - seid_do, July, 2013

        String notifId = createIdentifier(notification);
        if (catchingUpWithMissedNotifications.containsKey(notifId)
            && catchingUpWithMissedNotifications.get(notifId)
            && lastMissedNotifications.get(notifId) == NotificationService.NO_MISSED) {
            queuedNotifications.get(notifId).add(notification);
        } else if (catchingUpWithMissedNotifications.containsKey(notifId)
            && catchingUpWithMissedNotifications.get(notifId)
            && notification.getHeader().getNumber() > lastMissedNotifications.get(notifId)) {
            queuedNotifications.get(notifId).add(notification);
        } else {
            handleIncomingNotification(notification);
            if (catchingUpWithMissedNotifications.containsKey(notifId)
                && catchingUpWithMissedNotifications.get(notifId)
                && notification.getHeader().getNumber() == lastMissedNotifications.get(notifId)) {
                catchingUpWithMissedNotifications.put(notifId, false);
                while (!queuedNotifications.get(notifId).isEmpty()) {
                    processNotification(queuedNotifications.get(notifId).pollFirst());
                }
            }
        }
    }

    private String createIdentifier(Notification notification) {
        return createIdentifier(notification.getHeader().getNotificationIdentifier(),
            notification.getHeader().getPublishPlatform().getInstanceNodeSessionIdString());
    }

    private String createIdentifier(String notifId, String nodeId) {
        return notifId + nodeId;
    }

    private void handleIncomingNotification(Notification notification) {
        // enqueue received row for batch processing
        batchAggregator.enqueue(notification);
    }

    /**
     * Process all collected {@link ConsoleRow} updates and perform a single GUI update to improve performance.
     */
    protected abstract void processCollectedNotifications(List<Notification> notifications);

    @Override
    public Class<? extends Serializable> getInterface() {
        return NotificationSubscriber.class;
    }

    /**
     * Registers a notification type for handling past notifications of that type.
     * 
     * @param notifId identifier of the notification
     * @param nodeId identifier of source node
     * @param lastMissedNumber number of last missed notification.
     */
    public void setNumberOfLastMissingNotification(String notifId, String nodeId, Long lastMissedNumber) {
        String notificationId = createIdentifier(notifId, nodeId);
        queuedNotifications.put(notificationId, new LinkedList<Notification>());
        lastMissedNotifications.put(notificationId, lastMissedNumber);
        catchingUpWithMissedNotifications.put(notificationId, true);
    }

    /**
     * Flushes all pending notifications.
     */
    public void flush() {
        // TODO review @5.0: does this need a replacement after subscriber class changes? add method to flush the batchProcessor?
        // processCollectedNotifications();
    }

}
