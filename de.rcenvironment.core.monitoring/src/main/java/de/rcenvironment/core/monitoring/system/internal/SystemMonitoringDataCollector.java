/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import oshi.software.os.OSProcess;

/**
 * This class collects all system monitoring data on a regular basis and stores them in a {@link RingBuffer} for further usage.
 * 
 * @author Dominik Schneider
 *
 */
public final class SystemMonitoringDataCollector {

    private static final int FULL_COLLECTIONS_WITHOUT_USAGE = 6;

    private AsyncTaskService asyncTaskService;

    private boolean isRunning;

    private RingBuffer<SystemDataSnapshot> ringBuffer;

    private Future<?> schedulingFuture;

    private final SystemIntegrationAdapter adapter;

    private final AtomicInteger fullCollectionCounter = new AtomicInteger(0);

    private final Log log = LogFactory.getLog(SystemMonitoringDataCollector.class);

    /**
     * Controls collection of system monitoring data. Does <b>not</b> start data collection at initialization.
     * 
     * @param integrationAdapter SystemIntegrationAdapter providing system information
     * @param asyncService AsyncTaskService for scheduling data collection
     */
    public SystemMonitoringDataCollector(SystemIntegrationAdapter adapter, AsyncTaskService taskService) {

        if (adapter == null || taskService == null) {
            throw new IllegalArgumentException("Constructor parameters must not be null");
        }

        // setting up dependencies
        this.adapter = adapter;
        this.asyncTaskService = taskService;

        // scheduling tasks
        this.isRunning = false;
    }

    /**
     * Activates the collection of system monitoring data without process information. Multiple execution does not schedule multiple tasks.
     * 
     * @param itemNumber the maximal number of items available
     * @param interval the time in milliseconds between data collections
     */
    public void startCollection(int itemNumber, int interval) {
        if (!isRunning) {
            this.schedulingFuture =
                this.asyncTaskService.scheduleAtFixedInterval("System Monitoring", this::collectData, interval);
            log.debug("Scheduled system monitoring data collection");
            this.ringBuffer = new RingBuffer<SystemDataSnapshot>(itemNumber);
            this.isRunning = true;
        }
    }

    /**
     * Stops the collection of system monitoring data if collector is running.
     */
    public void stopCollection() {
        if (this.isRunning) {
            this.schedulingFuture.cancel(true);
            log.debug("Canceled system monitoring data collection");
        }
    }

    /**
     * Get whether the collector is running or not.
     * 
     * @return true if the collector is running
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * Enforces that full system monitoring data inclusive process information are collected. This method should be called each time when a
     * {@link FullSystemAndProcessDataSnapshot} is calculated.
     */
    public void resetPartialCollectionFallback() {
        fullCollectionCounter.set(FULL_COLLECTIONS_WITHOUT_USAGE);
    }

    /**
     * Retrieves the ring buffer containing the system monitoring data.
     * 
     * @return the ring buffer containing the system monitoring data, null if collection has not started yet
     */
    public RingBuffer<SystemDataSnapshot> getRingBuffer() {
        return this.ringBuffer;
    }

    private void collectData() {

        final long timeStamp = System.currentTimeMillis();
        final long[] totalCpuTicks = this.adapter.getSystemCpuLoadTicks();
        double totalCpuUsage = 0;
        if (this.ringBuffer.getLatestEntry().isPresent()) {
            totalCpuUsage = this.adapter.getSystemCpuLoadBetweenTicks(this.ringBuffer.getLatestEntry().get().getTotalCpuTicks());
        }
        final long totalRamUsage = this.adapter.getTotalRamUsage();
        List<OSProcess> allProcesses = new ArrayList<>();
        OSProcess rceProcess = null;
        Set<OSProcess> subProcesses = new HashSet<OSProcess>();
        if (fullCollectionCounter.get() > 0) {
            fullCollectionCounter.decrementAndGet();
            allProcesses = this.adapter.getAllProcesses();
            rceProcess = allProcesses.stream()
                .filter(process -> process.getProcessID() == this.adapter.getSelfJavaPid())
                .findAny()
                .get();
            subProcesses = getSubProcesses(rceProcess, allProcesses);
        }

        final SystemDataSnapshot snapshot =
            new SystemDataSnapshot(timeStamp, totalCpuTicks, totalCpuUsage, totalRamUsage, rceProcess, subProcesses);
        this.ringBuffer.add(snapshot);
    }

    private Set<OSProcess> getSubProcesses(OSProcess rootProcess, List<OSProcess> allProcesses) {
        Set<OSProcess> setToReturn = new HashSet<>();
        insertSubProcesses(rootProcess, allProcesses, setToReturn);
        setToReturn.remove(rootProcess);
        return setToReturn;
    }

    private void insertSubProcesses(OSProcess rootProcess, List<OSProcess> allProcesses, Set<OSProcess> targetSet) {
        // gets all subprocess recursively
        // HashSet is used for fast handling
        List<OSProcess> directChildren = allProcesses.stream()
            .filter(process -> process.getParentProcessID() == rootProcess.getProcessID())
            .collect(Collectors.toList());
        for (OSProcess subProcess : directChildren) {
            insertSubProcesses(subProcess, allProcesses, targetSet);
        }
        targetSet.add(rootProcess);
    }

}
