/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.communication.common.NodeIdentifierFactory;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.notification.Notification;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.notification.SimpleNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.concurrent.AsyncExceptionListener;
import de.rcenvironment.core.utils.common.concurrent.CallablesGroup;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.common.concurrent.TaskDescription;

/**
 * Handles the connection to the subscription service and the retrieval of "missed" notifications.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class GenericSubscriptionManager {

    private final GenericSubscriptionEventProcessor eventProcessor;

    private final WorkflowHostService workflowHostService;
    
    private final CommunicationService communicationService;

    private final String wildCard = ".*";

    private final Set<String> subscribed = new HashSet<String>();

    /**
     * Default constructor.
     * 
     * @param model the {@link WorkflowStateModel} to apply received events on
     * @param communicationService the {@link CommunicationService} instance to use
     */
    public GenericSubscriptionManager(GenericSubscriptionEventProcessor eventProcessor, CommunicationService communicationService,
        WorkflowHostService workflowHostService) {
        this.eventProcessor = eventProcessor;
        this.communicationService = communicationService;
        this.workflowHostService = workflowHostService;
    }

    private Set<String> updateSubscribedIds(Set<String> subscribedIds) {

        Set<String> currentIdsToSubscribe = new HashSet<String>();

        Set<String> missingSubscribed = new HashSet<String>();

        Set<NodeIdentifier> allNodes = communicationService.getReachableNodes();
        Set<NodeIdentifier> allWorkflowNodes = workflowHostService.getWorkflowHostNodesAndSelf();

        for (NodeIdentifier node : allNodes) {
            for (NodeIdentifier wfNode : allWorkflowNodes) {
                String id = StringUtils.escapeAndConcat(node.getIdString(), wfNode.getIdString());
                currentIdsToSubscribe.add(id);
            }
        }

        missingSubscribed = new HashSet<String>(currentIdsToSubscribe);
        missingSubscribed.removeAll(subscribedIds);

        subscribed.retainAll(currentIdsToSubscribe);

        return missingSubscribed;
    }

    /**
     * Subscribes to the relevant notification id and catches up with previous updates. It only considers "new" platforms, which where not
     * known during initialize.
     * 
     * @param notificationIdSuffixes identifiers of notifications to subscribe
     */
    public synchronized void updateSubscriptions(String[] notificationIdSuffixes) {

        Set<String> missingSubscribed = updateSubscribedIds(subscribed);

        final SimpleNotificationService sns = new SimpleNotificationService();

        CallablesGroup<Void> callablesGroup = SharedThreadPool.getInstance().createCallablesGroup(Void.class);

        for (String missing : missingSubscribed) {
            final String missingSnapshot = missing;
            final String[] nodes = StringUtils.splitAndUnescape(missing);
            final NodeIdentifier targetNode = NodeIdentifierFactory.fromNodeId(nodes[0]);
            for (String notificationIdSuffix : notificationIdSuffixes) {
                final String notificationIdSuffixSnapshot = notificationIdSuffix;
                callablesGroup.add(new Callable<Void>() {

                    @Override
                    @TaskDescription("Distributed console/input model notification subscriptions")
                    public Void call() throws Exception {
                        Map<String, Long> lastMissedNumbers = sns.subscribe(wildCard + nodes[1]
                            + notificationIdSuffixSnapshot, eventProcessor, targetNode);
                        retrieveMissedNotifications(sns, targetNode, lastMissedNumbers);
                        subscribed.add(missingSnapshot);
                        return (Void) null;
                    }
                });
            }
        }
        callablesGroup.executeParallel(new AsyncExceptionListener() {

            @Override
            public void onAsyncException(Exception e) {
                LogFactory.getLog(getClass()).error(
                    "Asynchronous exception during parallel console/input model notification subscriptions", e);
            }
        });
    }

    private void retrieveMissedNotifications(SimpleNotificationService sns,
        NodeIdentifier node, Map<String, Long> lastMissedNumbers) {

        for (String notifId : lastMissedNumbers.keySet()) {
            Long lastMissedNumber = lastMissedNumbers.get(notifId);
            if (lastMissedNumber != NotificationService.NO_MISSED) {
                eventProcessor.setNumberOfLastMissingNotification(notifId, node.getIdString(), lastMissedNumber);
                Log log = LogFactory.getLog(getClass());
                log.debug(String.format("Starting to fetch stored notifications for id %s from node %s", notifId, node));
                Map<String, List<Notification>> storedNotifications = sns.getNotifications(notifId, node);
                log.debug(String.format("Received %d stored notification entries for id %s from node %s", storedNotifications.size(),
                    notifId, node));
                for (Entry<String, List<Notification>> e : storedNotifications.entrySet()) {
                    log.debug(String.format("  Received %d notifications for topic %s", e.getValue().size(), e.getKey()));
                }
                for (List<Notification> notifications : storedNotifications.values()) {
                    // TODO 5.0 final: replaced commented-out code with this line; remove old code after testing
                    eventProcessor.receiveBatchedNotifications(notifications);
                    // Iterator<Notification> notificationIterator = notifications.iterator();
                    // while (notificationIterator.hasNext()) {
                    // eventProcessor.notify(notificationIterator.next());
                    // }
                }
            }
        }
    }

}
