/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
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

/**
 * This interface describes methods provided by the notification service. These methods allow to
 * publish new notifications, subscribe for certain notifications and fetch these notifications.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public interface NotificationService {

    /** Constant representing that no notification was missed. */
    int NO_MISSED = -1;

    /**
     * Sets the buffer size for the given notification represented by its notification identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to set the buffer
     *        size for.
     * @param bufferSize The buffer size to set.
     */
    void setBufferSize(String notificationId, int bufferSize);
    
    /**
     * Removes a specified publisher identified by the notification identifier of the
     * {@link Notification}s it published, i.e. all stored information concerning this identifier
     * like {@link Notification}s, {@link NotificationSubscriber} and so on are deleted. All
     * subscribers for this {@link Notification} will be notified that the publisher will no longer
     * publish new ones.
     * 
     * @param notificationId The identifier of the {@link Notification}s the publisher
     *        creates.
     */
    void removePublisher(String notificationId);

    /**
     * Sends a new {@link Notification}. If it is the first notification sent under the given
     * identifier a new publisher will be registered internally.
     * 
     * @param notificationId The identifier of the notification to send.
     * @param notificationBody The body of the notification to send.
     * @param <T> any {@link Object} that extends {@link Serializable}.
     */
    <T extends Serializable> void send(String notificationId, T notificationBody);

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s
     * represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}.
     *        <code>null</code> if local.
     * @return the edition number of the last notification, which was sent and missed by the new {@link NotificationSubscriber}.
     */
    Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber);

    /**
     * Unregisters the specified {@link NotificationSubscriber} so it will no longer receive
     * {@link Notification}s represented by the given identifier.
     * 
     * @param notificationId The identifier of the notification associated with the
     *        corresponding publisher.
     * @param subscriber The {@link NotificationSubscriber} to remove.
     *        <code>null</code> if local.
     */
    void unsubscribe(String notificationId, NotificationSubscriber subscriber);

    /**
     * Returns the {@link NotificationHeader} of all stored {@link Notification}s represented by the
     * given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the
     *        {@link Notification} to get the {@link NotificationHeader} for.
     * @return the {@link NotificationHeader}.
     */
    Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationId);

    /**
     * Returns all stored {@link Notification}s represented by the given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the
     *        {@link Notification} to get the {@link NotificationHeader} for.
     * @return the {@link Notification}s.
     */
    Map<String, List<Notification>> getNotifications(String notificationId);

    /**
     * Returns the {@link Notification} belonging to the given {@link NotificationHeader}.
     * 
     * @param header The {@link NotificationHeader} of the {@link Notification} to get.
     * @return the {@link Notification}.
     */
    Notification getNotification(NotificationHeader header);

}
