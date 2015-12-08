/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import java.util.Map;

import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * A service for fetching system monitoring data for the local instance's host system. The available methods allow to get system resource
 * information, kill processes and their children.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
// TODO split into two interfaces: the system information methods, and the delegating snapshot fetch method? - misc_ro
public interface SystemMonitoringService {

    /**
     * The multiplier to convert an internal percentage value to human-readable percentage values (0..100).
     */
    double PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER = 100.0;

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
    double getIdle() throws OperatingSystemException;

    /**
     * Get total available system RAM in MB.
     * 
     * @throws OperatingSystemException if getting system ram fails.
     * @return total system available system RAM.
     */
    long getTotalRAM() throws OperatingSystemException;

    /**
     * Get used RAM of a specific process in MB.
     * 
     * @param pid process id of specific process.
     * @throws OperatingSystemException if getting used ram of specific process fails.
     * @return ram usage of a specific process.
     */
    long getProcessRAMUsage(Long pid) throws OperatingSystemException;

    /**
     * Get used RAM in percent.
     * 
     * @throws OperatingSystemException if getting total ram usage fails.
     * @return used ram in percent.
     */
    double getTotalUsedRAMPercentage() throws OperatingSystemException;

    /**
     * Get used RAM of a specific process in percent.
     * 
     * @param pid process id of a specific process.
     * @throws OperatingSystemException if getting ram usage of specific process fails.
     * @return used ram of a specific process in percent.
     */
    double getProcessRAMPercentage(Long pid) throws OperatingSystemException;

    /**
     * Get local disk usage in MB.
     * 
     * @return local disk usage.
     * @throws OperatingSystemException if getting local disk usage fails.
     */
    long getLocalDiskUsage() throws OperatingSystemException;

    /**
     * Get free local disk space in MB.
     * 
     * @throws OperatingSystemException if getting local free disk space fails.
     * @return free local disk space.
     */
    long getFreeLocalDiskSpace() throws OperatingSystemException;

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
    Map<Long, String> getChildProcesses(Long ppid) throws OperatingSystemException;

    /**
     * Get a {@link SystemMonitoringDataSnapshot} which contains all system resource informations at once. This limits network traffic.
     * 
     * @param node The {@link NodeIdentifier} of the corresponding publisher. If <code>null</code> the reference will be gotten from the
     *        local platform.
     * @throws OperatingSystemException if getting {@link SystemMonitoringDataSnapshot} fails.
     * @throws RemoteOperationException on communication failures.
     * @return {@link SystemMonitoringDataSnapshot}.
     */
    SystemMonitoringDataSnapshot getMonitoringModel(NodeIdentifier node) throws OperatingSystemException, RemoteOperationException;

    /**
     * Kills a specific process.
     * 
     * @param pid process id of process which should be killed.
     * @param force Force the process to be killed with signum -9.
     * @throws OperatingSystemException if killing a specific process fails.
     */
    void kill(Long pid, Boolean force) throws OperatingSystemException;

}
