/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedSet;

import de.rcenvironment.core.notification.api.RemotableNotificationService;

/**
 * This interface describes methods provided by the notification service. These methods allow to publish new notifications, subscribe for
 * certain notifications and fetch these notifications.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
// TODO rename to match new 7.0.0 naming conventions
public interface NotificationService extends RemotableNotificationService {

    /** Constant representing that no notification was missed. */
    int NO_MISSED = -1;

    /**
     * Sets the buffer size for the given notification represented by its notification identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to set the buffer size for.
     * @param bufferSize The buffer size to set.
     */
    void setBufferSize(String notificationId, int bufferSize);

    /**
     * Removes a specified publisher identified by the notification identifier of the {@link Notification}s it published, i.e. all stored
     * information concerning this identifier like {@link Notification}s, {@link NotificationSubscriber} and so on are deleted. All
     * subscribers for this {@link Notification} will be notified that the publisher will no longer publish new ones.
     * 
     * @param notificationId The identifier of the {@link Notification}s the publisher creates.
     */
    void removePublisher(String notificationId);

    /**
     * Sends a new {@link Notification}. If it is the first notification sent under the given identifier a new publisher will be registered
     * internally.
     * 
     * @param notificationId The identifier of the notification to send.
     * @param notificationBody The body of the notification to send.
     * @param <T> any {@link Object} that extends {@link Serializable}.
     */
    <T extends Serializable> void send(String notificationId, T notificationBody);

    /**
     * Returns the {@link NotificationHeader} of all stored {@link Notification}s represented by the given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the {@link Notification} to get the {@link NotificationHeader}
     *        for.
     * @return the {@link NotificationHeader}.
     */
    Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationId);

    /**
     * Returns the {@link Notification} belonging to the given {@link NotificationHeader}.
     * 
     * @param header The {@link NotificationHeader} of the {@link Notification} to get.
     * @return the {@link Notification}.
     */
    Notification getNotification(NotificationHeader header);

}
