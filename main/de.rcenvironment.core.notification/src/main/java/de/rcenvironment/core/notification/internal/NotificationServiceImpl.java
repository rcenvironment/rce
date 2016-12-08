/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationHeader;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.toolkitbridge.transitional.StatsCounter;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchProcessor;

/**
 * Implementation of the {@link NotificationService}.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class NotificationServiceImpl implements NotificationService {

    private static final boolean TOPIC_STATISTICS_ENABLED = false;

    /**
     * Helper class to hold local information about subscribers. This includes a set of the subscribed topics, and a {@link BatchAggregator}
     * to group messages to this subscriber.
     * 
     * @author Robert Mischke
     */
    private static final class LocalSubscriberMetaData {

        private final Set<NotificationTopic> subscribedTopics;

        private final BatchAggregator<Notification> batchAggregator;

        /**
         * @param batchAggregator the aggregator instance to use for this subscriber
         */
        LocalSubscriberMetaData(BatchAggregator<Notification> batchAggregator) {
            this.batchAggregator = batchAggregator;
            this.subscribedTopics = new HashSet<NotificationTopic>();
        }

        /**
         * Adds a {@link NotificationTopic} that this subscriber has registered for. Used via {@link #getSubscribedTopics()} to unsubscribe
         * from all topics if necessary.
         * 
         * @param topic the already-subscribed topic
         */
        public void addSubscribedTopic(NotificationTopic topic) {
            synchronized (subscribedTopics) {
                subscribedTopics.add(topic);
            }
        }

        /**
         * Adds a {@link NotificationTopic} that this subscriber is no longer registered for.
         * 
         * @param topic the topic to disconnect from this subscriber
         */
        public boolean removeSubscribedTopic(NotificationTopic topic) {
            synchronized (subscribedTopics) {
                return subscribedTopics.remove(topic);
            }
        }

        public Collection<NotificationTopic> getSubscribedTopics() {
            synchronized (subscribedTopics) {
                // copy to immutable collection to prevent concurrent modifications
                return new ArrayList<NotificationTopic>(subscribedTopics);
            }
        }

        public BatchAggregator<Notification> getBatchAggregator() {
            return batchAggregator;
        }

    }

    /**
     * A {@link BatchProcessor} implementation that sends out batches of {@link Notification}s to a single {@link NotificationSubscriber}.
     * 
     * @author Robert Mischke
     */
    private final class NotificationBatchSender implements BatchProcessor<Notification> {

        private NotificationSubscriber subscriber;

        /**
         * @param subscriber the subscriber to send received batches to
         */
        NotificationBatchSender(NotificationSubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void processBatch(List<Notification> batch) {
            sendNotificationsToSubscriber(subscriber, batch);
        }
    }

    // the maximum number of notifications to aggregate to a single batch
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final int MAX_NOTIFICATION_BATCH_SIZE = 50;

    // the maximum time a notification may be delayed by batch aggregation
    // NOTE: arbitrary value; adjust when useful/necessary
    private static final long MAX_NOTIFICATION_LATENCY = 100;

    private static final Log LOGGER = LogFactory.getLog(NotificationServiceImpl.class);

    /** Local topics. */
    private Map<String, NotificationTopic> topics = Collections.synchronizedMap(new HashMap<String, NotificationTopic>());

    /** Current number of all notifications. */
    private Map<String, Long> currentNumbers = Collections.synchronizedMap(new HashMap<String, Long>());

    /** Buffer sizes of all notifications. */
    private Map<String, Integer> bufferSizes = Collections.synchronizedMap(new HashMap<String, Integer>());

    /** Stored notifications. */
    private Map<String, SortedMap<NotificationHeader, Notification>> allNotifications =
        Collections.synchronizedMap(new HashMap<String, SortedMap<NotificationHeader, Notification>>());

    private WeakHashMap<NotificationSubscriber, LocalSubscriberMetaData> subscriberMap =
        new WeakHashMap<NotificationSubscriber, NotificationServiceImpl.LocalSubscriberMetaData>();

    private PlatformService platformService;

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
    }

    @Override
    public void setBufferSize(String notificationId, int bufferSize) {
        if (bufferSize != 0) {
            bufferSizes.put(notificationId, new Integer(bufferSize));
            // synchronize explicitly to avoid potential "lost update" problem
            synchronized (allNotifications) {
                if (!allNotifications.containsKey(notificationId)) {
                    allNotifications.put(notificationId, new TreeMap<NotificationHeader, Notification>());
                }
            }
        }
    }

    @Override
    public void removePublisher(String notificationId) {

        synchronized (topics) {
            NotificationTopic topic = getNotificationTopic(notificationId);
            if (topic != null) {
                topics.remove(topic.getName());
                currentNumbers.remove(notificationId);
                bufferSizes.remove(notificationId);
                allNotifications.remove(notificationId);
            }
        }
    }

    @Override
    public synchronized <T extends Serializable> void send(String notificationId, T notificationBody) {

        if (TOPIC_STATISTICS_ENABLED) {
            if (StatsCounter.isEnabled()) {
                StatsCounter.count("Notifications sent by id", notificationId);
                StatsCounter.countClass("Notifications sent by body type", notificationBody);
            }
        }

        if (getNotificationTopic(notificationId) == null) {
            registerNotificationTopic(notificationId);
        }

        Long currentEdition = currentNumbers.get(notificationId);
        Notification notification = new Notification(notificationId, currentEdition.longValue() + 1,
            platformService.getLocalInstanceNodeSessionId(), notificationBody);

        SortedMap<NotificationHeader, Notification> notifications = allNotifications.get(notificationId);
        if (notifications != null) {
            Integer bufferSize = bufferSizes.get(notificationId);

            if (bufferSize > 0 && notifications.size() >= bufferSize) {
                if (notifications.remove(notifications.firstKey()) != null) {
                    notifications.put(notification.getHeader(), notification);
                }
            } else {
                notifications.put(notification.getHeader(), notification);
            }
        }

        for (NotificationTopic matchingTopic : getMatchingNotificationTopics(notificationId)) {
            for (NotificationSubscriber subscriber : matchingTopic.getSubscribers()) {
                if (TOPIC_STATISTICS_ENABLED) {
                    if (StatsCounter.isEnabled()) {
                        StatsCounter.count("Notifications enqueued by type", notificationId);
                    }
                }
                sendNotificationToSubscriber(notification, subscriber);
            }
        }

        // TODO review: is this guaranteed to be consistent with asynchronous sending? -- misc_ro
        currentNumbers.put(notificationId, notification.getHeader().getNumber());
    }

    private void sendNotificationToSubscriber(Notification notification, NotificationSubscriber subscriber) {
        getLocalSubscriberMetaData(subscriber).getBatchAggregator().enqueue(notification);
    }

    @Override
    @AllowRemoteAccess
    public Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber) {

        Map<String, Long> lastNumbers = new HashMap<String, Long>();

        NotificationTopic topic;

        synchronized (topics) {
            topic = getNotificationTopic(notificationId);
            if (topic == null) {
                topic = registerNotificationTopic(notificationId);
                if (TOPIC_STATISTICS_ENABLED) {
                    if (StatsCounter.isEnabled()) {
                        StatsCounter.count("Register Topic", notificationId);
                    }
                }
            }
        }
        topic.add(subscriber);
        getLocalSubscriberMetaData(subscriber).addSubscribedTopic(topic);

        synchronized (currentNumbers) {
            for (String tmpId : currentNumbers.keySet()) {
                if (tmpId.matches(notificationId)) {
                    lastNumbers.put(tmpId, currentNumbers.get(tmpId));
                }
            }
        }

        return lastNumbers;
    }

    @Override
    @AllowRemoteAccess
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber) {

        synchronized (topics) {
            NotificationTopic topic = getNotificationTopic(notificationId);
            if (topic != null) {
                topic.remove(subscriber);
                getLocalSubscriberMetaData(subscriber).removeSubscribedTopic(topic);
            }
        }
    }

    @Override
    public Notification getNotification(NotificationHeader header) {

        Notification notification = null;
        Map<NotificationHeader, Notification> notifications = allNotifications.get(header.getNotificationIdentifier());
        if (notifications != null) {
            notification = notifications.get(header);
        }
        return notification;
    }

    @Override
    public Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationId) {

        Map<String, SortedSet<NotificationHeader>> allHeaders = new HashMap<String, SortedSet<NotificationHeader>>();

        // note: access to iterators of synchronized maps must be synchronized explicitly
        synchronized (allNotifications) {
            // TODO iterating over map entries would probably be more efficient
            for (String tmpId : allNotifications.keySet()) {
                if (tmpId.matches(notificationId)) {
                    Map<NotificationHeader, Notification> notifications = allNotifications.get(tmpId);
                    SortedSet<NotificationHeader> headers = new TreeSet<NotificationHeader>(notifications.keySet());
                    allHeaders.put(tmpId, headers);
                }
            }
        }

        return allHeaders;
    }

    @Override
    @AllowRemoteAccess
    public Map<String, List<Notification>> getNotifications(String notificationId) {

        Map<String, List<Notification>> allNotificationsToGet = new HashMap<String, List<Notification>>();

        // note: access to iterators of synchronized maps must be synchronized explicitly
        synchronized (allNotifications) {
            // TODO iterating over map entries would probably be more efficient
            for (String tmpId : allNotifications.keySet()) {
                if (tmpId.matches(notificationId)) {
                    Map<NotificationHeader, Notification> notifications = allNotifications.get(tmpId);
                    List<Notification> notificationsToGet = new ArrayList<Notification>(notifications.values());
                    allNotificationsToGet.put(tmpId, notificationsToGet);
                }
            }
        }
        return allNotificationsToGet;
    }

    /**
     * Sends a single {@link Notification} to a {@link NotificationSubscriber}.
     * 
     * @param subscriber the subscriber to send the notification to
     * @param matchingTopic the matching topic that caused the subscriber to receive this notification
     * @param notifications the notifications, ie the actual content
     */
    private void sendNotificationsToSubscriber(NotificationSubscriber subscriber, List<Notification> notifications) {
        try {
            try {
                subscriber.receiveBatchedNotifications(notifications);
            } catch (RuntimeException e) {
                // TODO >=8.0.0: safeguard code added in 7.0.0 transition; remove if never observed in 7.x cycle
                LOGGER.error("Unexpected RTE thrown from receiveBatchedNotifications()", e);
                throw new RemoteOperationException(e.toString());
            }
        } catch (RemoteOperationException e) {
            // not much information available, so use identity to tell subscribers apart in log
            int subscriberIdentity = System.identityHashCode(subscriber);
            Collection<NotificationTopic> subscribedTopics = getLocalSubscriberMetaData(subscriber).getSubscribedTopics();
            if (subscribedTopics.isEmpty()) {
                LOGGER.debug("Tried to remove subscriber " + subscriberIdentity
                    + " after a callback failure but it had no (or no more) topics to unsubscribe from; triggering error: " + e.toString());
            } else {
                for (NotificationTopic topic : subscribedTopics) {
                    unsubscribe(topic.getName(), subscriber);
                    LOGGER.debug("Removed subscriber " + subscriberIdentity + " from topic "
                        + topic.getName() + " after a callback failure: " + e.toString());
                }
            }
        }
    }

    private NotificationTopic registerNotificationTopic(String notificationId) {

        NotificationTopic topic = new NotificationTopic(notificationId);
        synchronized (topics) {
            topics.put(topic.getName(), topic);
        }
        currentNumbers.put(notificationId, new Long(NO_MISSED));
        return topic;
    }

    private NotificationTopic getNotificationTopic(String notificationId) {
        synchronized (topics) {
            return topics.get(notificationId);
        }
    }

    private LocalSubscriberMetaData getLocalSubscriberMetaData(NotificationSubscriber subscriber) {
        synchronized (subscriberMap) {
            LocalSubscriberMetaData metaData = subscriberMap.get(subscriber);
            if (metaData == null) {
                final BatchProcessor<Notification> batchProcessor = new NotificationBatchSender(subscriber);
                final BatchAggregator<Notification> batchAggregator =
                    ConcurrencyUtils.getFactory().createBatchAggregator(MAX_NOTIFICATION_BATCH_SIZE, MAX_NOTIFICATION_LATENCY,
                        batchProcessor);
                metaData = new LocalSubscriberMetaData(batchAggregator);
                subscriberMap.put(subscriber, metaData);
            }
            return metaData;
        }
    }

    private Set<NotificationTopic> getMatchingNotificationTopics(String currentNotificationId) {

        // TODO (p2) >8.0.0: this is performed on every send() call, and is quite CPU and GC intensive; rework approach
        Set<NotificationTopic> matchingTopics = new HashSet<NotificationTopic>();
        synchronized (topics) {
            for (NotificationTopic topic : topics.values()) {
                if (topic.getNotificationIdFilter().matcher(currentNotificationId).matches()) {
                    matchingTopics.add(topic);
                }
            }
            return matchingTopics;
        }
    }

}
