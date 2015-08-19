/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.util.List;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;

/**
 * Default implementation of {@link NotificationSubscriber}. Use this as a base class for {@link NotificationSubscriber}s, unless there is a
 * specific reason to implement the interface directly.
 * 
 * @author Robert Mischke
 */
public abstract class DefaultNotificationSubscriber implements NotificationSubscriber {

    private static final long serialVersionUID = -3772887574186333020L;

    @Override
    @AllowRemoteAccess
    public final void receiveBatchedNotifications(List<Notification> notifications) {
        // catch all RTEs here so only transport errors can reach the remote caller
        try {
            // TODO review: should be decoupled from caller via thread pool to improve performance - misc_ro
            for (Notification notification : notifications) {
                processNotification(notification);
            }
        } catch (RuntimeException e) {
            // Note: acquiring the logger dynamically as it will be used very rarely - misc_ro
            LogFactory.getLog(getClass()).error("Error in notification handler", e);
        }
    }

    @Override
    @AllowRemoteAccess
    @Deprecated
    public final void receiveNotification(Notification notification) {
        LogFactory.getLog(getClass()).warn("Deprecated single-notification method variant called");
        // TODO review: should be decoupled from caller via thread pool to improve performance - misc_ro
        processNotification(notification);
    }

    protected abstract void processNotification(Notification notification);
}
