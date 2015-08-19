/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.internal.NotificationServiceImpl;

/**
 * Factory for mock objects used by this bundle's test.
 * 
 * @author Doreen Seider
 */
public final class NotificationMockFactory {

    private static NotificationMockFactory instance = null;

    private CommunicationService communicationServiceMock;

    private PlatformService platformServiceMock;

    private NotificationService notificationServiceMock;

    private NotificationMockFactory() {
        createPlatformServiceMock();
        createNotificationServiceMock();
        createCommunicationServiceMock();
    }

    /**
     * Getter.
     * 
     * @return Instance of this class.
     */
    public static synchronized NotificationMockFactory getInstance() {
        if (instance == null) {
            instance = new NotificationMockFactory();
        }
        return instance;
    }

    public CommunicationService getCommunicationServiceMock() {
        return communicationServiceMock;
    }

    public NotificationService getNotificationServiceMock() {
        return notificationServiceMock;
    }

    public PlatformService getPlatformServiceMock() {
        return platformServiceMock;
    }

    private void createPlatformServiceMock() {
        platformServiceMock = EasyMock.createNiceMock(PlatformService.class);

        EasyMock.expect(platformServiceMock.getLocalNodeId()).andReturn((NodeIdentifier) NotificationTestConstants.LOCALHOST)
            .anyTimes();

        EasyMock.expect(platformServiceMock.isLocalNode(NotificationTestConstants.LOCALHOST)).andReturn(true).anyTimes();
        EasyMock.expect(platformServiceMock.isLocalNode(NotificationTestConstants.REMOTEHOST)).andReturn(false).anyTimes();

        EasyMock.replay(platformServiceMock);
    }

    private void createNotificationServiceMock() {
        notificationServiceMock = EasyMock.createNiceMock(NotificationServiceImpl.class);

        EasyMock.expect(notificationServiceMock.getNotification(NotificationTestConstants.NOTIFICATION_HEADER))
            .andReturn(NotificationTestConstants.NOTIFICATION);

        Map<String, SortedSet<NotificationHeader>> headers = new HashMap<String, SortedSet<NotificationHeader>>();
        headers.put(NotificationTestConstants.NOTIFICATION_ID, NotificationTestConstants.NOTIFICATION_HEADERS);
        EasyMock.expect(notificationServiceMock.getNotificationHeaders(NotificationTestConstants.NOTIFICATION_ID))
            .andReturn(headers);

        Map<String, List<Notification>> notifications = new HashMap<String, List<Notification>>();
        notifications.put(NotificationTestConstants.NOTIFICATION_ID, NotificationTestConstants.NOTIFICATIONS);
        EasyMock.expect(notificationServiceMock.getNotifications(NotificationTestConstants.NOTIFICATION_ID))
            .andReturn(notifications);

        EasyMock.replay(notificationServiceMock);
    }

    private void createCommunicationServiceMock() {
        communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);

        EasyMock.expect(communicationServiceMock.getService(EasyMock.eq(NotificationService.class),
            EasyMock.eq(NotificationTestConstants.REMOTEHOST), EasyMock.eq((BundleContext) null)))
            .andReturn(notificationServiceMock).anyTimes();

        EasyMock.expect(communicationServiceMock.getService(EasyMock.eq(NotificationService.class),
            EasyMock.eq(NotificationTestConstants.LOCALHOST), EasyMock.eq((BundleContext) null)))
            .andReturn(notificationServiceMock).anyTimes();

        EasyMock.replay(communicationServiceMock);
    }

}
