/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Supportive service for accessing the {@link NotificationService} without handling remote stuff.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
// TODO rename to match new 7.0.0 naming conventions
public interface DistributedNotificationService {

    /**
     * Sets the buffer size for the given notification represented by its notification identifier.
     * 
     * @param notificationIdentifier The identifier of the {@link Notification}s to set the buffer size for.
     * @param bufferSize The buffer size to set.
     */
    void setBufferSize(String notificationIdentifier, int bufferSize);

    /**
     * Removes a specified publisher identified by the notification identifier of the {@link Notification}s it published, i.e. all stored
     * information concerning this identifier like {@link Notification}s, {@link NotificationSubscriber} and so on are deleted. All
     * subscribers for this {@link Notification} will be notified that the publisher will no longer publish new ones.
     * 
     * @param notificationIdentifier The identifier of the {@link Notification}s the publisher creates.
     */
    void removePublisher(String notificationIdentifier);

    /**
     * Sends a new {@link Notification}.
     * 
     * @param notificationId The identifier of the notification to send.
     * @param notificationBody The body of the notification to send.
     * @param <T> any {@link Object} that extends {@link Serializable}.
     */
    <T extends Serializable> void send(String notificationId, T notificationBody);

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}.
     * @param publisherPlatform The {@link InstanceNodeSessionId} of the corresponding publisher. <code>null</code> if local.
     * @return the number of the last notification, which was sent and missed by the new {@link NotificationSubscriber} sorted by the
     *         matching notification identifier.
     * 
     * @throws RemoteOperationException if the remote subscription fails
     */
    Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber, ResolvableNodeId publisherPlatform)
        throws RemoteOperationException;

    /**
     * Unregisters the specified {@link NotificationSubscriber} so it will no longer receive {@link Notification}s represented by the given
     * identifier.
     * 
     * @param notificationId The identifier of the notification associated with the corresponding publisher.
     * @param subscriber The {@link NotificationSubscriber} to remove.
     * @param publishPlatform The {@link InstanceNodeSessionId} of the corresponding publisher. <code>null</code> if local.
     * 
     * @throws RemoteOperationException if the remote subscription cancellation fails
     */
    void unsubscribe(String notificationId, NotificationSubscriber subscriber, ResolvableNodeId publishPlatform)
        throws RemoteOperationException;

    /**
     * Returns all stored {@link Notification}s represented by the given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the {@link Notification} to get.
     * @param publishPlatform The {@link InstanceNodeSessionId} of the corresponding publisher. <code>null</code> if local.
     * @return the {@link Notification}s sorted by the matching notification identifier.
     * @throws RemoteOperationException TODO
     */
    Map<String, List<Notification>> getNotifications(String notificationId, ResolvableNodeId publishPlatform)
        throws RemoteOperationException;

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}. <code>null</code> if local.
     * @return the number of the last notification, which was sent and missed by the new {@link NotificationSubscriber} sorted by the
     *         matching notification identifier.
     */
    Map<InstanceNodeSessionId, Map<String, Long>> subscribeToAllReachableNodes(String notificationId, NotificationSubscriber subscriber);
}
