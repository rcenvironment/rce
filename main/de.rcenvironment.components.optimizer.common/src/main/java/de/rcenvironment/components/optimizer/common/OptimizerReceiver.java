/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.common;

import java.io.Serializable;

import de.rcenvironment.core.notification.NotificationSubscriber;

/**
 * Responsible for receiving study values.
 * @author Christian Weiss.
 */
public interface OptimizerReceiver extends Serializable {

    /**
     * @return the adequate study.
     */
    ResultSet getStudy();

    /**
     * @param notificationSubscriber used to subscribe for notifications containing study values.
     */
    void setNotificationSubscriber(NotificationSubscriber notificationSubscriber);

    /**
     * Initializes the {@link OptimizerReceiver}.
     */
    void initialize();

}
