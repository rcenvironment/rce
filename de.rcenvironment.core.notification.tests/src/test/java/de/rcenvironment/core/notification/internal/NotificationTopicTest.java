/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import java.util.Set;

import junit.framework.TestCase;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationHeader;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.NotificationTestConstants;

/**
 * Test cases for the class {@link NotificationTopic}.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class NotificationTopicTest extends TestCase {

    /**
     * A text for a notification.
     */
    private static final String NOTIFICATION_TEXT = "This is a notification for the tests.";
    
    /**
     * The class under test.
     */
    private NotificationTopic myPublisher = null;

    /**
     * A publisher name for the tests.
     */
    private String myPublisherName = null;

    /**
     * A notification for the tests.
     */
    private Notification myNotification = null;

    /**
     * A header for the tests.
     */
    private NotificationHeader myHeader = null;

    /**
     * A subscriber for the tests.
     */
    private NotificationSubscriber myNotificationSubscriber = null;

    /**
     * Initializes the test class.
     * 
     * @throws Exception if an error occur.
     */
    public NotificationTopicTest() throws Exception {
        myPublisherName = NotificationTestConstants.NOTIFICATION_ID;
        myNotification = new Notification(myPublisherName,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            NOTIFICATION_TEXT);
        myHeader = myNotification.getHeader();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myPublisher = new NotificationTopic(myPublisherName);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myPublisher = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * Test if the method can be called.
     */
    public final void testAddForSuccess() {
        myPublisher.add(myNotificationSubscriber);
    }

    /**
     * Test if the method can be called.
     */
    public final void testRemoveForSuccess() {
        myPublisher.remove(myNotificationSubscriber);
    }

    /**
     * Test if the method can be called.
     */
    public final void testGetNameForSuccess() {
        myPublisher.getName();
    }

    /**
     * Test if the method can be called.
     */
    public final void testGetSubscriberForSuccess() {
        myPublisher.getSubscribers();
    }

    /**
     * Test if the method can be called.
     */
    public final void testEqualsForSuccess() {
        NotificationTopic publisher = new NotificationTopic(myPublisherName);

        myPublisher.equals(publisher);
        myPublisher.equals(myHeader);
    }

    /**
     * Test if the method can be called.
     */
    public final void testHashCodeForSuccess() {
        NotificationTopic publisher = new NotificationTopic(myPublisherName);
        publisher.hashCode();
    }

    /*
     * #################### Test for sanity ####################
     */

    /**
     * Test the name from the associated publisher can be obtained.
     */
    public final void testGetNameForSanity() {
        String name = myPublisher.getName();

        assertNotNull(name);
        assertEquals(name, myPublisherName);
    }

    /**
     * Test if the local subscribers can be obtained.
     */
    public final void testGetLocalSubscriberForSanity() {
        myPublisher.add(myNotificationSubscriber);
        Set<NotificationSubscriber> subscribers = myPublisher.getSubscribers();

        assertNotNull(subscribers);
        assertEquals(1, subscribers.size());

        myPublisher.remove(myNotificationSubscriber);
        subscribers = myPublisher.getSubscribers();

        assertNotNull(subscribers);
        assertEquals(0, subscribers.size());
    }

    /**
     * Test if two publishers are equal.
     */
    public final void testEqualsForSanity() {
        NotificationTopic publisher = new NotificationTopic(myPublisherName);
        assertTrue(myPublisher.equals(publisher));
        publisher = new NotificationTopic("myPublisherName");
        assertFalse(myPublisher.equals(publisher));
    }

}
