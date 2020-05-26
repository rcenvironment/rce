/*
 * Copyright 2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.List;
import java.util.Map;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;

/**
 * A common interface for low-level system integration operations.
 *
 * @author Robert Mischke
 */
public interface SystemIntegrationAdapter {

    /**
     * @return true if this adapter's return values represent actual system data; false if they are placeholders
     */
    boolean isProvidingActualSystemData();

    /**
     * @return the total amount of system RAM, determined once; TODO unit?
     */
    long getCachedTotalSystemRam();

    /**
     * @return true whether self and launcher PIDs and process states are available. May trigger a fetching attempt of those ids internally.
     */
    boolean areSelfPidsAndProcessStatesAvailable();

    /**
     * @return the JVM's PID; available if {@link #areSelfPidsAndProcessStatesAvailable()} returned true
     */
    long getSelfJavaPid();

    /**
     * @return the JVM's process name; available if {@link #areSelfPidsAndProcessStatesAvailable()} returned true
     */
    String getSelfJavaProcessName();

    /**
     * @return the native launcher's PID; available if {@link #areSelfPidsAndProcessStatesAvailable()} returned true
     */
    long getSelfLauncherPid();

    /**
     * @return the native launcher's process name; available if {@link #areSelfPidsAndProcessStatesAvailable()} returned true
     */
    String getSelfLauncherProcessName();

    /**
     * Get total system CPU usage in percent. Guaranteed to be either 0%..100%, or NaN in case of failure.
     * 
     * @throws OperatingSystemException if getting total cpu usage fails.
     * @return total system cpu usage.
     */
    double getTotalCPUUsage() throws OperatingSystemException;

    /**
     * Get CPU usage of a specific process in percent.
     * 
     * @param pid process id.
     * @throws OperatingSystemException if getting cpu usage fails.
     * @return cpu usage of specific process.
     */
    double getProcessCPUUsage(Long pid) throws OperatingSystemException;

    /**
     * Get CPU Idle.
     * 
     * @throws OperatingSystemException if getting cpu idle fails.
     * @return cpu idle.
     */
    double getReportedCPUIdle() throws OperatingSystemException;

    /**
     * Get the total available system RAM in MB.
     * 
     * @throws OperatingSystemException if getting system ram fails.
     * @return total system available system RAM.
     */
    long getTotalSystemRAM() throws OperatingSystemException;

    /**
     * Get used RAM of a specific process in MB.
     * 
     * @param pid process id of specific process.
     * @throws OperatingSystemException if getting used ram of specific process fails.
     * @return ram usage of a specific process.
     */
    long getProcessRAMUsage(Long pid) throws OperatingSystemException;

    /**
     * Get used RAM as an absolute value. Note that this value may be calculated from a percentage, so accuracy may be limited.
     * 
     * @throws OperatingSystemException if getting total ram usage fails.
     * @return used ram in percent.
     */
    long getTotalUsedRAM() throws OperatingSystemException;

    /**
     * The amount of available/free RAM in MiB. Note that this value may be calculated from a percentage, so accuracy may be limited.
     * 
     * @throws OperatingSystemException on failure to read system data
     * @return available/free physical RAM in MiB
     */
    long getFreeRAM() throws OperatingSystemException;

    /**
     * Get used RAM in percent, represented as 0..1.
     * 
     * @throws OperatingSystemException if getting total ram usage fails.
     * @return used ram in percent.
     */
    double getTotalUsedRAMPercentage() throws OperatingSystemException;

    /**
     * Get used RAM of a specific process in percent, represented as 0..1.
     * 
     * @param pid process id of a specific process.
     * @throws OperatingSystemException if getting ram usage of specific process fails.
     * @return used ram of a specific process in percent.
     */
    double getProcessRAMPercentage(Long pid) throws OperatingSystemException;

    /**
     * Get a map of all running processes. PID is mapped to the process name.
     * 
     * @throws OperatingSystemException if getting process list fails.
     * @return a list of pids.
     */
    Map<Long, String> getProcesses() throws OperatingSystemException;

    /**
     * Get a map of all children of a specific process. PID is mapped to the process name.
     * 
     * @param ppid process id of a parent process.
     * @throws OperatingSystemException if fetching the child processes fails
     * @return a list of pids. Returns null if there are no children.
     */
    Map<Long, String> getChildProcessesAndIds(Long ppid) throws OperatingSystemException;

    /**
     * Get detail information about the given process' child processes.
     * 
     * @param pid the ID of the parent process
     * @return a list of information wrappers for each child process
     * @throws OperatingSystemException if fetching the child processes fails
     */
    List<ProcessInformation> getFullChildProcessInformation(long pid) throws OperatingSystemException;
}
