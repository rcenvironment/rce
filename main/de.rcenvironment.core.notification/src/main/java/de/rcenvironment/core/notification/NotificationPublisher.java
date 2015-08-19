/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

/**
 * Interface which can be implemented by publishers which want to allow
 * {@link NotificationSubscriber} o trigger for a new {@link Notification}.
 * 
 * @author Doreen Seider
 */
public interface NotificationPublisher {

    /**
     * Called by the {@link NotificationService} if someone request it by calling its trigger()
     * method.
     * @param notificationIdentifier The identifier representing the {@link Notification} to trigger.
     */
    void trigger(String notificationIdentifier);
}
