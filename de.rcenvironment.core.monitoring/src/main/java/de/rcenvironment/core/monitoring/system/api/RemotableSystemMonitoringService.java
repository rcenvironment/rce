/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * {@link RemotableService} interface for fetching system monitoring data from the local or other instances.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
@RemotableService
public interface RemotableSystemMonitoringService {

    /**
     * Get a {@link FullSystemAndProcessDataSnapshot} which contains all system resource informations at once. This limits network traffic.
     * 
     * @return {@link FullSystemAndProcessDataSnapshot}.
     * @throws OperatingSystemException if getting {@link FullSystemAndProcessDataSnapshot} fails.
     * @throws RemoteOperationException standard remote service call exception
     */
    FullSystemAndProcessDataSnapshot getCompleteSnapshot() throws OperatingSystemException, RemoteOperationException;

    /**
     * Retrieves lightweight system load information. Currently, this consists of the latest observed CPU load percentage, the average of a
     * certain number of most recent CPU load values, and the most recent free/available RAM value.
     * 
     * @param maxSamples the maximum number of samples to include in the CPU load average; the current polling interval is 1 second
     * @return an immutable system load information holder
     * @throws RemoteOperationException standard remote service call exception
     */
    SystemLoadInformation getSystemLoadInformation(Integer maxSamples) throws RemoteOperationException;

}
