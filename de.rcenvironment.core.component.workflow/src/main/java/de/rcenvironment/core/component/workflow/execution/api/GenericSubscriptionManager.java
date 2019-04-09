/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.NodeIdentifierUtils;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncExceptionListener;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * Handles the connection to the subscription service and the retrieval of "missed" notifications.
 * 
 * Note: Consider this broken. Not because of the code here but because of the {@link NotificationService} and lack of a reliable concept
 * for event streams. --seid_do
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class GenericSubscriptionManager {

    private static final String NOTIFICATION_PATTERN_WILDCARD = ".*";

    private final GenericSubscriptionEventProcessor eventProcessor;

    private final WorkflowHostService workflowHostService;

    private final CommunicationService communicationService;

    private final DistributedNotificationService notificationService;

    private final Set<String> subscribedIds = new HashSet<String>();

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled(getClass());

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor.
     * 
     * @param model the {@link WorkflowStateModel} to apply received events on
     * @param communicationService the {@link CommunicationService} instance to use
     * @param notificationService the {@link DistributedNotificationService} instance to use
     */
    public GenericSubscriptionManager(GenericSubscriptionEventProcessor eventProcessor, CommunicationService communicationService,
        WorkflowHostService workflowHostService, DistributedNotificationService notificationService) {
        this.eventProcessor = eventProcessor;
        this.communicationService = communicationService;
        this.workflowHostService = workflowHostService;
        this.notificationService = notificationService;
    }

    private Set<String> updateSubscribedIds() {

        final Set<String> currentIdsToSubscribe = new HashSet<String>();

        final Set<InstanceNodeSessionId> allWorkflowNodes = workflowHostService.getWorkflowHostNodesAndSelf();
        for (InstanceNodeSessionId wfNode : allWorkflowNodes) {
            final String id = wfNode.getInstanceNodeSessionIdString();
            currentIdsToSubscribe.add(id);
        }

        final Set<String> missingSubscribed = new HashSet<String>(currentIdsToSubscribe);
        missingSubscribed.removeAll(subscribedIds); // determine missing nodes/ids

        subscribedIds.retainAll(currentIdsToSubscribe); // purge unreachable nodes/ids

        return missingSubscribed;
    }

    /**
     * Subscribes to the relevant notification id and catches up with previous updates. It only considers "new" platforms, which where not
     * known during initialize.
     * 
     * @param notificationIdPrefixes identifiers of notifications to subscribe
     */
    public synchronized void updateSubscriptionsForPrefixes(String[] notificationIdPrefixes) {

        final Set<String> missingSubscribedIds = updateSubscribedIds();

        final CallablesGroup<Void> callablesGroup = ConcurrencyUtils.getFactory().createCallablesGroup(Void.class);

        for (final String missingId : missingSubscribedIds) {
            final InstanceNodeSessionId targetWorkflowHostNode =
                NodeIdentifierUtils.parseInstanceNodeSessionIdStringWithExceptionWrapping(missingId);
            for (final String notificationIdPrefix : notificationIdPrefixes) {
                callablesGroup.add(new Callable<Void>() {

                    @Override
                    @TaskDescription("Distributed console/input model notification subscriptions")
                    public Void call() throws Exception {
                        Map<String, Long> lastMissedNumbers = notificationService.subscribe(
                            StringUtils.format("%s%s:" + NOTIFICATION_PATTERN_WILDCARD, notificationIdPrefix,
                                targetWorkflowHostNode.getInstanceNodeIdString()),
                            eventProcessor, targetWorkflowHostNode);
                        retrieveMissedNotifications(targetWorkflowHostNode, lastMissedNumbers);
                        synchronized (subscribedIds) {
                            subscribedIds.add(missingId);
                        }
                        return (Void) null;
                    }
                });
            }
        }
        callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e1) {
                // unwrap ExecutionExceptions
                Throwable e;
                if (e1.getClass() == ExecutionException.class && e1.getCause() != null) {
                    e = e1.getCause();
                } else {
                    e = e1;
                }

                if (e.getCause() == null) {
                    // log a compressed message; this includes the case of RemoteOperationExceptions, which (by design) never have a "cause"
                    log.warn(
                        "Asynchronous exception during parallel console/input model notification subscriptions: " + e.toString());
                } else {
                    // on unexpected errors, log the full stacktrace
                    log.error(
                        "Asynchronous exception during parallel console/input model notification subscriptions", e);
                }
            }
        });
    }

    private void retrieveMissedNotifications(InstanceNodeSessionId targetNode, Map<String, Long> lastMissedNumbers)
        throws RemoteOperationException {

        for (String notifId : lastMissedNumbers.keySet()) {
            Long lastMissedNumber = lastMissedNumbers.get(notifId);
            if (lastMissedNumber != NotificationService.NO_MISSED) {
                eventProcessor.setNumberOfLastMissingNotification(notifId, targetNode.getInstanceNodeSessionIdString(), lastMissedNumber);
                if (verboseLogging) {
                    log.debug(StringUtils.format("Starting to fetch stored notifications for id %s from node %s", notifId, targetNode));
                }
                Map<String, List<Notification>> storedNotifications = notificationService.getNotifications(notifId, targetNode);
                if (verboseLogging) {
                    log.debug(StringUtils.format("Received %d stored notification entries for id %s from node %s",
                        storedNotifications.size(), notifId, targetNode));
                    for (Entry<String, List<Notification>> e : storedNotifications.entrySet()) {
                        log.debug(StringUtils.format("  Received %d notifications for topic %s", e.getValue().size(), e.getKey()));
                    }
                }
                for (List<Notification> notifications : storedNotifications.values()) {
                    eventProcessor.receiveBatchedNotifications(notifications);
                }
            }
        }
    }

}
