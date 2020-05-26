/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataService;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;

/**
 * Implementation of {@link SystemMonitoringDataService}.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class SystemMonitoringDataServiceImpl implements SystemMonitoringDataService {

    private final SystemIntegrationAdapter adapter = SystemIntegrationEntryPoint.getAdapter();

    // TODO remove in actual release
    protected void activate(BundleContext bundleContext) {}

    @Override
    public boolean isProvidingActualSystemData() {
        return adapter.isProvidingActualSystemData();
    }

    @Override
    public double getTotalCPUUsage() throws OperatingSystemException {
        return adapter.getTotalCPUUsage();
    }

    @Override
    public double getProcessCPUUsage(Long pid) throws OperatingSystemException {
        return adapter.getProcessCPUUsage(pid);
    }

    @Override
    public double getReportedCPUIdle() throws OperatingSystemException {
        return adapter.getReportedCPUIdle();
    }

    @Override
    public long getTotalSystemRAM() throws OperatingSystemException {
        return adapter.getCachedTotalSystemRam();
    }

    @Override
    public long getProcessRAMUsage(Long pid) throws OperatingSystemException {
        return adapter.getProcessRAMUsage(pid);
    }

    @Override
    public long getTotalUsedRAM() throws OperatingSystemException {
        return adapter.getTotalUsedRAM();
    }

    @Override
    public long getFreeRAM() throws OperatingSystemException {
        return adapter.getFreeRAM();
    }

    @Override
    public double getTotalUsedRAMPercentage() throws OperatingSystemException {
        return adapter.getTotalUsedRAMPercentage();
    }

    @Override
    public double getProcessRAMPercentage(Long pid) throws OperatingSystemException {
        return adapter.getProcessRAMPercentage(pid);
    }

    @Override
    public Map<Long, String> getProcesses() throws OperatingSystemException {
        return adapter.getProcesses();
    }

    @Override
    public Map<Long, String> getChildProcessesAndIds(Long pid) throws OperatingSystemException {
        return adapter.getChildProcessesAndIds(pid);
    }

    @Override
    public List<ProcessInformation> getFullChildProcessInformation(long pid) throws OperatingSystemException {
        return adapter.getFullChildProcessInformation(pid);
    }

}
