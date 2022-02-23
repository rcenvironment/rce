/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.model.AverageOfDoubles;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.OSFamily;
import oshi.software.os.OSProcess;

/**
 * This class calculates system monitoring data of the regularly collected "raw" data of the system.
 * 
 * @author Dominik Schneider
 *
 */
public final class SystemMonitoringDataProcessor {

    private final long totalRam;

    private final RingBuffer<SystemDataSnapshot> ringBuffer;

    private final SystemIntegrationAdapter adapter;

    private final Log log = LogFactory.getLog(this.getClass());

    private FullSystemAndProcessDataSnapshot cachedFullSnapshot;

    private SystemLoadInformation defaultLoadInformation;

    private SystemDataSnapshot cachedDataSnapshot;

    // TODO review synchronization, is it necessary so synchronize access to cached data (worst case: doubled calculation of snapshot?)?
    /**
     * Returns a SystemMonitoringData Processor to calculate System Monitoring Data.
     * 
     * @param ringBuffer containing the data for calculation
     * @param adapter providing system information
     */
    public SystemMonitoringDataProcessor(RingBuffer<SystemDataSnapshot> ringBuffer, SystemIntegrationAdapter adapter) {
        this.ringBuffer = ringBuffer;
        this.adapter = adapter;
        this.totalRam = this.adapter.getTotalRam();
        // default non-empty snapshot
        this.cachedDataSnapshot = new SystemDataSnapshot(0, null, 0, 0, null, null);
        this.cachedFullSnapshot =
            new FullSystemAndProcessDataSnapshot(0, 0, this.totalRam, 0, Collections.emptyList(),
                Collections.emptyList());
        this.defaultLoadInformation = new SystemLoadInformation(new AverageOfDoubles(), 0, 0);
    }

    /**
     * Create a full system monitoring data snapshot. The latest snapshot is always cached, so no calculations take place if no new data is
     * available.
     * 
     * @return {@link FullSystemAndProcessDataSnapshot}
     */
    public FullSystemAndProcessDataSnapshot createFullSystemSnapshot() {
        final List<SystemDataSnapshot> snapshots = this.ringBuffer.getLastItems(2);
        synchronized (this.cachedDataSnapshot) {
            if (snapshots.size() > 1 && this.cachedDataSnapshot != snapshots.get(1)) {
                this.cachedDataSnapshot = snapshots.get(1);
                this.cachedFullSnapshot = createFullSystemSnapshot(snapshots.get(1), snapshots.get(0));
            }
        }
        return this.cachedFullSnapshot;

    }

    /**
     * Retrieve the information about the system load.
     * 
     * @param maxSamples Maximum count of samples taken into account for calculation
     * @return {@link SystemLoadInformation}
     */
    public SystemLoadInformation getSystemLoadInformation(int maxSamples) {
        final List<SystemDataSnapshot> snapshots = this.ringBuffer.getLastItems(maxSamples);
        if (snapshots.size() > 0) {
            return getSystemLoadInformation(snapshots);
        } else {
            return this.defaultLoadInformation;
        }

    }

    private FullSystemAndProcessDataSnapshot createFullSystemSnapshot(SystemDataSnapshot newSnapshot, SystemDataSnapshot oldSnapshot) {

        final double cpuUsage = newSnapshot.getTotalCpuUsage();
        final double idle = 1 - cpuUsage;
        final long ramUsage = newSnapshot.getTotalSystemRamUsage();

        if (oldSnapshot.getRceOwnProcess() != null) {
            final Set<OSProcess> oldProcesses = oldSnapshot.getRceSubProcesses();
            oldProcesses.add(oldSnapshot.getRceOwnProcess());

            final ProcessInformation rceProcess =
                createProcessInformationFromOsProcess(newSnapshot.getRceOwnProcess(), newSnapshot.getRceSubProcesses(), oldProcesses);
            final List<ProcessInformation> subProcesses = rceProcess.getChildren();
            final List<ProcessInformation> rceProcesses = new ArrayList<>();
            rceProcesses.add(rceProcess);
            orderOwnAndSubProcesses(rceProcesses, subProcesses);

            return new FullSystemAndProcessDataSnapshot(cpuUsage, ramUsage, this.totalRam, idle,
                subProcesses, rceProcesses);
        } else {
            return new FullSystemAndProcessDataSnapshot(cpuUsage, ramUsage, this.totalRam, idle,
                Collections.emptyList(), Collections.emptyList());
        }

    }

    private SystemLoadInformation getSystemLoadInformation(List<SystemDataSnapshot> snapshots) {
        double latestCpuLoad = snapshots.get(snapshots.size() - 1).getTotalCpuUsage();
        long latestRamUsage = snapshots.get(snapshots.size() - 1).getTotalSystemRamUsage();

        RingBufferOfDouble doubleRingBuffer = new RingBufferOfDouble(snapshots.size());
        snapshots.forEach(element -> doubleRingBuffer.add(element.getTotalCpuUsage()));

        return new SystemLoadInformation(doubleRingBuffer.getAverageOfLatest(snapshots.size()), latestCpuLoad, latestRamUsage);
    }

    /**
     * Creates the full ProcessInformation tree (recursively).
     * 
     * @param parent The root OSProcess
     * @param processes latest OSProcess
     * @param oldProcesses previos OSProcess
     * @return ProcessInformation of the root OSProcess
     */
    private ProcessInformation createProcessInformationFromOsProcess(OSProcess parent, Set<OSProcess> processes,
        Set<OSProcess> oldProcesses) {
        double cpuUsage = 0.0;
        for (OSProcess oldProcess : oldProcesses) {
            if (parent.getProcessID() == oldProcess.getProcessID()) {
                // retrieving cpu usage (compared with previous snapshot)
                cpuUsage = parent.getProcessCpuLoadBetweenTicks(oldProcess) / this.adapter.getLogicalProcessorsCount();
                break;
            }
        }

        // retrieving ram usage
        final long ramUsage = parent.getResidentSetSize();

        // handling the children (recursively to build hierarchical structure)
        final List<OSProcess> directChildren = processes.stream()
            .filter(child -> child.getParentProcessID() == parent.getProcessID())
            .collect(Collectors.toList());
        final List<ProcessInformation> children = new ArrayList<>();
        directChildren.forEach(child -> {
            children.add(createProcessInformationFromOsProcess(child, processes, oldProcesses));
        });
        return new ProcessInformation(parent.getProcessID(), parent.getName(), children, cpuUsage, ramUsage);
    }

    private void orderOwnAndSubProcesses(final List<ProcessInformation> ownProcesses, final List<ProcessInformation> subProcesses) {
        if (OSFamily.isLinux()) {
            // move Linux "Webkit*" processes (related to the integrated help browser) to "own processes"
            final List<ProcessInformation> processesToMove = subProcesses.stream()
                .filter(process -> process.getName().startsWith("WebKit"))
                .collect(Collectors.toList());
            if (!processesToMove.isEmpty()) {
                ownProcesses.addAll(processesToMove);
                subProcesses.removeAll(processesToMove);
            }
        } else if (OSFamily.isWindows()) {
            // eliminate "conhost" on Windows systems (see #17328)
            // as this has only been observed during Maven tests, just drop it instead of muddying the "own processes" pool
            final List<ProcessInformation> processesToDelete = subProcesses.stream()
                .filter(process -> process.getName().equals("conhost"))
                .collect(Collectors.toList());
            if (!processesToDelete.isEmpty()) {
                log.debug("Eliminated 'conhost' from the list of sub-processes; "
                    + "this is normal during integration tests, please report any other sightings");
                subProcesses.removeAll(processesToDelete);
            }
        }
    }

}
