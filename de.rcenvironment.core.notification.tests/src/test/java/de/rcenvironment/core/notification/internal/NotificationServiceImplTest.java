/*
 * Copyright 2006-2022 DLR, Germany
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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationHeader;
import de.rcenvironment.core.notification.NotificationMockFactory;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.NotificationTestConstants;

// TODO there seem to be no explicit tests for multi-threaded usage, which is problematic; consider 
// adding them, if it's still worth it before we replace this code completely -- misc_ro

/**
 * Test cases for the class {@link NotificationServiceImpl}.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
@SuppressWarnings("serial")
public class NotificationServiceImplTest {

    private static final long SLEEP = 1000L;

    private static final String NOTIFICATION_TEXT = "This is a notification for the tests.";

    private NotificationServiceImpl notificationService = null;

    private String notificationId = null;

    private InstanceNodeSessionId myPublisherPlatform = null;

    private String myOtherPublisherName = null;

    private NotificationSubscriber notificationSubscriber = null;

    /** A counter to indicate if a notification subscriber has been notified. */
    private int myCounter = 0;

    /**
     * Creates and initializes objects used for the tests.
     * 
     * @throws Exception if an error occur.
     */
    public NotificationServiceImplTest() throws Exception {

        notificationId = NotificationTestConstants.NOTIFICATION_ID;
        myPublisherPlatform = NotificationTestConstants.LOCAL_INSTANCE_SESSION;
        myOtherPublisherName = NotificationTestConstants.OTHER_NOTIFICATION_IDENTIFIER;
        notificationSubscriber = new DefaultNotificationSubscriber() {

            @Override
            public void processNotification(Notification n) {}

            @Override
            public Class<? extends Serializable> getInterface() {
                return NotificationSubscriber.class;
            }
        };
    }

    /** Set up. */
    @Before
    public void setUp() {
        notificationService = new NotificationServiceImpl();
        notificationService.bindPlatformService(NotificationMockFactory.getInstance().getPlatformServiceMock());
        myCounter = 0;
    }

    /** Tear down. */
    @After
    public void tearDown() {
        notificationService = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * Test if the method can be called.
     */
    @Test
    public final void testRegisterPublisherForSuccess() {
        notificationService.setBufferSize(notificationId, 9);
    }

    /**
     * Test if the method can be called.
     */
    @Test
    public final void testDeregisterPublisherForSuccess() {
        notificationService.removePublisher(notificationId);
    }

    /**
     * Test if the method can be called.
     */
    @Test
    public final void testSendForSuccess() {
        notificationService.send(notificationId, NOTIFICATION_TEXT);
    }

    /**
     * Test if the method can be called.
     */
    @Test
    public final void testSubscribeForSuccess() {
        final Long noNotificationsSentYet = new Long(-1);
        assertEquals(noNotificationsSentYet, notificationService.subscribe(notificationId, notificationSubscriber).get(notificationId));
        notificationService.send(notificationId, NOTIFICATION_TEXT);

        notificationService.awaitAsyncTaskCompletion();

        assertEquals(new Long(0), notificationService.subscribe(notificationId, notificationSubscriber).get(notificationId));

        notificationService.subscribe(notificationId, notificationSubscriber);
        notificationService.subscribe(notificationId, notificationSubscriber);
    }

    /**
     * Test if the method can be called.
     */
    @Test
    public final void testUnsubscribeForSuccess() {
        notificationService.unsubscribe(notificationId, notificationSubscriber);
        notificationService.unsubscribe(notificationId, notificationSubscriber);
    }

    /*
     * #################### Test for failure ####################
     */

    /**
     * Test if an {@link IllegalArgumentException} is thrown when using illegal arguments.
     */
    @Test
    public final void testDeregisterPublisherForFailure() {
        notificationService.removePublisher(myOtherPublisherName);
    }

    /**
     * Test if an {@link IllegalArgumentException} is thrown when using illegal arguments.
     */
    @Test
    public final void testSendForFailure() {
        notificationService.removePublisher(notificationId);
        notificationService.send(notificationId, NOTIFICATION_TEXT);

    }

    /**
     * Test if an {@link IllegalArgumentException} is thrown when using illegal arguments.
     */
    @Test
    public final void testUnsubscribeForFailure() {
        notificationService.unsubscribe("myPublisherName", notificationSubscriber);

        notificationService.removePublisher(notificationId);
        notificationService.unsubscribe(notificationId, notificationSubscriber);

    }

    /**
     * Test method.
     */
    @Test
    public final void testSendForSanity() {

        final int unlimitedBufferSize = -1;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        final int numberOfNotifications = 20;

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        Map<String, SortedSet<NotificationHeader>> headers = notificationService
            .getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);

        assertEquals(1, headers.size());
        assertEquals(numberOfNotifications, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());

        notificationService.setBufferSize(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        final int anotherNumberOfNotifications = 10;
        for (int i = 0; i < anotherNumberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID,
                new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_REGEX);

        assertEquals(2, headers.size());
        assertEquals(numberOfNotifications, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(anotherNumberOfNotifications, headers.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).size());

        notificationService.removePublisher(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);
        notificationService.removePublisher(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID);

        final int limitedBufferSize = 10;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, limitedBufferSize);

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);
        // TODO (p2) investigate: this check fails when using asynchronous sending, despite the awaitAsyncTaskCompletion() above
        assertEquals(1, headers.size());
        assertEquals(limitedBufferSize, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(10, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).first().getNumber());

        notificationService.removePublisher(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);

        final int noBuffer = 0;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, noBuffer);

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);
        assertEquals(0, headers.size());

    }

    /**
     * Test if a subscriber gets a message.
     * 
     * @throws InterruptedException if an exception occurs.
     */
    @Test
    public final void testSubscribeForSanity() throws InterruptedException {

        NotificationSubscriber subscriber = new DefaultNotificationSubscriber() {

            @Override
            public void processNotification(Notification notification) {
                assertNotNull(notification);
                assertEquals(notification.getHeader().getNotificationIdentifier(), notificationId);
                assertEquals(notification.getHeader().getPublishPlatform(), myPublisherPlatform);
                myCounter++;
            }

            @Override
            public Class<? extends Serializable> getInterface() {
                return NotificationSubscriber.class;
            }
        };
        notificationService.subscribe(notificationId, subscriber);
        notificationService.send(notificationId, NOTIFICATION_TEXT);
        notificationService.awaitAsyncTaskCompletion();

        Thread.sleep(SLEEP);
        assertEquals(1, myCounter);

        notificationService.subscribe(notificationId, subscriber);
        notificationService.send(notificationId, NOTIFICATION_TEXT);
        notificationService.awaitAsyncTaskCompletion();

        Thread.sleep(SLEEP);
        assertEquals(2, myCounter);

        notificationService.subscribe(notificationId, subscriber);
        notificationService.subscribe(notificationId, subscriber);

        notificationService.removePublisher(notificationId);
        notificationService.awaitAsyncTaskCompletion();

        // TODO what does this test, exactly?
        notificationService.subscribe(notificationId, notificationSubscriber);
    }

    /**
     * Test if a subscriber gets a message.
     */
    @Test
    public final void testUnsubscribeForSanity() {
        NotificationSubscriber subscriber = new DefaultNotificationSubscriber() {

            @Override
            public void processNotification(Notification notification) {
                myCounter++;
                fail("This subscriber should not get a notification after beeing unsubscribed!");
            }

            @Override
            public Class<? extends Serializable> getInterface() {
                return NotificationSubscriber.class;
            }
        };
        notificationService.subscribe(notificationId, subscriber);
        notificationService.unsubscribe(notificationId, subscriber);
        notificationService.send(notificationId, NOTIFICATION_TEXT);
        notificationService.awaitAsyncTaskCompletion();

        assertEquals(0, myCounter);

        // TODO what does this test, exactly?
        notificationService.subscribe(notificationId, subscriber);
        notificationService.unsubscribe(notificationId, subscriber);
    }

    /**
     * Test method.
     */
    @Test
    public final void testGetNotificationHeaders() {
        final int unlimitedBufferSize = -1;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        final int numberOfNotifications = 7;

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.NOTIFICATION_ID, new Integer(1));
        }

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }

        notificationService.awaitAsyncTaskCompletion();

        Map<String, SortedSet<NotificationHeader>> headers = notificationService
            .getNotificationHeaders(NotificationTestConstants.NOTIFICATION_ID);
        assertTrue(headers.isEmpty());

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);

        assertEquals(1, headers.size());
        assertEquals(numberOfNotifications, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(0, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).first().getNumber());
        assertEquals(numberOfNotifications - 1, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).last().getNumber());

        notificationService.setBufferSize(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);
        final int anotherNumberOfNotifications = 9;

        for (int i = 0; i < anotherNumberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_REGEX);

        assertEquals(2, headers.size());

        assertEquals(numberOfNotifications, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(0, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).first().getNumber());
        assertEquals(numberOfNotifications - 1, headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).last().getNumber());

        assertEquals(anotherNumberOfNotifications, headers.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(0, headers.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).first().getNumber());
        assertEquals(anotherNumberOfNotifications - 1,
            headers.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).last().getNumber());
    }

    /**
     * Test method.
     */
    @Test
    public final void testGetNotification() {
        final int unlimitedBufferSize = -1;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        Integer object = new Integer(1);
        notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, object);
        notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));

        notificationService.awaitAsyncTaskCompletion();

        Map<String, SortedSet<NotificationHeader>> headers = notificationService
            .getNotificationHeaders(NotificationTestConstants.NOTIFICATION_ID);
        assertTrue(headers.isEmpty());

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);

        assertEquals(1, headers.size());
        Notification notification = notificationService
            .getNotification(headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).first());

        assertNotNull(notification);
        assertEquals(object, notification.getBody());
        assertEquals(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, notification.getHeader().getNotificationIdentifier());

        notificationService.setBufferSize(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);
        String anotherObject = new String();
        notificationService.send(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, anotherObject);

        notificationService.awaitAsyncTaskCompletion();

        headers = notificationService.getNotificationHeaders(NotificationTestConstants.PERSISTENT_NOTIFICATION_REGEX);

        assertEquals(2, headers.size());

        notification = notificationService.getNotification(headers.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).first());
        assertEquals(object, notification.getBody());
        assertEquals(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, notification.getHeader().getNotificationIdentifier());

        notification = notificationService
            .getNotification(headers.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).first());
        assertEquals(anotherObject, notification.getBody());
        assertEquals(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, notification.getHeader().getNotificationIdentifier());
    }

    /** Test. */
    @Test
    public final void testGetNotifications() {
        final int unlimitedBufferSize = -1;
        notificationService.setBufferSize(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        final int numberOfNotifications = 7;

        for (int i = 0; i < numberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        Map<String, List<Notification>> notifications = notificationService
            .getNotifications(NotificationTestConstants.NOTIFICATION_ID);

        assertTrue(notifications.isEmpty());

        notifications = notificationService.getNotifications(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID);

        assertEquals(1, notifications.size());
        assertEquals(numberOfNotifications, notifications.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());

        notificationService.setBufferSize(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, unlimitedBufferSize);

        final int anotherNumberOfNotifications = 9;

        for (int i = 0; i < anotherNumberOfNotifications; i++) {
            notificationService.send(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID, new Integer(1));
        }
        notificationService.awaitAsyncTaskCompletion();

        notifications = notificationService.getNotifications(NotificationTestConstants.PERSISTENT_NOTIFICATION_REGEX);

        assertEquals(2, notifications.size());

        assertEquals(numberOfNotifications, notifications.get(NotificationTestConstants.PERSISTENT_NOTIFICATION_ID).size());
        assertEquals(anotherNumberOfNotifications,
            notifications.get(NotificationTestConstants.ANOTHER_PERSISTENT_NOTIFICATION_ID).size());
    }

}
