/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.components.optimizer.gui.view;

import java.io.Serializable;

import de.rcenvironment.components.optimizer.common.OptimizerResultSet;
import de.rcenvironment.core.notification.DefaultNotificationSubscriber;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationSubscriber;
/**
 * Used to subscribe to {@link Dataset}s.
 * @author Christian Weiss
 */
public class DatasetNotificationSubscriber extends DefaultNotificationSubscriber {

    private static final long serialVersionUID = 7984538979387371048L;

    private final transient OptimizerDatastore datastore;

    public DatasetNotificationSubscriber(final OptimizerDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Class<? extends Serializable> getInterface() {
        return NotificationSubscriber.class;
    }

    @Override
    public void processNotification(Notification notification) {
        final OptimizerResultSet dataset = (OptimizerResultSet) notification.getBody();
        datastore.addDataset(dataset);
    }
    
}
