/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Class providing convenient access to the distributed notification system. It serves as an
 * abstraction of all distributed {@link NotificationService}s.
 * 
 * @author Doreen Seider
 */
public class SimpleNotificationService {

    private DistributedNotificationService distrNotificationService;

    public SimpleNotificationService() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        distrNotificationService = serviceRegistryAccess.getService(DistributedNotificationService.class);
    }

    /**
     * Sets the buffer size for the given notification represented by its notification identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to set the buffer size for.
     * @param bufferSize The buffer size to set.
     */
    public void setBufferSize(String notificationId, int bufferSize) {
        distrNotificationService.setBufferSize(notificationId, bufferSize);
    }

    /**
     * Removes a specified publisher identified by the notification identifier of the
     * {@link Notification}s it published, i.e. all stored information concerning this identifier
     * like {@link Notification}s, {@link NotificationSubscriber} and so on are deleted. All
     * subscribers for this {@link Notification} will be notified that the publisher will no longer
     * publish new ones.
     * 
     * @param notificationId The identifier of the {@link Notification}s the publisher creates.
     */
    public void removePublisher(String notificationId) {
        distrNotificationService.removePublisher(notificationId);
    }

    /**
     * Sends a new {@link Notification}.
     * 
     * @param notificationId The identifier of the notification to send.
     * @param notificationBody The body of the notification to send.
     * @param <T> any {@link Object} that extends {@link Serializable}.
     */
    public <T extends Serializable> void send(String notificationId, T notificationBody) {
        distrNotificationService.send(notificationId, notificationBody);
    }

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s
     * represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}.
     * @param publishPlatform The {@link NodeIdentifier} of the corresponding publisher.
     *        <code>null</code> if local.
     * @return the number of the last notification, which was sent and missed by the new
     *         {@link NotificationSubscriber} sorted by the matching notification identifier.
     */
    public Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber, NodeIdentifier publishPlatform) {
        return distrNotificationService.subscribe(notificationId, subscriber, publishPlatform);
    }

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s
     * represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}.
     *        <code>null</code> if local.
     * @return the number of the last notification, which was sent and missed by the new
     *         {@link NotificationSubscriber} sorted by the matching notification identifier.
     */
    public Map<NodeIdentifier, Map<String, Long>> subscribeToAllReachableNodes(String notificationId,
        NotificationSubscriber subscriber) {
        return distrNotificationService.subscribeToAllReachableNodes(notificationId, subscriber);
    }

    /**
     * Unregisters the specified {@link NotificationSubscriber} so it will no longer receive
     * {@link Notification}s represented by the given identifier.
     * 
     * @param notificationId The identifier of the notification associated with the corresponding
     *        publisher.
     * @param subscriber The {@link NotificationSubscriber} to remove.
     * @param publishPlatform The {@link NodeIdentifier} of the corresponding publisher.
     *        <code>null</code> if local.
     */
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber, NodeIdentifier publishPlatform) {
        distrNotificationService.unsubscribe(notificationId, subscriber, publishPlatform);
    }

    /**
     * Returns the {@link NotificationHeader} of all stored {@link Notification}s represented by the
     * given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the {@link Notification}
     *        to get the {@link NotificationHeader} for.
     * @param publishPlatform The {@link NodeIdentifier} of the corresponding publisher.
     *        <code>null</code> if local.
     * @return the {@link NotificationHeader} sorted by the matching notification identifier.
     */
    public Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationId, NodeIdentifier publishPlatform) {
        return distrNotificationService.getNotificationHeaders(notificationId, publishPlatform);
    }

    /**
     * Returns all stored {@link Notification}s represented by the given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the {@link Notification}
     *        to get the {@link NotificationHeader} for (can be regEx).
     * @param publishPlatform The {@link NodeIdentifier} of the corresponding publisher.
     *        <code>null</code> if local.
     * @return the {@link Notification}s sorted by the matching notification identifier.
     */
    public Map<String, List<Notification>> getNotifications(String notificationId, NodeIdentifier publishPlatform) {
        return distrNotificationService.getNotifications(notificationId, publishPlatform);
    }

    /**
     * Returns the {@link Notification} belonging to the given {@link NotificationHeader}.
     * 
     * @param header The {@link NotificationHeader} of the {@link Notification} to get.
     * @return the {@link Notification}.
     */
    public Notification getNotification(NotificationHeader header) {
        return distrNotificationService.getNotification(header);
    }

}
