/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.notification.api.RemotableNotificationService;
import de.rcenvironment.core.notification.internal.NotificationServiceImpl;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Factory for mock objects used by this bundle's test.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
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

    // note: not used as of RCE 7.0.0
    public RemotableNotificationService getNotificationServiceMock() {
        return notificationServiceMock;
    }

    public PlatformService getPlatformServiceMock() {
        return platformServiceMock;
    }

    private void createPlatformServiceMock() {
        platformServiceMock = EasyMock.createNiceMock(PlatformService.class);

        EasyMock.expect(platformServiceMock.getLocalInstanceNodeSessionId()).andReturn(NotificationTestConstants.LOCAL_INSTANCE_SESSION)
            .anyTimes();

        EasyMock.expect(platformServiceMock.matchesLocalInstance(NotificationTestConstants.LOCAL_INSTANCE_SESSION)).andReturn(true)
            .anyTimes();
        EasyMock.expect(platformServiceMock.matchesLocalInstance(NotificationTestConstants.REMOTE_INSTANCE_SESSION)).andReturn(false)
            .anyTimes();

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
        try {
            EasyMock.expect(notificationServiceMock.getNotifications(NotificationTestConstants.NOTIFICATION_ID))
                .andReturn(notifications);
        } catch (RemoteOperationException e) {
            throw new AssertionError("Exception during setup", e); // should never happen
        }

        EasyMock.replay(notificationServiceMock);
    }

    // note: not used as of RCE 7.0.0
    private void createCommunicationServiceMock() {
        communicationServiceMock = EasyMock.createNiceMock(CommunicationService.class);

        EasyMock.expect(communicationServiceMock.getRemotableService(EasyMock.eq(NotificationService.class),
            EasyMock.eq(NotificationTestConstants.REMOTE_INSTANCE_SESSION))).andReturn(notificationServiceMock).anyTimes();

        EasyMock.expect(communicationServiceMock.getRemotableService(EasyMock.eq(NotificationService.class),
            EasyMock.eq(NotificationTestConstants.LOCAL_INSTANCE_SESSION))).andReturn(notificationServiceMock).anyTimes();

        EasyMock.replay(communicationServiceMock);
    }

}
