/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification.testutils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;

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
    public Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber, NodeIdentifier publisherPlatform) {
        return null;
    }

    @Override
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber, NodeIdentifier publishPlatform) {}

    @Override
    public Map<String, List<Notification>> getNotifications(String notificationId, NodeIdentifier publishPlatform) {
        return null;
    }

    @Override
    public Map<NodeIdentifier, Map<String, Long>> subscribeToAllReachableNodes(String notificationId, NotificationSubscriber subscriber) {
        return null;
    }

}
