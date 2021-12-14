/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.Set;

import oshi.software.os.OSProcess;

/**
 * This class represents a snapshot container for all system data regularly retrieved for later calculations and usage.
 * 
 * @author Dominik Schneider
 *
 */
public final class SystemDataSnapshot {

    private final long timestamp;

    private final long[] totalCpuTicks;

    private final double totalCpuUsage;

    private final long totalSystemRAMUsage;

    private final OSProcess rceOwnProcess;

    private final Set<OSProcess> rceSubProcesses;

    public SystemDataSnapshot(final long timestamp, final long[] totalCpuTicks, final double totalCpuUsage, final long totalSystemRAMUsage,
        final OSProcess rceOwnProcess, final Set<OSProcess> rceSubProcesses) {
        this.timestamp = timestamp;
        this.totalCpuTicks = totalCpuTicks;
        this.totalCpuUsage = totalCpuUsage;
        this.totalSystemRAMUsage = totalSystemRAMUsage;
        this.rceOwnProcess = rceOwnProcess;
        this.rceSubProcesses = rceSubProcesses;

    }

    /**
     * Get the timestamp of this snapshot in milliseconds.
     * 
     * @return the timestamp in ms.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the cpu ticks.
     * 
     * @return CPU ticks as long
     */
    public long[] getTotalCpuTicks() {
        return totalCpuTicks;
    }

    /**
     * Get the cpu usage.
     * 
     * @return CPU usage between 0 and 1.
     */
    public double getTotalCpuUsage() {
        return totalCpuUsage;
    }

    /**
     * Get the total usage of RAM (absolute).
     * 
     * @return the total usage of RAM
     */
    public long getTotalSystemRamUsage() {
        return totalSystemRAMUsage;
    }

    /**
     * Get the RCE main process(does NOT include subprocesses).
     * 
     * @return a list of all RCE processes
     */
    public OSProcess getRceOwnProcess() {
        return rceOwnProcess;
    }

    /**
     * Get a set of all child processes of RCE.
     * 
     * @return a list of all RCE child processes
     */
    public Set<OSProcess> getRceSubProcesses() {
        return rceSubProcesses;
    }

}
