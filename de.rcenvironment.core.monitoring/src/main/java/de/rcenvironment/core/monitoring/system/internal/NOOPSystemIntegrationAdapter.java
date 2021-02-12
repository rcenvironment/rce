/*
 * Copyright 2020-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;

/**
 * A fallback {@link SystemIntegrationAdapter} implementation that provides no-operation behavior. For system monitoring, default values are
 * returned. For active operations, like killing processes, the JavaDoc of the method should state whether the call is ignored, or an error
 * is thrown.
 *
 * @author Robert Mischke
 */
public class NOOPSystemIntegrationAdapter implements SystemIntegrationAdapter {

    @Override
    public boolean isProvidingActualSystemData() {
        return false;
    }

    @Override
    public long getCachedTotalSystemRam() {
        return 0;
    }

    @Override
    public boolean areSelfPidsAndProcessStatesAvailable() {
        return false;
    }

    @Override
    public long getSelfJavaPid() {
        return 0;
    }

    @Override
    public String getSelfJavaProcessName() {
        return null;
    }

    @Override
    public long getSelfLauncherPid() {
        return 0;
    }

    @Override
    public String getSelfLauncherProcessName() {
        return null;
    }

    @Override
    public double getTotalCPUUsage() throws OperatingSystemException {
        return 0;
    }

    @Override
    public double getProcessCPUUsage(Long pid) throws OperatingSystemException {
        return 0;
    }

    @Override
    public double getReportedCPUIdle() throws OperatingSystemException {
        return 0;
    }

    @Override
    public long getTotalSystemRAM() throws OperatingSystemException {
        return 0;
    }

    @Override
    public long getProcessRAMUsage(Long pid) throws OperatingSystemException {
        return 0;
    }

    @Override
    public long getTotalUsedRAM() throws OperatingSystemException {
        return 0;
    }

    @Override
    public long getFreeRAM() throws OperatingSystemException {
        return 0;
    }

    @Override
    public double getTotalUsedRAMPercentage() throws OperatingSystemException {
        return 0;
    }

    @Override
    public double getProcessRAMPercentage(Long pid) throws OperatingSystemException {
        return 0;
    }

    @Override
    public Map<Long, String> getProcesses() throws OperatingSystemException {
        return new HashMap<>();
    }

    @Override
    public Map<Long, String> getChildProcessesAndIds(Long ppid) throws OperatingSystemException {
        return new HashMap<>();
    }

    @Override
    public List<ProcessInformation> getFullChildProcessInformation(long pid) throws OperatingSystemException {
        return new ArrayList<>();
    }

}
