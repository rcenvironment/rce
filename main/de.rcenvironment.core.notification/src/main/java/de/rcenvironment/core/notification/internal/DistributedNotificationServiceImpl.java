/*
 * Copyright (C) 2006-2015 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.api.RemotableNotificationService;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link DistributedNotificationService}.
 * 
 * @author Doreen Seider
 * 
 */
// FIXME clarify behavior on failure: return null, empty collections or throw exceptions? -- misc_ro
// (see related Mantis issue #6542)
public class DistributedNotificationServiceImpl implements DistributedNotificationService {

    private static final Log LOGGER = LogFactory.getLog(DistributedNotificationServiceImpl.class);

    private static NotificationService notificationService;

    private static CommunicationService nullCommunicationService = ServiceUtils.createFailingServiceProxy(CommunicationService.class);

    private static CommunicationService communicationService = nullCommunicationService;

    private static BundleContext context;

    protected void activate(BundleContext bundleContext) {
        context = bundleContext;
    }

    protected void bindNotificationService(NotificationService newNotificationService) {
        notificationService = newNotificationService;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    @Override
    public void setBufferSize(String notificationId, int bufferSize) {
        notificationService.setBufferSize(notificationId, bufferSize);
    }

    @Override
    public void removePublisher(String notificationId) {
        notificationService.removePublisher(notificationId);
    }

    @Override
    public <T extends Serializable> void send(String notificationId, T notificationBody) {
        notificationService.send(notificationId, notificationBody);
    }

    @Override
    public Map<String, Long> subscribe(String notificationId, NotificationSubscriber subscriber,
        NodeIdentifier publishPlatform) throws RemoteOperationException {
        try {
            Pattern.compile(notificationId);
        } catch (RuntimeException e) {
            LOGGER.error("Notification Id is not a valid RegExp: " + notificationId, e);
            throw e;
        }
        try {
            final RemotableNotificationService remoteService = (RemotableNotificationService) communicationService.getRemotableService(
                RemotableNotificationService.class, publishPlatform);
            return remoteService.subscribe(notificationId, subscriber);
        } catch (RemoteOperationException e) {
            String message = MessageFormat.format("Failed to subscribe to publisher @{0}: ", publishPlatform);
            throw new RemoteOperationException(message + e.toString());
        }
    }

    @Override
    public Map<NodeIdentifier, Map<String, Long>> subscribeToAllReachableNodes(final String notificationId,
        final NotificationSubscriber subscriber) {

        final Map<NodeIdentifier, Map<String, Long>> missedNumbersMap =
            Collections.synchronizedMap(new HashMap<NodeIdentifier, Map<String, Long>>());

        // do not filter by "workflow host" flag for now, as components may send out
        // notifications from nodes that are not workflow hosts - misc_ro, July 2013
        Set<NodeIdentifier> nodesToSubscribeTo = communicationService.getReachableNodes();

        // create the parallel subscription tasks; no return value as results are added to the map
        CallablesGroup<Void> callables = SharedThreadPool.getInstance().createCallablesGroup(Void.class);
        for (final NodeIdentifier nodeId : nodesToSubscribeTo) {
            callables.add(new Callable<Void>() {

                @Override
                @TaskDescription("Distributed notification subscription")
                public Void call() throws Exception {
                    Map<String, Long> missedNumbers = subscribe(notificationId, subscriber, nodeId);
                    missedNumbersMap.put(nodeId, missedNumbers);
                    return (Void) null;
                }
            });
        }
        callables.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                LOGGER.error("Asynchronous exception while subscribing for notification " + notificationId);
            }
        });

        return missedNumbersMap;
    }

    @Override
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber, NodeIdentifier publishPlatform)
        throws RemoteOperationException {
        try {
            getRemoteNotificationService(publishPlatform).unsubscribe(notificationId, subscriber);
        } catch (RuntimeException | RemoteOperationException e) {
            throw new RemoteOperationException(MessageFormat.format("Failed to unsubscribe from remote publisher @{0}: ", publishPlatform)
                + e.getMessage());
        }
    }

    @Override
    public Map<String, List<Notification>> getNotifications(String notificationId, NodeIdentifier publishPlatform) {
        try {
            return getRemoteNotificationService(publishPlatform).getNotifications(notificationId);
        } catch (RuntimeException | RemoteOperationException e) {
            String message = MessageFormat.format("Failed to get remote notifications @{0}: ", publishPlatform);
            LOGGER.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private RemotableNotificationService getRemoteNotificationService(NodeIdentifier publishPlatform) throws RemoteOperationException {
        return (RemotableNotificationService) communicationService.getRemotableService(RemotableNotificationService.class, publishPlatform);
    }

}
