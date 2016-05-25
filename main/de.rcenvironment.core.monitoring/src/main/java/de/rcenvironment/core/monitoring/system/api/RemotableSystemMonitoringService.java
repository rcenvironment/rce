/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * {@link RemotableService} interface for fetching system monitoring data from the local or other instances.
 * 
 * @author David Scholz
 * @author Robert Mischke (minor changes)
 */
@RemotableService
public interface RemotableSystemMonitoringService {

    /**
     * Get a {@link SystemMonitoringDataSnapshot} which contains all system resource informations at once. This limits network traffic.
     * 
     * @return {@link SystemMonitoringDataSnapshot}.
     * @throws OperatingSystemException if getting {@link SystemMonitoringDataSnapshot} fails.
     * @throws RemoteOperationException standard remote service call exception
     */
    SystemMonitoringDataSnapshot getCompleteSnapshot() throws OperatingSystemException, RemoteOperationException;

}
