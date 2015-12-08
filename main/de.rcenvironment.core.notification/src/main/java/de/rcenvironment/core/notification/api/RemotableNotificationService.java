/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification.api;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote-accessible methods for notification management.
 * 
 * @author Robert Mischke (extracted from {@link NotificationService})
 */
@RemotableService
public interface RemotableNotificationService {

    /**
     * Registers the specified {@link NotificationSubscriber} to receive {@link Notification}s represented by the given identifier.
     * 
     * @param notificationId The identifier of the {@link Notification}s to receive.
     * @param subscriber The {@link NotificationSubscriber} for this {@link Notification}. <code>null</code> if local.
     * @return the edition number of the last notification, which was sent and missed by the new {@link NotificationSubscriber}.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber) throws RemoteOperationException;

    /**
     * Unregisters the specified {@link NotificationSubscriber} so it will no longer receive {@link Notification}s represented by the given
     * identifier.
     * 
     * @param notificationId The identifier of the notification associated with the corresponding publisher.
     * @param subscriber The {@link NotificationSubscriber} to remove. <code>null</code> if local.
     * @throws RemoteOperationException standard remote operation exception
     */
    void unsubscribe(String notificationId, NotificationSubscriber subscriber) throws RemoteOperationException;

    /**
     * Returns all stored {@link Notification}s represented by the given notification identifier.
     * 
     * @param notificationId The notification identifier which represents the {@link Notification} to get the {@link NotificationHeader}
     *        for.
     * @return the {@link Notification}s.
     * @throws RemoteOperationException standard remote operation exception
     */
    Map<String, List<Notification>> getNotifications(String notificationId) throws RemoteOperationException;

}
