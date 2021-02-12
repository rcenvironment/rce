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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.OperatingSystemException.ErrorType;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * @author Brigitte Boden
 *
 */
public class OSHISystemIntegrationAdapter implements SystemIntegrationAdapter {

    private static final int WAITING_TIME_IN_MS = 1000;

    private static final double NANOSEC_TO_MSEC_FACTOR = 1000000d;

    private static final long BYTES_TO_MIB_FACTOR = 1048576;

    private SystemInfo sysInfo;

    private CentralProcessor processor;

    private OperatingSystem os;

    private long cachedTotalSystemRam;

    private int selfJavaPid;

    private int selfLauncherPid;

    private volatile double totalCPUUsage;

    private ConcurrentHashMap<Integer, Double> cpuUsagePerTask;

    private final Log log = LogFactory.getLog(getClass());

    public OSHISystemIntegrationAdapter() {
        sysInfo = new SystemInfo();
        processor = sysInfo.getHardware().getProcessor();
        os = sysInfo.getOperatingSystem();
        cachedTotalSystemRam = convertBytesToMiB(sysInfo.getHardware().getMemory().getTotal());
        selfJavaPid = sysInfo.getOperatingSystem().getProcessId();
        selfLauncherPid = sysInfo.getOperatingSystem().getProcess(selfJavaPid).getParentProcessID();
        totalCPUUsage = 0;
        cpuUsagePerTask = new ConcurrentHashMap<Integer, Double>();
    }

    @Override
    public boolean isProvidingActualSystemData() {
        return true;
    }

    @Override
    public long getCachedTotalSystemRam() {
        return cachedTotalSystemRam;
    }

    @Override
    public boolean areSelfPidsAndProcessStatesAvailable() {
        return true;
    }

    @Override
    public long getSelfJavaPid() {
        return selfJavaPid;
    }

    @Override
    public String getSelfJavaProcessName() {
        return os.getProcess(selfJavaPid).getName();
    }

    @Override
    public long getSelfLauncherPid() {
        return selfLauncherPid;
    }

    @Override
    public String getSelfLauncherProcessName() {
        return os.getProcess(selfLauncherPid).getName();
    }

    @Override
    public double getTotalCPUUsage() throws OperatingSystemException {
        // As computing the value requires some waiting time,
        // return the last computed value and trigger the next computation
        ConcurrencyUtils.getAsyncTaskService().execute("Calculate total CPU usage", () -> {
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(WAITING_TIME_IN_MS);
            } catch (InterruptedException e) {
                log.debug("Waiting was interrupted; probably because RCE was shutdown");
            }
            totalCPUUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        });

        return totalCPUUsage;
    }

    @Override
    public double getProcessCPUUsage(Long pid) throws OperatingSystemException {
        if (pid == null) {
            throw new OperatingSystemException(ErrorType.NO_SUCH_PROCESS);
        }
        Integer processId = Math.toIntExact(pid);
        // As computing the value requires some waiting time,
        // we return the last computed value and trigger the next computation
        ConcurrencyUtils.getAsyncTaskService().execute("Calculate CPU usage for pid " + processId,
            () -> this.updateCpuUsageForProcess(processId));

        return cpuUsagePerTask.getOrDefault(processId, 0.0);
    }

    private void updateCpuUsageForProcess(final int processId) {
        final int numberOfCpus = processor.getLogicalProcessorCount();
        OSProcess process = os.getProcess(processId);
        if (process == null) {
            cpuUsagePerTask.remove(processId);
            return;
        }

        long previousNanoTime = System.nanoTime();
        long previousProcessTime = process.getKernelTime() + process.getUserTime();
        try {
            Thread.sleep(WAITING_TIME_IN_MS);
        } catch (InterruptedException e) { // NOSONAR
            /*
             * We explicitly do not rethrow the interrupted exception here, as the most likely case for this exception is that RCE is
             * currently shutting down. Hence, we merely log this exception and abort the update in order to allow the calling process to
             * finish termination as quickly as possible
             */
            log.debug("Waiting was interrupted; probably because RCE was shutdown", e);
            return;
        }

        // We have to re-get the process here as it might have been terminated in the meantime
        process = os.getProcess(processId);
        if (process == null) {
            cpuUsagePerTask.remove(processId);
            return;
        }

        long currentNanoTime = System.nanoTime();
        long currentProcessTime = process.getKernelTime() + process.getUserTime();
        long processTimeDifference = currentProcessTime - previousProcessTime;
        double elapsedTime = (currentNanoTime - previousNanoTime) / NANOSEC_TO_MSEC_FACTOR;
        cpuUsagePerTask.put(processId, (processTimeDifference / elapsedTime) / numberOfCpus);
    }

    @Override
    public double getReportedCPUIdle() throws OperatingSystemException {
        // CURRENTLY NOT USED
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTotalSystemRAM() throws OperatingSystemException {
        return getCachedTotalSystemRam();
    }

    @Override
    public long getProcessRAMUsage(Long pid) throws OperatingSystemException {
        if (pid != null) {
            OSProcess p = os.getProcess(Math.toIntExact(pid));
            if (p != null) {
                return p.getResidentSetSize();
            }
            // p can be null if the process has ended (race condition)
            return 0;
        } else {
            // In case of null id
            throw new OperatingSystemException(ErrorType.NO_SUCH_PROCESS);
        }
    }

    @Override
    public long getTotalUsedRAM() throws OperatingSystemException {
        return getCachedTotalSystemRam() - convertBytesToMiB(sysInfo.getHardware().getMemory().getAvailable());
    }

    @Override
    public long getFreeRAM() throws OperatingSystemException {
        return convertBytesToMiB(sysInfo.getHardware().getMemory().getAvailable());
    }

    @Override
    public double getTotalUsedRAMPercentage() throws OperatingSystemException {
        return (double) getTotalUsedRAM() / getCachedTotalSystemRam();
    }

    @Override
    public double getProcessRAMPercentage(Long pid) throws OperatingSystemException {
        // CURRENTLY NOT USED
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, String> getProcesses() throws OperatingSystemException {
        Map<Long, String> procMap = new HashMap<Long, String>();
        for (OSProcess process : sysInfo.getOperatingSystem().getProcesses()) {
            procMap.put(Integer.toUnsignedLong(process.getProcessID()), process.getName());
        }
        return procMap;
    }

    @Override
    public Map<Long, String> getChildProcessesAndIds(Long ppid) throws OperatingSystemException {
        Map<Long, String> childMap = new HashMap<Long, String>();
        if (ppid != null) {
            for (ProcessInformation p : getFullChildProcessInformation(ppid)) {
                childMap.put(p.getPid(), p.getName());
            }
        }
        return childMap;
    }

    @Override
    public List<ProcessInformation> getFullChildProcessInformation(long pid) throws OperatingSystemException {
        List<ProcessInformation> children = new ArrayList<>();
        for (OSProcess process : sysInfo.getOperatingSystem().getChildProcesses(Math.toIntExact(pid), 0, null)) {
            ProcessInformation p = new ProcessInformation(process.getProcessID(), process.getName(),
                getFullChildProcessInformation(process.getProcessID()),
                getProcessCPUUsage((long) process.getProcessID()),
                getProcessRAMUsage((long) process.getProcessID()));
            children.add(p);
        }

        return children;
    }

    private long convertBytesToMiB(long valueInByte) {
        return valueInByte / BYTES_TO_MIB_FACTOR;
    }

}
