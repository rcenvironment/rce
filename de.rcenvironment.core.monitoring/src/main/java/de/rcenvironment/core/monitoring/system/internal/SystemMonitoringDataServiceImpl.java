/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Humidor;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.ptql.ProcessFinder;
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

    private static final Sigar SIGAR = new Sigar();

    private static final int CONVERSION_FACTOR_1K = 1024;

    private static final double CONVERSION_FACTOR_PERCENT = 100;

    private static final int SIG_ID_KILL = 15;

    private static final int SIG_ID_FORCE_KILL = -9;

    private static final String NO_SUCH_PROCESS_MESSAGE = "No such process";

    private static final String ACCESS_DENIED_EXCEPTION = "Access is denied.";

    private static final Log LOGGER = LogFactory.getLog(SystemMonitoringDataServiceImpl.class);

    private static final int TOTAL_CPU_NAN_MAX_RETRY_COUNT = 50; // maximum observed in local testing was ~20 retries at 1 msec wait

    private static final int TOTAL_CPU_NAN_RETRY_WAIT_MSEC = 1;

    private long cachedTotalSystemRam;

    protected void activate(BundleContext bundleContext) {
        try {
            cachedTotalSystemRam = fetchTotalSystemRAM();
        } catch (OperatingSystemException e) {
            // TODO or re-throw the exception and refuse to start instead?
            cachedTotalSystemRam = 0;
            LOGGER.error("Failed to initialize total system RAM: " + e.toString());
        }
    }

    @Override
    public double getTotalCPUUsage() throws OperatingSystemException {
        try {
            double cpuUsage;
            final SigarProxy sigarProxy = Humidor.getInstance().getSigar();
            cpuUsage = sigarProxy.getCpuPerc().getCombined();
            if (Double.isNaN(cpuUsage)) {
                int retryCount = 1;
                while (true) {
                    cpuUsage = sigarProxy.getCpuPerc().getCombined();
                    if (!Double.isNaN(cpuUsage)) {
                        // valid
                        LOGGER.debug("Fetched valid CPU load data after " + retryCount + " immediate retries");
                        break;
                    }
                    if (retryCount < TOTAL_CPU_NAN_MAX_RETRY_COUNT) {
                        try {
                            Thread.sleep(TOTAL_CPU_NAN_RETRY_WAIT_MSEC);
                        } catch (InterruptedException e) {
                            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_CPU_USAGE,
                                "Interrupted while waiting for CPU load data retry");
                        }
                        retryCount++;
                    } else {
                        throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_CPU_USAGE,
                            "Failed to fetch valid CPU load data even after " + retryCount + " immediate retries");
                    }
                }
            }
            return SystemMonitoringUtils.clampToPercentageOrNAN(cpuUsage);
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_CPU_USAGE);
        }
    }

    @Override
    public double getProcessCPUUsage(Long pid) throws OperatingSystemException {
        double processCPUusage = 0;
        int watch = 0;
        try {
            if (pid == null) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS);
            }

            // TODO review this for NaN handling; this only tries to prevent 0.0 values
            do {
                synchronized (SIGAR) {
                    processCPUusage = SIGAR.getProcCpu(pid).getPercent();
                }

                watch++;
                if (watch == 10) {
                    return processCPUusage;
                }
            } while (processCPUusage == 0.0);

        } catch (SigarException e) {
            if (e.getMessage().contains(NO_SUCH_PROCESS_MESSAGE)) {
                throw createNoSuchProcessException(pid);
            } else if (e.getMessage().contains(ACCESS_DENIED_EXCEPTION)) {
                throw createAccessDeniedException(pid);
            } else {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_CPU_PROCESS_USAGE,
                    " of process with pid: " + pid);
            }
        }

        synchronized (SIGAR) {
            try {
                processCPUusage /= SIGAR.getCpuList().length;
            } catch (SigarException e) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CPU_LIST);
            }
        }

        return SystemMonitoringUtils.clampToPercentageOrNAN(processCPUusage);
    }

    @Override
    public double getReportedCPUIdle() throws OperatingSystemException {
        try {
            double idle = 0.0;
            synchronized (SIGAR) {
                idle = SIGAR.getCpuPerc().getIdle();
            }
            return SystemMonitoringUtils.clampToPercentageOrNAN(idle);
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_IDLE);
        }
    }

    @Override
    public long getTotalSystemRAM() throws OperatingSystemException {
        return cachedTotalSystemRam;
    }

    @Override
    public long getProcessRAMUsage(Long pid) throws OperatingSystemException {
        try {
            if (pid == null) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS);
            }
            return Humidor.getInstance().getSigar().getProcMem(pid).getResident() / (CONVERSION_FACTOR_1K * CONVERSION_FACTOR_1K);
        } catch (SigarException e) {
            if (e.getMessage().contains(NO_SUCH_PROCESS_MESSAGE)) {
                throw createNoSuchProcessException(pid);
            } else if (e.getMessage().contains(ACCESS_DENIED_EXCEPTION)) {
                throw createAccessDeniedException(pid);
            } else {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_RAM_PROCESS_USAGE,
                    " of process with pid: " + pid);
            }
        }
    }

    @Override
    public long getTotalUsedRAM() throws OperatingSystemException {
        return (long) (cachedTotalSystemRam * getTotalUsedRAMPercentage());
    }

    @Override
    public long getFreeRAM() throws OperatingSystemException {
        return (long) (cachedTotalSystemRam * (1.0 - getTotalUsedRAMPercentage()));
    }

    @Override
    public double getTotalUsedRAMPercentage() throws OperatingSystemException {
        try {
            return Humidor.getInstance().getSigar().getMem().getUsedPercent() / CONVERSION_FACTOR_PERCENT;
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM_PERCENTAGE);
        }
    }

    @Override
    public double getProcessRAMPercentage(Long pid) throws OperatingSystemException {
        long totalRam = getTotalSystemRAM();
        if (totalRam == 0) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM_PERCENTAGE);
        }
        long ram = getProcessRAMUsage(pid);
        return ram / totalRam;
    }

    @Override
    public long getUsedLocalDiskSpace() throws OperatingSystemException {
        return getFileSystemUsage().getUsed();
    }

    @Override
    public long getFreeLocalDiskSpace() throws OperatingSystemException {
        return getFileSystemUsage().getFree();
    }

    @Override
    public Map<Long, String> getProcesses() throws OperatingSystemException {
        try {
            final Map<Long, String> processMap = new HashMap<>();
            final List<Long> processList = Arrays.<Long> asList(ArrayUtils.toObject(Humidor.getInstance().getSigar().getProcList()));

            for (Long pid : processList) {
                synchronized (SIGAR) {
                    // this is necessary!
                    long[] pids =
                        ProcessFinder.find(SIGAR,
                            PTQLWrapper.createQuery().createQueryString(PTQLWrapper.pid(), PTQLWrapper.eq()) + pid);
                    for (long process : pids) {
                        processMap.put(process, Humidor.getInstance().getSigar().getProcState(process).getName());
                    }
                }
            }
            return processMap;
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_PROCESS_LIST);
        }

    }

    @Override
    public Map<Long, String> getChildProcessesAndIds(Long pid) throws OperatingSystemException {

        if (pid == null) {
            return Collections.emptyMap();
        }

        final List<ProcessInformation> childList;
        childList = getFullChildProcessInformation(pid);

        final Map<Long, String> childMap = new HashMap<>();
        if (!childList.isEmpty()) {
            for (ProcessInformation process : childList) {
                childMap.put(process.getPid(), process.getName());
            }
        }
        return childMap;
    }

    @Override
    public void kill(Long pid, Boolean force) throws OperatingSystemException {
        try {
            LOGGER.info("Killing process with pid: " + pid);
            if (force) {
                synchronized (SIGAR) {
                    SIGAR.kill(pid, SIG_ID_FORCE_KILL);
                }
            } else {
                synchronized (SIGAR) {
                    SIGAR.kill(pid, SIG_ID_KILL);
                }
            }
        } catch (final SigarException e) {
            if (e.getMessage().contains("The parameter is incorrect.")) {
                LOGGER.error("Failed to shut down process caused by incorrect parameters. The process with pid: " + pid
                    + " may already be dead.");
                return;
            } else {
                if (e.getMessage().contains(ACCESS_DENIED_EXCEPTION)) {
                    throw createAccessDeniedException(pid);
                }
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_KILL_PROCESS, " with pid: " + pid);
            }
        }
    }

    private long fetchTotalSystemRAM() throws OperatingSystemException {
        try {
            final long ramValue = Humidor.getInstance().getSigar().getMem().getRam();
            if (ramValue == 0) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM);
            }
            return ramValue;
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM);
        }
    }

    @Override
    public List<ProcessInformation> getFullChildProcessInformation(long pid) throws OperatingSystemException {
        long[] pids = null;
        List<ProcessInformation> children = null;
        synchronized (SIGAR) {
            try {
                pids = ProcessFinder.find(SIGAR,
                    PTQLWrapper.createQuery().createQueryString(PTQLWrapper.statePPID(), PTQLWrapper.eq()) + pid);
            } catch (SigarException e) {
                if (e.getMessage().equals(ACCESS_DENIED_EXCEPTION)) {
                    throw createAccessDeniedException(pid);
                }
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CHILD_PROCESS_LIST,
                    " of parent process with pid: " + pid);
            }
        }
        if (pids != null && pids.length > 0) {
            children = new ArrayList<>();
            for (Long child : pids) {
                ProcessInformation p;
                try {
                    p =
                        new ProcessInformation(child, fetchProcessState(child).getName(), getFullChildProcessInformation(child),
                            getProcessCPUUsage(child),
                            getProcessRAMUsage(child));
                } catch (OperatingSystemException e) {
                    if (e.getErrorType().equals(OperatingSystemException.ErrorType.NO_SUCH_PROCESS)) {
                        LOGGER.info("Couldn't find process with pid: " + child + ". Process might already be dead.");
                        continue;
                    }
                    throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CHILD_PROCESS_LIST,
                        " of parent process with pid: " + pid);
                }
                children.add(p);
            }
        }
        if (children != null) {
            return children;
        } else {
            return Collections.emptyList();
        }
    }

    private FileSystemUsage getFileSystemUsage() throws OperatingSystemException {
        FileSystem[] fileSystemList;
        try {
            fileSystemList = Humidor.getInstance().getSigar().getFileSystemList();
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_FILE_SYSTEM);
        }
        FileSystemUsage usage = null;

        for (FileSystem fs : fileSystemList) {
            if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
                try {
                    usage = Humidor.getInstance().getSigar().getFileSystemUsage(fs.getDirName());
                } catch (SigarException e) {
                    throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_FILE_SYSTEM_USAGE);
                }
            }
        }
        return usage;
    }

    @Override
    public ProcState fetchProcessState(long pid) throws OperatingSystemException {
        try {
            return Humidor.getInstance().getSigar().getProcState(pid);
        } catch (SigarException e) {
            throw new OperatingSystemException(
                OperatingSystemException.ErrorType.FAILED_TO_GET_PROCESS_STATE + " for process with pid: " + pid);
        }
    }

    private OperatingSystemException createAccessDeniedException(long pid) {
        return new OperatingSystemException(OperatingSystemException.ErrorType.ACCESS_DENIED, " to process with pid: " + pid
            + ". You may not have the appropriate permissions.");
    }

    private OperatingSystemException createNoSuchProcessException(long pid) {
        return new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS, ". The process with pid: " + pid
            + " may not exist.");
    }

}
