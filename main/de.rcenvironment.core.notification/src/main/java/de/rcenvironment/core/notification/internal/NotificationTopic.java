/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.rcenvironment.core.notification.NotificationSubscriber;

/**
 * Objects of this class represent local notification topics used by the notification service. They
 * are created when a {@link Notification} concerning that topic is sent or an
 * {@link NotificationSubscriber} is registered for that topic. Local and remote
 * {@link NotificationSubscriber}s for this {@link NotificationTopic} are stored here.
 * 
 * @author Andre Nurzenski
 * @author Doreen Seider
 */
public class NotificationTopic {

    /** The identifier of this notification topic (can be an regEx). */
    private String notificationId;
    
    /** The cached, pre-compiled notification filter derived from the notificationId field. */  
    private Pattern compiledRegExp;

    private Set<NotificationSubscriber> subscribers = Collections.synchronizedSet(new HashSet<NotificationSubscriber>());

    protected NotificationTopic(String notificationIdentifier) {
        notificationId = notificationIdentifier;
    }

    /**
     * Adds a local {@link NotificationSubscriber} to this {@link NotificationTopic}.
     * 
     * @param subscriber The {@link NotificationSubscriber} to add.
     */
    protected synchronized void add(NotificationSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Removes a local {@link NotificationSubscriber} from this {@link NotificationTopic}.
     * 
     * @param subscriber The {@link NotificationSubscriber} to remove.
     */
    protected synchronized void remove(NotificationSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Returns the identifier of this {@link NotificationTopic}.
     * 
     * @return The identifier of this {@link NotificationTopic} (can be an regEx).
     */
    protected String getName() {
        return notificationId;
    }

    /**
     * Returns the regular expression filter used to check other notification ids whether they match
     * this topic.
     * 
     * @return the compiled regular expression to filter other notification ids against
     */
    public Pattern getNotificationIdFilter() {
        // Note: not synchronized to avoid overhead; race conditions arising from this should be
        // harmless, as regexp compilation is deterministic, the assignment is atomic,
        // and the performance cost of duplicate compilation is negligible. -- misc_ro
        if (compiledRegExp == null) {
            compiledRegExp = Pattern.compile(notificationId);
        }
        return compiledRegExp;
    }

    /**
     * Returns the {@link NotificationSubscriber}s for this {@link NotificationTopic}.
     * 
     * @return the {@link NotificationSubscriber}s.
     */
    protected synchronized Set<NotificationSubscriber> getSubscribers() {
        return new HashSet<NotificationSubscriber>(subscribers);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NotificationTopic) {
            return notificationId.equals(((NotificationTopic) object).getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return notificationId.hashCode();
    }

}
