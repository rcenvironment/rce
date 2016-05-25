/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.monitoring.system.internal.AsyncSystemMonitoringDataFetchTask;
import de.rcenvironment.core.utils.common.concurrent.SharedThreadPool;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Fetches monitoring data asynchronously.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class SystemMonitoringDataPollingManager {

    private static final int REFRESH_INTERVAL_MSEC = 5000; // TODO make configurable

    private final CommunicationService communicationService;

    private final Map<NodeIdentifier, Future<?>> futureMap = new HashMap<>();

    private final SharedThreadPool threadPool = SharedThreadPool.getInstance();

    private final Log log = LogFactory.getLog(getClass());

    public SystemMonitoringDataPollingManager() {
        communicationService = ServiceRegistry.createAccessFor(this).getService(CommunicationService.class);
    }

    /**
     * Fetches data of the corresponding {@link SystemMonitoringDataSnapshot} from a given node.
     * 
     * @param nodeId The {@link NodeIdentifier} of the node whose data should be fetched.
     * @param callbackListener The {@link SystemMonitoringDataSnapshotListener}.
     */
    public synchronized void startPollingTask(final NodeIdentifier nodeId, final SystemMonitoringDataSnapshotListener callbackListener) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Node id must not be null");
        }
        final RemotableSystemMonitoringService remoteService =
            communicationService.getRemotableService(RemotableSystemMonitoringService.class, nodeId);
        final Future<?> existingFuture = futureMap.get(nodeId);
        if (existingFuture != null) {
            log.debug("Monitoring start requested for node " + nodeId + ", but it already has a monitoring task; ignoring request");
            return;
        }
        final AsyncSystemMonitoringDataFetchTask backgroundTask =
            new AsyncSystemMonitoringDataFetchTask(callbackListener, remoteService);
        final Future<?> newFuture = threadPool.scheduleAtFixedRate(backgroundTask, REFRESH_INTERVAL_MSEC);
        futureMap.put(nodeId, newFuture);
        log.debug("Started system monitoring background task for node " + nodeId);
    }

    /**
     * 
     * Cancels monitoring task of specified {@link NodeIdentifier}.
     * 
     * @param nodeId The {@link NodeIdentifier} of the node.
     */
    public synchronized void cancelPollingTask(NodeIdentifier nodeId) {
        removeAndCancelTask(nodeId);
    }

    /**
     * Cancels all running {@link AsyncSystemMonitoringDataFetchTask} if rce instance is disposed.
     */
    public synchronized void cancelAllPollingTasks() {
        for (Entry<NodeIdentifier, Future<?>> entry : futureMap.entrySet()) {
            final NodeIdentifier nodeId = entry.getKey();
            final Future<?> taskFuture = entry.getValue();
            cancelTaskFuture(nodeId, taskFuture);
        }
        futureMap.clear();
    }

    /**
     * Cancels {@link AsyncSystemMonitoringDataFetchTask}s of multiple nodes.
     * 
     * @param nodes Set of disconnectedNodes.
     */
    public synchronized void cancelPollingTasks(final Set<NodeIdentifier> nodes) {
        if (nodes != null) {
            for (NodeIdentifier nodeId : nodes) {
                removeAndCancelTask(nodeId);
            }
        }
    }

    private void removeAndCancelTask(NodeIdentifier nodeId) {
        if (nodeId == null) {
            log.warn("Attempted to cancel a monitoring task with a null node id");
            return;
        }
        Future<?> taskFuture = futureMap.remove(nodeId);
        if (taskFuture != null) {
            cancelTaskFuture(nodeId, taskFuture);
        }
    }

    private void cancelTaskFuture(NodeIdentifier nodeId, Future<?> future) {
        future.cancel(false);
        log.debug("Stopped system monitoring background task for node " + nodeId);
    }
}
