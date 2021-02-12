/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import junit.framework.TestCase;

/**
 * Test cases for {@link Notification}.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public class NotificationTest extends TestCase {

    /**
     * Constant for a string.
     */
    private static final String THROWABLE_TEXT = "This is an exception for the tests.";

    /**
     * The class under test.
     */
    private Notification myNotification = null;

    /**
     * A payload for the tests.
     */
    private Throwable myThrowable = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myThrowable = new NullPointerException(THROWABLE_TEXT);
        myNotification = new Notification(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            myThrowable);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myThrowable = null;
        myNotification = null;
    }

    /*
     * #################### Test for success ####################
     */

    /**
     * Test if the method can be called.
     */
    public final void testGetHeaderForSuccess() {
        myNotification.getHeader();
    }

    /**
     * Test if the method can be called.
     */
    public final void testGetBodyForSuccess() {
        myNotification.getBody();
    }

    /*
     * #################### Test for failure ####################
     */
    
    /**
     * Test if illegal arguments are handled correctly.
     */
    public void testCreationForFailure() {
        try {
            new Notification(null,
                NotificationTestConstants.NOTIFICATION_EDITION,
                NotificationTestConstants.LOCAL_INSTANCE_SESSION,
                myThrowable);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        final int illegalEdition = -2;
        try {
            new Notification(NotificationTestConstants.NOTIFICATION_ID,
                illegalEdition,
                NotificationTestConstants.LOCAL_INSTANCE_SESSION,
                myThrowable);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        try {
            new Notification(NotificationTestConstants.NOTIFICATION_ID,
                NotificationTestConstants.NOTIFICATION_EDITION,
                null,
                myThrowable);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
        new Notification(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            null);
    }

    /*
     * #################### Test for sanity ####################
     */

    /**
     * Test if the message contains the correct header.
     */
    public final void testGetHeaderForSanity() {
        NotificationHeader header = myNotification.getHeader();

        assertNotNull(header);
        assertEquals(NotificationTestConstants.NOTIFICATION_ID, header.getNotificationIdentifier());
        assertFalse(header.getNotificationIdentifier().equals(NotificationTestConstants.OTHER_NOTIFICATION_IDENTIFIER));
    }

    /**
     * Test if message contains the correct body.
     */
    public final void testGetBodyForSanity() {
        Object payload = myNotification.getBody();

        assertNotNull(payload);
        assertTrue(payload instanceof Throwable);
        assertEquals(myThrowable, payload);
        assertEquals(THROWABLE_TEXT, ((Throwable) payload).getMessage());
    }

    /**
     * Test if two equal headers are announced as equal.
     * 
     * @throws Exception if an error occur.
     */
    public final void testEqualsForSanity() throws Exception {
        Notification notification = new Notification(NotificationTestConstants.OTHER_NOTIFICATION_IDENTIFIER,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            myThrowable);
        assertFalse(myNotification.equals(notification));
        notification = new Notification(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            new String());
        assertFalse(myNotification.equals(notification));
        final int sleep = 1000;
        Thread.sleep(sleep);
        notification = new Notification(NotificationTestConstants.NOTIFICATION_ID,
            NotificationTestConstants.NOTIFICATION_EDITION,
            NotificationTestConstants.LOCAL_INSTANCE_SESSION,
            myThrowable);
        assertFalse(myNotification.equals(notification));

        assertFalse(myNotification.equals(new Object()));

    }

    /**
     * Test if two equal headers are announced as equal.
     */
    public final void testHashCodeForSanity() {
        assertEquals(myNotification.getHeader().hashCode() + myNotification.getBody().hashCode(), myNotification.hashCode());
    }
    
    /**
     * Test the toString() method.
     */
    public final void testToString() {
        assertEquals(myNotification.getHeader().toString() + "_" + myNotification.getBody().toString(), myNotification.toString());
    }

}
