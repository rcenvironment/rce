/*
 * Copyright 2020-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import oshi.software.os.OSProcess;

/**
 * This integration test class tests the OSHI library for consistency (high level and roughly). Only runs as junit plug-in test.
 * 
 * @author Dominik Schneider
 *
 */
public class SystemMonitoringOSHILibraryTest {

    SystemIntegrationAdapter adapter = SystemIntegrationEntryPoint.getAdapter();

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Testing CPU data provided by OSHI.
     */
    @Test
    public void smokeTestCPURequests() {
        long[] cpuTicks = this.adapter.getSystemCpuLoadTicks();
        assertNotNull("CPU ticks could not be acquired by OSHI", cpuTicks);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            log.debug("Got interrupted while waiting for CPU data: " + e);
            fail("Got interrupted while waiting for CPU data");
        }
        double cpuUsage = this.adapter.getSystemCpuLoadBetweenTicks(cpuTicks);
        assertTrue("CPU usage as reported by OSHI not in [0,1]", Double.compare(0.0, cpuUsage) <= 0 && Double.compare(cpuUsage, 1.0) <= 0);

        assertTrue("Number of logical processors is <= 0", this.adapter.getLogicalProcessorsCount() > 0);
    }

    /**
     * Testing RAM information provided by OSHI.
     */
    @Test
    public void smokeTestMemoryRequests() {
        long totalRam = this.adapter.getTotalRam();
        assertTrue("Total RAM reported by OSHI is not valid", Long.compare(0, totalRam) < 0);

        long ramUsage = this.adapter.getTotalRamUsage();
        assertTrue("RAM usage as reported by OSHI not in (0,totalRAM]",
            Long.compare(0, ramUsage) < 0 && Long.compare(ramUsage, totalRam) <= 0);
    }

    /**
     * Testing process information provided by OSHI.
     */
    @Test
    public void smokeTestProcessHandling() {
        List<OSProcess> processList = this.adapter.getAllProcesses();
        assertNotNull("Process list is null", processList);
        assertFalse("Process list is empty", processList.isEmpty());

        Optional<OSProcess> ownProcess = processList.stream()
            .filter(process -> process.getProcessID() == this.adapter.getSelfJavaPid())
            .findAny();
        assertTrue("Own process not found in process list", ownProcess.isPresent());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.debug("Got interrupted while waiting for CPU data: " + e);
            fail("Got interrupted while waiting for CPU data");
        }

        List<OSProcess> secondProcessList = this.adapter.getAllProcesses();
        Optional<OSProcess> secondOwnProcess = secondProcessList.stream()
            .filter(process -> process.getProcessID() == this.adapter.getSelfJavaPid())
            .findAny();
        assertTrue("Own process not found in process list with second query", secondOwnProcess.isPresent());

        double cpuUsage = secondOwnProcess.get().getProcessCpuLoadBetweenTicks(ownProcess.get());
        assertTrue("CPU usage of own process reported by OSHI is not in [0,logicalProcessorCount]",
            Double.compare(0, cpuUsage) <= 0 && Double.compare(cpuUsage, this.adapter.getLogicalProcessorsCount()) <= 0);
    }

}
