/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationHeader;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.NotificationTestConstants;
import de.rcenvironment.core.notification.api.RemotableNotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test cases for {@link DistributedNotificationServiceImpl}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (adapted for 7.0.0; 8.0.0 id adaptations)
 */
public class DistributedNotificationServiceTest {

    private static final String EXPECTED_EXCEPTION = "Expected exception because of DummyRemoteNotificationService";

    private static final String EXPECTED_EXCEPTION_FAILING = "Expected exception not thrown by DummyRemoteNotificationService";

    private DistributedNotificationServiceImpl notificationService;

    private Notification remoteNotification = new Notification("identifier", 0, NotificationTestConstants.REMOTE_INSTANCE_SESSION,
        new String());

    private Notification anotherRemoteNotification = new Notification("id", 0, NotificationTestConstants.REMOTE_INSTANCE_SESSION,
        new String());

    private Map<String, List<Notification>> notifications = new HashMap<String, List<Notification>>();

    private Map<String, SortedSet<NotificationHeader>> headers = new HashMap<String, SortedSet<NotificationHeader>>();

    private Map<String, List<Notification>> remoteNotifications = new HashMap<String, List<Notification>>();;

    private Map<String, SortedSet<NotificationHeader>> remoteHeaders = new HashMap<String, SortedSet<NotificationHeader>>();

    private BundleContext context = EasyMock.createNiceMock(BundleContext.class);

    /** Inject the notification service before the test methods run. */
    @Before
    public void initialize() {
        notificationService = new DistributedNotificationServiceImpl();
        notificationService.bindNotificationService(new DummyLocalNotificationService());
        notificationService.bindCommunicationService(new DummyCommunicationService());
        notificationService.activate(context);
    }

    /** Test. */
    @Test
    public void testRemovePublisher() {
        try {
            notificationService.removePublisher(NotificationTestConstants.NOTIFICATION_ID);
        } catch (RuntimeException e) {
            assertEquals("deregistered", e.getMessage());
        }
    }

    /** Test. */
    @Test
    public void testSetBufferSize() {

        try {
            notificationService.setBufferSize(NotificationTestConstants.NOTIFICATION_ID, 0);
        } catch (RuntimeException e) {
            assertEquals("registered with buffer", e.getMessage());
        }
    }

    /**
     * Test.
     * 
     * @throws RemoteOperationException on error
     */
    @Test
    public void testGetNotifications() throws RemoteOperationException {
        assertEquals(notifications, notificationService.getNotifications(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION));

        assertEquals(remoteNotifications,
            notificationService.getNotifications(remoteNotification.getHeader().getNotificationIdentifier(),
                NotificationTestConstants.REMOTE_INSTANCE_SESSION));

        try {
            notificationService.getNotifications(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.REMOTE_INSTANCE_SESSION);
            fail(EXPECTED_EXCEPTION_FAILING);
        } catch (IllegalStateException e) {
            assertTrue(EXPECTED_EXCEPTION, true);
        }
    }

    /** Test. */
    @Test
    public void testSend() {
        try {
            notificationService.send(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.NOTIFICATION.getBody());
        } catch (RuntimeException e) {
            assertEquals("sent", e.getMessage());
        }
    }

    /**
     * Test.
     * 
     * @throws RemoteOperationException on unexpected failure
     */
    @Test
    public void testSubscribe() throws RemoteOperationException {
        assertNotNull(notificationService.subscribe(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_SUBSCRIBER,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION));
    }

    /**
     * Test.
     */
    @Test
    public void testUnsubscribeFailureWithoutSubscription() {
        try {
            notificationService.unsubscribe(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.NOTIFICATION_SUBSCRIBER,
                NotificationTestConstants.LOCAL_INSTANCE_SESSION);
            fail("Expected exception");
        } catch (RemoteOperationException e) {
            final String expectedMessage =
                "Failed to unsubscribe from publisher \"local\" ["
                    + NotificationTestConstants.LOCAL_INSTANCE_SESSION.getInstanceNodeSessionIdString()
                    + "]: unsubscribed";
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Test {@link NotificationService}.
     * 
     * TODO Don't throw exception for test purposes. - seid_do
     * 
     * @author Doreen Seider
     */
    class DummyLocalNotificationService implements NotificationService {

        @Override
        public void removePublisher(String notificationIdentifier) {
            throw new RuntimeException("deregistered");
        }

        @Override
        public Notification getNotification(NotificationHeader header) {
            if (header.equals(NotificationTestConstants.NOTIFICATION_HEADER)) {
                return NotificationTestConstants.NOTIFICATION;
            } else {
                return null;
            }
        }

        @Override
        public Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationIdentifier) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)) {
                return headers;
            } else {
                return null;
            }
        }

        @Override
        public Map<String, List<Notification>> getNotifications(String notificationIdentifier) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)) {
                return notifications;
            } else {
                return null;
            }
        }

        @Override
        public void setBufferSize(String notificationIdentifier, int buffer) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID) && buffer == 0) {
                throw new RuntimeException("registered with buffer");
            }
        }

        @Override
        public void send(String notificationIdentifier, Serializable notificationBody) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)
                && notificationBody.equals(NotificationTestConstants.NOTIFICATION.getBody())) {
                throw new RuntimeException("sent");
            }
        }

        @Override
        public Map<String, Long> subscribe(String notificationIdentifier, NotificationSubscriber subscriber) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)
                && subscriber.equals(NotificationTestConstants.NOTIFICATION_SUBSCRIBER)) {
                return new HashMap<String, Long>();
            }
            return null;
        }

        @Override
        public void unsubscribe(String notificationIdentifier, NotificationSubscriber subscriber) {
            if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)
                && subscriber.equals(NotificationTestConstants.NOTIFICATION_SUBSCRIBER)) {
                throw new RuntimeException("unsubscribed");
            }
        }

    }

    /**
     * Test {@link NotificationService} implementation.
     * 
     * @author Doreen Seider
     */
    class DummyRemoteNotificationService implements NotificationService {

        @Override
        public void removePublisher(String notificationIdentifier) {}

        @Override
        public Notification getNotification(NotificationHeader header) {
            if (header.equals(remoteNotification.getHeader())) {
                return remoteNotification;
            } else if (header.equals(anotherRemoteNotification.getHeader())) {
                throw new UndeclaredThrowableException(new RuntimeException());
            } else {
                return null;
            }
        }

        @Override
        public Map<String, SortedSet<NotificationHeader>> getNotificationHeaders(String notificationIdentifier) {
            if (notificationIdentifier.equals(remoteNotification.getHeader().getNotificationIdentifier())) {
                return remoteHeaders;
            } else if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)) {
                throw new UndeclaredThrowableException(new RuntimeException());
            } else {
                return null;
            }
        }

        @Override
        public Map<String, List<Notification>> getNotifications(String notificationIdentifier) {
            if (notificationIdentifier.equals(remoteNotification.getHeader().getNotificationIdentifier())) {
                return remoteNotifications;
            } else if (notificationIdentifier.equals(NotificationTestConstants.NOTIFICATION_ID)) {
                throw new IllegalStateException();
            } else {
                return null;
            }
        }

        @Override
        public void setBufferSize(String notificationIdentifier, int buffer) {}

        @Override
        public void send(String notificationIdentifier, Serializable notificationBody) {}

        @Override
        public Map<String, Long> subscribe(String notificationIdentifier, NotificationSubscriber subscriber) {
            Map<String, Long> numbers = new HashMap<String, Long>();
            numbers.put(notificationIdentifier, new Long(5));
            return numbers;
        }

        @Override
        public void unsubscribe(String notificationIdentifier, NotificationSubscriber subscriber) {}

    }

    /**
     * Test {@link CommunicationService} implementation.
     * 
     * @author Doreen Seider
     * @author Robert Mischke (adapted for 7.0.0)
     */
    class DummyCommunicationService extends CommunicationServiceDefaultStub {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getRemotableService(Class<T> iface, NetworkDestination nodeId) {
            if (iface == RemotableNotificationService.class
                && nodeId.equals(NotificationTestConstants.LOCAL_INSTANCE_SESSION)) {
                return (T) new DummyLocalNotificationService();
            } else if (iface == RemotableNotificationService.class
                && nodeId.equals(NotificationTestConstants.REMOTE_INSTANCE_SESSION)) {
                return (T) new DummyRemoteNotificationService();
            } else {
                return null;
            }
        }

    }

}
