/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.List;

import oshi.software.os.OSProcess;

/**
 * A common interface for low-level system integration operations.
 *
 * @author Robert Mischke
 * @author Dominik Schneider
 */
public interface SystemIntegrationAdapter {

    /**
     * @return the JVM's PID
     */
    int getSelfJavaPid();

    /**
     * Get Processor CPU Load tick counters.
     * 
     * @return An array of 7 long values representing time spent in different system states. Use this only for creating snapshots and cpu
     *         usage calculation.
     */
    long[] getSystemCpuLoadTicks();

    /**
     * Returns the "recent cpu usage" for the whole system by counting ticks from {@link getSystemCpuLoadTicks()} between the user-provided
     * value from a previous call.
     * 
     * @param previousCpuTicks A tick array from a previous call to {@link getSystemCpuLoadTicks()}.
     * @return CPU load between 0 and 1 (100%)
     */
    double getSystemCpuLoadBetweenTicks(long[] previousCpuTicks);

    /**
     * Get the total system RAM in MB.
     * 
     * @return system RAM in MB
     */
    long getTotalRam();

    /**
     * Get the total used system RAM in MB.
     * 
     * @return used system RAM in MB
     */
    long getTotalRamUsage();

    /**
     * Gets all currently running processes. This is the <b>only</b> method to retrieve processes as getting all information at once is the
     * fastest option in OSHI.
     * 
     * 
     * @return An unmodifiable list of all running{@link OSProcess}es.
     */
    List<OSProcess> getAllProcesses();

    /**
     * Gets the number of logical processors.
     * 
     * @return number of logical processors
     */
    int getLogicalProcessorsCount();

}
