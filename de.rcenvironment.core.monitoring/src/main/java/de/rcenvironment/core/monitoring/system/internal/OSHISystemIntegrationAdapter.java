/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.monitoring.system.internal;

import java.util.List;

import de.rcenvironment.core.eventlog.api.EventLog;
import de.rcenvironment.core.eventlog.api.EventType;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * @author Brigitte Boden
 * @author Dominik Schneider
 * @author Robert Mischke (event logging)
 */
public class OSHISystemIntegrationAdapter implements SystemIntegrationAdapter {

    private static final long BYTES_TO_MIB_FACTOR = 1048576;

    private final CentralProcessor processor;

    private final OperatingSystem os;

    private final GlobalMemory memory;

    private final long cachedTotalSystemRam;

    private final int selfJavaPid;

    private final int logicalProcessors;

    public OSHISystemIntegrationAdapter() {
        SystemInfo sysInfo = new SystemInfo();
        this.os = sysInfo.getOperatingSystem();
        this.selfJavaPid = this.os.getProcessId();

        HardwareAbstractionLayer hardware = sysInfo.getHardware();
        this.processor = hardware.getProcessor();
        this.logicalProcessors = this.processor.getLogicalProcessorCount();
        this.memory = hardware.getMemory();
        long totalSystemRamBytes = this.memory.getTotal(); // fetch once
        this.cachedTotalSystemRam = convertBytesToMiB(totalSystemRamBytes);

        // fetch system information as seen by the JVM as well; not strictly in the right place (as this is independent from OSHI),
        // but kept simple for now -- if there is ever more than one system backend, move this event log generation outside
        Runtime systemRuntime = Runtime.getRuntime();
        String cpuCoreCount = Integer.toString(systemRuntime.availableProcessors());
        String maxHeapSize = Long.toString(systemRuntime.maxMemory());

        // write system and JVM information to the event log
        EventLog.append(EventLog.newEntry(EventType.SYSMON_INITIALIZED)
            .set(EventType.Attributes.SYSTEM_TOTAL_RAM, totalSystemRamBytes)
            .set(EventType.Attributes.SYSTEM_LOGICAL_CPUS, logicalProcessors)
            .set(EventType.Attributes.JVM_PROCESSOR_COUNT, cpuCoreCount)
            .set(EventType.Attributes.JVM_HEAP_LIMIT, maxHeapSize)
            .set(EventType.Attributes.JVM_PID, selfJavaPid));
    }

    @Override
    public int getSelfJavaPid() {
        return this.selfJavaPid;
    }

    @Override
    public long[] getSystemCpuLoadTicks() {
        return this.processor.getSystemCpuLoadTicks();
    }

    @Override
    public double getSystemCpuLoadBetweenTicks(long[] previousCpuTicks) {
        return this.processor.getSystemCpuLoadBetweenTicks(previousCpuTicks);
    }

    @Override
    public long getTotalRam() {
        return this.cachedTotalSystemRam;
    }

    @Override
    public long getTotalRamUsage() {
        return this.cachedTotalSystemRam - convertBytesToMiB(this.memory.getAvailable());
    }

    @Override
    public List<OSProcess> getAllProcesses() {
        return this.os.getProcesses(OperatingSystem.ProcessFiltering.VALID_PROCESS, null, 0);
    }

    private long convertBytesToMiB(long valueInByte) {
        return valueInByte / BYTES_TO_MIB_FACTOR;
    }

    @Override
    public int getLogicalProcessorsCount() {
        return this.logicalProcessors;
    }

}
