/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.common;

import java.io.Serializable;

import de.rcenvironment.core.notification.NotificationSubscriber;

/**
 * Responsible for receiving study values.
 * @author Christian Weiss.
 */
public interface StudyReceiver extends Serializable {

    /**
     * @return the adequate study.
     */
    Study getStudy();

    /**
     * @param notificationSubscriber used to subscribe for notifications containing study values.
     */
    void setNotificationSubscriber(NotificationSubscriber notificationSubscriber);

    /**
     * Initializes the {@link StudyReceiver}.
     */
    void initialize();

}
