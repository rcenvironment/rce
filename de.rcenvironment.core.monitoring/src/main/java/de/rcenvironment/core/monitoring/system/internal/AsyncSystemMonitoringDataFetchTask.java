/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.RemotableSystemMonitoringService;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshotListener;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link Runnable} for fetching monitoring data.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class AsyncSystemMonitoringDataFetchTask implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(AsyncSystemMonitoringDataFetchTask.class);

    private RemotableSystemMonitoringService service;

    private SystemMonitoringDataSnapshotListener listener;

    public AsyncSystemMonitoringDataFetchTask(SystemMonitoringDataSnapshotListener callbackListener,
        RemotableSystemMonitoringService service) {
        this.listener = callbackListener;
        this.service = service;
    }

    @Override
    public void run() {

        try {
            listener.onMonitoringDataChanged(service.getCompleteSnapshot());
        } catch (OperatingSystemException | RemoteOperationException e) {
            LOGGER.warn("Error fetching monitoring data: " + e.toString());
        }
    }

}
