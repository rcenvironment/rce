/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.notification.internal;

import java.io.Serializable;
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
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.NotificationSubscriber;
import de.rcenvironment.core.notification.api.RemotableNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.ServiceUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Implementation of {@link DistributedNotificationService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (8.0.0 id adaptations)
 */
// FIXME clarify behavior on failure: return null, empty collections or throw exceptions? -- misc_ro
// (see related Mantis issue #6542)
public class DistributedNotificationServiceImpl implements DistributedNotificationService {

    private static final Log LOGGER = LogFactory.getLog(DistributedNotificationServiceImpl.class);

    private static NotificationService notificationService;

    private static CommunicationService nullCommunicationService = ServiceUtils.createFailingServiceProxy(CommunicationService.class);

    private static CommunicationService communicationService = nullCommunicationService;

    private static PlatformService platformService = ServiceUtils.createFailingServiceProxy(PlatformService.class);

    protected void activate(BundleContext bundleContext) {}

    protected void bindNotificationService(NotificationService newNotificationService) {
        notificationService = newNotificationService;
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }

    protected void bindPlatformService(PlatformService newPlatformService) {
        platformService = newPlatformService;
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
        ResolvableNodeId publishPlatform) throws RemoteOperationException {
        try {
            Pattern.compile(notificationId);
        } catch (RuntimeException e) {
            LOGGER.error("Notification Id is not a valid RegExp: " + notificationId, e);
            throw e;
        }
        // If publishPlatform is null, insert local ID
        if (publishPlatform == null) {
            publishPlatform = platformService.getLocalInstanceNodeSessionId();
        }
        try {
            final RemotableNotificationService remoteService = (RemotableNotificationService) communicationService.getRemotableService(
                RemotableNotificationService.class, publishPlatform);
            return remoteService.subscribe(notificationId, subscriber);
        } catch (RemoteOperationException e) {
            String message = StringUtils.format("Failed to subscribe to publisher @%s: ", publishPlatform);
            throw new RemoteOperationException(message + e.toString());
        }
    }

    @Override
    public Map<InstanceNodeSessionId, Map<String, Long>> subscribeToAllReachableNodes(final String notificationId,
        final NotificationSubscriber subscriber) {

        final Map<InstanceNodeSessionId, Map<String, Long>> missedNumbersMap =
            Collections.synchronizedMap(new HashMap<InstanceNodeSessionId, Map<String, Long>>());

        // do not filter by "workflow host" flag for now, as components may send out
        // notifications from nodes that are not workflow hosts - misc_ro, July 2013
        Set<InstanceNodeSessionId> nodesToSubscribeTo = communicationService.getReachableInstanceNodes();

        // create the parallel subscription tasks; no return value as results are added to the map
        CallablesGroup<Void> callables = ConcurrencyUtils.getFactory().createCallablesGroup(Void.class);
        for (final InstanceNodeSessionId nodeId : nodesToSubscribeTo) {
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
    public void unsubscribe(String notificationId, NotificationSubscriber subscriber, ResolvableNodeId publishPlatform)
        throws RemoteOperationException {
        try {
            getRemoteNotificationService(publishPlatform).unsubscribe(notificationId, subscriber);
        } catch (RuntimeException | RemoteOperationException e) {
            throw new RemoteOperationException(
                StringUtils.format("Failed to unsubscribe from publisher %s: ", publishPlatform) + e.getMessage());
        }
    }

    @Override
    public Map<String, List<Notification>> getNotifications(String notificationId, ResolvableNodeId publishPlatform)
        throws RemoteOperationException {
        return getRemoteNotificationService(publishPlatform).getNotifications(notificationId);
    }

    private RemotableNotificationService getRemoteNotificationService(ResolvableNodeId publishPlatform) throws RemoteOperationException {
        return (RemotableNotificationService) communicationService.getRemotableService(RemotableNotificationService.class, publishPlatform);
    }

}
