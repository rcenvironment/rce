/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification.testutils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Common test/mock implementations of {@link CommunicationService}. These can be used directly, or can as superclasses for custom mock
 * classes.
 * 
 * Custom mock implementations of {@link CommunicationService} should use these as superclasses whenever possible to avoid code duplication,
 * and to shield the mock classes from irrelevant API changes.
 * 
 * @author Doreen Seider
 */
public class MockDistributedNotificationService implements DistributedNotificationService {

    @Override
    public void setBufferSize(String notificationIdentifier, int bufferSize) {}

    @Override
    public void removePublisher(String notificationIdentifier) {}

    @Override
    public <T extends Serializable> void send(String notificationId, T notificationBody) {}

    @Override
    public Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber, ResolvableNodeId publisherPlatform) {
        return null;
    }

    @Override
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber, ResolvableNodeId publishPlatform) {}

    @Override
    public Map<String, List<Notification>> getNotifications(String notificationId, ResolvableNodeId publishPlatform)
        throws RemoteOperationException {
        return null;
    }

    @Override
    public Map<InstanceNodeSessionId, Map<String, Long>> subscribeToAllReachableNodes(
        String notificationId, NotificationSubscriber subscriber) {
        return null;
    }

}
