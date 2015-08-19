/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.testutils.CommunicationServiceDefaultStub;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationHeader;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.NotificationTestConstants;

/**
 * Test cases for {@link DistributedNotificationServiceImpl}.
 * 
 * @author Doreen Seider
 */
public class DistributedNotificationServiceTest {

    private static final String EXPECTED_EXCEPTION = "Expected exception because of DummyRemoteNotificationService";

    private static final String EXPECTED_EXCEPTION_FAILING = "Expected exception not thrown by DummyRemoteNotificationService";

    private DistributedNotificationServiceImpl notificationService;

    private Notification remoteNotification = new Notification("identifier", 0, NotificationTestConstants.REMOTEHOST, new String());

    private Notification anotherRemoteNotification = new Notification("id", 0, NotificationTestConstants.REMOTEHOST, new String());

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

    /** Test. */
    @Test
    public void testGetNotification() {
        assertEquals(NotificationTestConstants.NOTIFICATION,
            notificationService.getNotification(NotificationTestConstants.NOTIFICATION_HEADER));
        
        assertEquals(remoteNotification,
            notificationService.getNotification(remoteNotification.getHeader()));
        
        try {
            notificationService.getNotification(anotherRemoteNotification.getHeader());
            fail(EXPECTED_EXCEPTION_FAILING);
        } catch (IllegalStateException e) {
            assertTrue(EXPECTED_EXCEPTION, true);
        }
    }

    /** Test. */
    @Test
    public void testGetNotificationHeaders() {
        assertEquals(headers, notificationService.getNotificationHeaders(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.LOCALHOST));
        
        assertEquals(remoteHeaders,
            notificationService.getNotificationHeaders(remoteNotification.getHeader().getNotificationIdentifier(),
                NotificationTestConstants.REMOTEHOST));
        
        
        try {
            notificationService.getNotificationHeaders(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.REMOTEHOST);
            fail(EXPECTED_EXCEPTION_FAILING);
        } catch (IllegalStateException e) {
            assertTrue(EXPECTED_EXCEPTION, true);
        }
    }

    /** Test. */
    @Test
    public void testGetNotifications() {
        assertEquals(notifications, notificationService.getNotifications(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.LOCALHOST));
        
        assertEquals(remoteNotifications,
            notificationService.getNotifications(remoteNotification.getHeader().getNotificationIdentifier(),
                NotificationTestConstants.REMOTEHOST));
        
        try {
            notificationService.getNotifications(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.REMOTEHOST);
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
     * @throws CommunicationException on unexpected failure
     */
    @Test
    public void testSubscribe() throws CommunicationException {
        assertNotNull(notificationService.subscribe(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_SUBSCRIBER,
            NotificationTestConstants.LOCALHOST));
    }

    /**
     * Test.
     */
    @Test
    public void testUnsubscribeFailureWithoutSubscription() {
        try {
            notificationService.unsubscribe(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.NOTIFICATION_SUBSCRIBER,
                NotificationTestConstants.LOCALHOST);
            fail("Expected exception");
        } catch (CommunicationException e) {
            assertEquals("Failed to unsubscribe from remote publisher @\"<unnamed>\" [localhost:1]: unsubscribed", e.getMessage());
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
                throw new UndeclaredThrowableException(new RuntimeException());
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
     * @author Doreen Seider
     */
    class DummyCommunicationService extends CommunicationServiceDefaultStub {
        
        @Override
        public Object getService(Class<?> iface, NodeIdentifier nodeId, BundleContext bundleContext)
            throws IllegalStateException {
            
            if (iface == NotificationService.class
                && nodeId.equals(NotificationTestConstants.LOCALHOST)) {
                return new DummyLocalNotificationService();
            } else if (iface == NotificationService.class
                && nodeId.equals(NotificationTestConstants.REMOTEHOST)) {
                return new DummyRemoteNotificationService();
            } else {
                return null;
            }
        }

    }

}
