/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.internal.AsyncSystemMonitoringDataFetchTask;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Fetches monitoring data asynchronously.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class SystemMonitoringDataPollingManager {

    private static final int REFRESH_INTERVAL_MSEC = 5000; // TODO make configurable

    private static final int START_INTERVAL_MSEC = 100;

    private final CommunicationService communicationService;

    private final AsyncTaskService asyncTaskService;

    private final Map<InstanceNodeSessionId, Future<?>> futureMap = new HashMap<>();

    private final Log log = LogFactory.getLog(getClass());

    public SystemMonitoringDataPollingManager(CommunicationService communicationService, AsyncTaskService asyncTaskService) {
        this.communicationService = communicationService;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * Fetches data of the corresponding {@link FullSystemAndProcessDataSnapshot} from a given node.
     * 
     * @param nodeId The {@link InstanceNodeSessionId} of the node whose data should be fetched.
     * @param callbackListener The {@link SystemMonitoringDataSnapshotListener}.
     */
    public synchronized void startPollingTask(final InstanceNodeSessionId nodeId,
        final SystemMonitoringDataSnapshotListener callbackListener) {
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
        final Future<?> newFuture =
            asyncTaskService.scheduleAtFixedRateAfterDelay("System Monitoring: Background fetching of system data", backgroundTask,
                START_INTERVAL_MSEC, REFRESH_INTERVAL_MSEC);
        futureMap.put(nodeId, newFuture);
        log.debug("Started system monitoring background task for node " + nodeId);
    }

    /**
     * 
     * Cancels monitoring task of specified {@link InstanceNodeSessionId}.
     * 
     * @param nodeId The {@link InstanceNodeSessionId} of the node.
     */
    public synchronized void cancelPollingTask(InstanceNodeSessionId nodeId) {
        removeAndCancelTask(nodeId);
    }

    /**
     * Cancels all running {@link AsyncSystemMonitoringDataFetchTask} if rce instance is disposed.
     */
    public synchronized void cancelAllPollingTasks() {
        for (Entry<InstanceNodeSessionId, Future<?>> entry : futureMap.entrySet()) {
            final InstanceNodeSessionId nodeId = entry.getKey();
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
    public synchronized void cancelPollingTasks(final Set<InstanceNodeSessionId> nodes) {
        if (nodes != null) {
            for (InstanceNodeSessionId nodeId : nodes) {
                removeAndCancelTask(nodeId);
            }
        }
    }

    private void removeAndCancelTask(InstanceNodeSessionId nodeId) {
        if (nodeId == null) {
            log.warn("Attempted to cancel a monitoring task with a null node id");
            return;
        }
        Future<?> taskFuture = futureMap.remove(nodeId);
        if (taskFuture != null) {
            cancelTaskFuture(nodeId, taskFuture);
        }
    }

    private void cancelTaskFuture(InstanceNodeSessionId nodeId, Future<?> future) {
        future.cancel(false);
        log.debug("Stopped system monitoring background task for node " + nodeId);
    }
}
