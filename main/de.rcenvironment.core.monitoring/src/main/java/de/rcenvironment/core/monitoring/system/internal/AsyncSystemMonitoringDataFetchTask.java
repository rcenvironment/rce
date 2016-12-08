/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.RemotableSystemMonitoringService;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshotListener;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

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
    @TaskDescription("Fetching monitoring data...")
    public void run() {

        try {
            listener.onMonitoringDataChanged(service.getCompleteSnapshot());
        } catch (OperatingSystemException | RemoteOperationException e) {
            LOGGER.warn("Error fetching monitoring data: " + e.toString());
        }
    }

}
