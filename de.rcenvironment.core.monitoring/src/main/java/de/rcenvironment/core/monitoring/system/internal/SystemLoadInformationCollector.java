/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringConstants;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataService;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.utils.common.TimeSource;

/**
 * Continuously collects low-detail system information (total CPU load, total RAM usage) and provides this data in the required forms. Most
 * notably, this includes providing a sliding average of the recent CPU load.
 *
 * @author Robert Mischke
 */
public class SystemLoadInformationCollector implements Runnable {

    /**
     * Internal copy of the {@link SystemMonitoringDataService} to avoid race conditions.
     */
    private final SystemMonitoringDataService dataSource;

    private final TimeSource timeSource;

    private final int minimumTimeDelta;

    private final int maximumTimeDelta;

    private final RingBufferOfDouble cpuLoadRingBuffer;

    private double latestCpuLoad = SystemMonitoringConstants.CPU_LOAD_UNKNOWN_DEFAULT;

    private long latestAvailableRam = SystemMonitoringConstants.RAM_UNKNOWN_DEFAULT;

    private int failureCount = 0;

    private long lastUpdateTime = Integer.MIN_VALUE; // neither zero nor Long.MIN_VALUE to prevent test artifacts

    private int ignoredUpdateTriggers;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * @param minimumTimeDelta the minimum time in msec that should be accepted between two subsequent update events; if update events
     *        arrive faster than that, the later ones should be ignored
     * @param maximumTimeDelta the maximum time in msec that is allowed to pass between two subsequent update events before this collector
     *        considers the data sequence "broken" and starts over, ie discards all previous data
     */
    public SystemLoadInformationCollector(SystemMonitoringDataService systemDataService, int bufferCapacity, TimeSource timeSource,
        int minimumTimeDelta, int maximumTimeDelta) {
        Objects.requireNonNull(systemDataService);
        Objects.requireNonNull(timeSource);
        this.dataSource = systemDataService;
        this.cpuLoadRingBuffer = new RingBufferOfDouble(bufferCapacity);
        this.timeSource = timeSource;
        this.minimumTimeDelta = minimumTimeDelta;
        this.maximumTimeDelta = maximumTimeDelta;
    }

    @Override
    public synchronized void run() {
        try {
            // update interval checks
            long time = timeSource.getCurrentTimeMillis();
            if (time < lastUpdateTime + minimumTimeDelta) {
                ignoredUpdateTriggers++;
                return;
            }
            lastUpdateTime = time;

            // if update triggers have been ignored, log them together as a single message when resuming
            if (ignoredUpdateTriggers > 0) {
                log.debug(StringUtils.format(
                    "Ignored %s delayed system monitoring update trigger(s) that arrived faster than "
                        + "the configured minimum time of %d msec; this may be caused by the host system "
                        + "resuming after being suspended, or a very high number of concurrent tasks",
                    ignoredUpdateTriggers, minimumTimeDelta));
                ignoredUpdateTriggers = 0;
            }

            // perform the actual update
            latestCpuLoad = dataSource.getTotalCPUUsage();
            latestAvailableRam = dataSource.getFreeRAM();
            cpuLoadRingBuffer.add(latestCpuLoad);
            failureCount = 0;
        } catch (OperatingSystemException e) {
            failureCount++;
            log.error("Error retrieving system load information (sequential failure count: " + failureCount + ")", e);
        }

    }

    /**
     * Fetches a snapshot of the aggregated system load information.
     * 
     * @param maxSamples the maximum number of samples to include in average values
     * @return an immutable holder of the generated data
     */
    public synchronized SystemLoadInformation getSystemLoadInformation(int maxSamples) {
        return new SystemLoadInformation(cpuLoadRingBuffer.getAverageOfLatest(maxSamples), latestCpuLoad, latestAvailableRam);
    }
}
