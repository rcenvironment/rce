/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;

/**
 * Local query methods for aggregated monitoring data.
 *
 * @author Robert Mischke
 */
public interface LocalSystemMonitoringAggregationService {

    /**
     * The polling interval for CPU and RAM load data; currently hard-coded.
     */
    int SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC = 1000;

    /**
     * The default minimum time in msec that should be accepted between two subsequent update events; if update events arrive faster than
     * that, the later ones should be ignored.
     */
    int MINIMUM_TIME_DELTA_TO_ACCEPT_BETWEEN_UPDATES = 500;

    /**
     * The default maximum time in msec that is allowed to pass between two subsequent update events before a collector should consider the
     * data sequence "broken" and start over, ie discard all previous data.
     */
    int MAXIMUM_TIME_DELTA_TO_ACCEPT_BEFORE_STARTING_OVER = 5000;

    /**
     * Fetches {@link SystemLoadInformation} objects from the specified nodes, while limiting their response times to a specified value. If
     * a node does not respond within this time, the returned map will not contain any data for it.
     * 
     * @param <T> the node id type to use
     * @param nodes the {@link InstanceNodeSessionId}s of the nodes to query
     * @param timeSpanMsec the maximum time span to aggregate monitoring data (e.g. CPU load) over, in msec
     * @param timeLimitMsec the maximum time to wait for an individual node's response, in msec
     * @return a map of the nodes that responded in time to their returned {@link SystemLoadInformation} objects
     * @throws TimeoutException on unexpected errors during asynchronous task execution
     * @throws ExecutionException on unexpected errors during asynchronous task execution
     * @throws InterruptedException on interruption while waiting for asynchronous task execution
     */
    <T extends ResolvableNodeId> Map<T, SystemLoadInformation> collectSystemMonitoringDataWithTimeLimit(
        Set<T> nodes, int timeSpanMsec, int timeLimitMsec) throws InterruptedException, ExecutionException, TimeoutException;

}
