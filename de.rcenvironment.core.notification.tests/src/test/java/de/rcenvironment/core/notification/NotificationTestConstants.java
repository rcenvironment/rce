/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierTestUtils;

/**
 * Test constants for this bundle tests.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
public final class NotificationTestConstants {

    /** Local Host. */
    public static final InstanceNodeSessionId LOCAL_INSTANCE_SESSION = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("local");

    /** Remote Host. */
    public static final InstanceNodeSessionId REMOTE_INSTANCE_SESSION = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("remote");

    /** Local Host. */
    public static final InstanceNodeSessionId UNREACHABLE_INSTANCE = NodeIdentifierTestUtils
        .createTestInstanceNodeSessionIdWithDisplayName("unreachable");

    /** Notification identifier. */
    public static final String PERSISTENT_NOTIFICATION_ID = "de.rcenvironment.rce.notification.persistent";

    /** Notification identifier. */
    public static final String ANOTHER_PERSISTENT_NOTIFICATION_ID = "de.rcenvironment.rce.notification.persistent.another";

    /** Notification identifier. */
    public static final String PERSISTENT_NOTIFICATION_REGEX = "de.rcenvironment.rce.notification.pers.*";

    /** Notification identifier of a persistent notification. */
    public static final String NOTIFICATION_ID = "de.rcenvironment.rce.notification";

    /** Other publisher name. */
    public static final String OTHER_NOTIFICATION_IDENTIFIER = "de.rcenvironment.rce.notification.other";

    /** Subscriber identifier. */
    public static final String SUBSCRIBER_IDENTIFIER = "de.rcenvironment.rce.notification.subscriber";

    /** Publisher name. */
    public static final long NOTIFICATION_EDITION = 0;

    /** Notification header. */
    public static final NotificationHeader NOTIFICATION_HEADER = new NotificationHeader(NOTIFICATION_ID,
        NOTIFICATION_EDITION, LOCAL_INSTANCE_SESSION);

    /** Notification headers. */
    public static final SortedSet<NotificationHeader> NOTIFICATION_HEADERS = new TreeSet<NotificationHeader>();

    /** Notifications. */
    public static final List<Notification> NOTIFICATIONS = new ArrayList<Notification>();

    /** Notification subscriber. */
    public static final NotificationSubscriber NOTIFICATION_SUBSCRIBER = new DefaultNotificationSubscriber() {

        private static final long serialVersionUID = 1L;

        @Override
        public void processNotification(Notification notification) {}

        @Override
        public Class<? extends Serializable> getInterface() {
            return NotificationSubscriber.class;
        }
    };

    /** Notification. */
    public static final Notification NOTIFICATION = new Notification(NOTIFICATION_ID,
        NOTIFICATION_EDITION, LOCAL_INSTANCE_SESSION, new String());

    /**
     * Private constructor of this utility class.
     */
    private NotificationTestConstants() {

    }

}
