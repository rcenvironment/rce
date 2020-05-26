/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.excel.gui.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.apache.commons.logging.LogFactory;

import de.rcenvironment.components.excel.common.ChannelValue;
import de.rcenvironment.components.excel.common.ExcelComponentConstants;
import de.rcenvironment.components.excel.common.ExcelUtils;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * ModelProvider of channels.
 * 
 * @author Markus Kunde
 */
public class ModelProvider extends Observable implements NotificationSubscriber {

    private static final long serialVersionUID = 611752605431248651L;

    private List<ChannelValue> channelValues = null;

    private boolean areNotificationsMissed = true;

    private Long lastMissedNotification = null;

    private Deque<Notification> queuedNotifications = new LinkedList<Notification>();

    private boolean isSubscribed = false;

    /**
     * Constructor.
     * 
     */
    public ModelProvider() {
        channelValues = new ArrayList<ChannelValue>();
    }

    /**
     * Subscribing model to notifications at all platforms.
     * 
     * @param componentIdentifier identifier of specific component
     * @param publishPlatform identifier of publishing platform
     * @throws RemoteOperationException when subscribing to remote notifications fails
     */
    public void subscribeToLocalToolRunPlatForm(final String componentIdentifier, final ResolvableNodeId publishPlatform)
        throws RemoteOperationException {
        if (!isSubscribed) {
            DistributedNotificationService notificationService =
                ServiceRegistry.createAccessFor(this).getService(DistributedNotificationService.class);

            Map<String, Long> lastMissedNotifications;
            try {
                lastMissedNotifications =
                    notificationService.subscribe(componentIdentifier + ExcelComponentConstants.NOTIFICATION_SUFFIX, this, publishPlatform);
            } catch (RemoteOperationException e) {
                LogFactory.getLog(getClass()).error("Failed to subscribe to Excel run platform: " + e.getMessage());
                return;
            }

            for (String notifId : lastMissedNotifications.keySet()) {
                Long lastMissedNumber = lastMissedNotifications.get(notifId);
                if (lastMissedNumber == NotificationService.NO_MISSED) {
                    setNotificationsMissed(false);
                    setLastMissedNotification(lastMissedNumber);
                } else {
                    setNotificationsMissed(true);
                    setLastMissedNotification(lastMissedNumber);
                }
                for (List<Notification> notifications : notificationService.getNotifications(notifId, publishPlatform).values()) {
                    Iterator<Notification> notificationIterator = notifications.iterator();
                    while (notificationIterator.hasNext()) {
                        notify(notificationIterator.next());
                    }
                }
            }

            isSubscribed = true;
        }
    }

    /**
     * Returns all ChannelValues.
     * 
     * @return list of ChannelValues
     */
    public List<ChannelValue> getChannelValues() {
        return channelValues;
    }

    /**
     * Method to add channelvalues to model.
     * 
     * @param channelVals list of channelvalues to add to model
     */
    public void addNewChannelValues(final List<ChannelValue> channelVals) {
        channelValues.addAll(channelVals);
        setChanged();
        notifyObservers();

        // Not nice, but workbook-object will not released.
        ExcelUtils.destroyGarbage();
    }

    /**
     * Method to add channelvalues to model.
     * 
     * @param cval channelvalue to add to model
     */
    public void addNewChannelValue(final ChannelValue cval) {
        channelValues.add(cval);
        setChanged();
        notifyObservers();

        // Not nice, but workbook-object will not released.
        ExcelUtils.destroyGarbage();
    }

    // TODO code copied from DefaultNotificationSubscriber; rework to subclass (create a nested class if necessary)
    @Override
    @AllowRemoteAccess
    public void receiveBatchedNotifications(List<Notification> notifications) {
        // catch all RTEs here so only transport errors can reach the remote caller
        try {
            for (Notification notification : notifications) {
                notify(notification);
            }
        } catch (RuntimeException e) {
            // Note: acquiring the logger dynamically as it will be used very rarely
            LogFactory.getLog(getClass()).error("Error in notification handler", e);
        }
    }

    private void notify(Notification notification) {
        if (areNotificationsMissed && lastMissedNotification == NotificationService.NO_MISSED) {
            queuedNotifications.add(notification);
        } else if (areNotificationsMissed && notification.getHeader().getNumber() > lastMissedNotification) {
            queuedNotifications.add(notification);
        } else {
            final Object body = notification.getBody();
            if (body instanceof ChannelValue) {
                ChannelValue val = (ChannelValue) notification.getBody();
                channelValues.add(val);
                setChanged();
                notifyObservers();

                // Not nice, but workbook-object will not released.
                ExcelUtils.destroyGarbage();
            }
            if (areNotificationsMissed && notification.getHeader().getNumber() == lastMissedNotification) {

                while (!queuedNotifications.isEmpty()) {
                    notify(queuedNotifications.getFirst());
                }
                areNotificationsMissed = false;
            }
        }
    }

    @Override
    public Class<? extends Serializable> getInterface() {
        return NotificationSubscriber.class;
    }

    public void setLastMissedNotification(Long number) {
        lastMissedNotification = number;
    }

    public void setNotificationsMissed(boolean missed) {
        areNotificationsMissed = missed;
    }
}
