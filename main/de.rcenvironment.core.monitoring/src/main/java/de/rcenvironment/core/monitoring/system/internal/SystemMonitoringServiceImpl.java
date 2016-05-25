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
import java.util.Collection;
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
import org.hyperic.sigar.ptql.ProcessFinder;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.NodeIdentifier;
import de.rcenvironment.core.monitoring.common.spi.PeriodicMonitoringDataContributor;
import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.ProcessInformation;
import de.rcenvironment.core.monitoring.system.api.RemotableSystemMonitoringService;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringConstants;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.core.utils.common.service.AdditionalServiceDeclaration;
import de.rcenvironment.core.utils.common.service.AdditionalServicesProvider;

/**
 * 
 * Implementation of the {@link SystemMonitoringService}.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public class SystemMonitoringServiceImpl implements SystemMonitoringService, RemotableSystemMonitoringService, AdditionalServicesProvider {

    private static final int CACHING_TIME = 2000;

    private static final String DOT = ".";

    private static final Log LOGGER = LogFactory.getLog(SystemMonitoringServiceImpl.class);

    private static final Sigar SIGAR = new Sigar();

    private static final int CONVERSION_FACTOR = 1024;

    private static final int KILL_SIGNUM = 15;

    private static final int FORCE_KILL_SIGNUM = -9;

    private static final double CONVERT_PERCENTAGE_FACTOR = 100;

    private static final String NO_SUCH_PROCESS_MESSAGE = "No such process";

    private static final String ACCESS_DENIED_EXCEPTION = "Access is denied.";

    private static CommunicationService communicationService;

    private long rcePid = 0;

    private long currentPid = 0;

    private ProcState rceState = null;

    private ProcState javaw = null;

    private long timestamp = 0;

    private SystemMonitoringDataSnapshot cachedModel;

    private long totalSystemRam;

    private Map<String, String> topicIdToDescriptionMap = new HashMap<>();

    protected void activate(BundleContext bundleContext) {
        try {
            assertPidsGathered();
        } catch (OperatingSystemException e) {
            LOGGER.error("Failed to gather process IDs of running RCE instance,"
                + " retry at the time related system monitoring data are requested: " + e.toString());
            return;
        }
        try {
            rceState = getProcState(rcePid);
            javaw = getProcState(currentPid);
            totalSystemRam = getTotalRAM();
            topicIdToDescriptionMap.put(SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_SIMPLE_SYSTEM_INFO,
                "Logs basic system monitoring data (total CPU and RAM usage)");
            // topicIdToDescriptionMap
            // .put(SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_DETAILED_SYSTEM_INFO,
            // "Logs monitoring data in more detail. Information such as CPU-usage, "
            // + "RAM-usage ect. of rce and rce sub-processes will be logged.");
        } catch (OperatingSystemException e) {
            LOGGER.error("Failed to initialize some system monitoring data: " + e.toString());
        }
    }

    protected void bindCommunicationService(CommunicationService newCommunicationService) {
        communicationService = newCommunicationService;
    }
    
    @Override
    public double getTotalCPUUsage() throws OperatingSystemException {
        double cpuUsage = 0.0;
        try {
            cpuUsage = Humidor.getInstance().getSigar().getCpuPerc().getCombined();
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_CPU_USAGE);
        }

        return SystemMonitoringUtils.clampToPercentageOrNAN(cpuUsage);
    }

    @Override
    public double getProcessCPUUsage(Long pid) throws OperatingSystemException {
        double processCPUusage = 0;
        int watch = 0;
        try {
            if (pid == null) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS);
            }

            do {
                synchronized (SIGAR) {
                    processCPUusage = SIGAR.getProcCpu(pid).getPercent();
                }

                watch++;
                if (watch == 10) {
                    return processCPUusage;
                }
            } while (processCPUusage == 0.0f);

        } catch (SigarException e) {
            if (e.getMessage().contains(NO_SUCH_PROCESS_MESSAGE)) {
                throw createNoSuchProcessException(pid);
            } else if (e.getMessage().contains(ACCESS_DENIED_EXCEPTION)) {
                throw createAccessDeniedException(pid);
            } else {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_CPU_PROCESS_USAGE,
                    " of process with pid: "
                        + pid + DOT);
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
    public double getIdle() throws OperatingSystemException {
        double idle = 0.0;
        try {
            synchronized (SIGAR) {
                idle = SIGAR.getCpuPerc().getIdle();
            }
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_IDLE);
        }

        return SystemMonitoringUtils.clampToPercentageOrNAN(idle);
    }

    @Override
    public long getTotalRAM() throws OperatingSystemException {
        long totalRam = 0;
        try {
            totalRam = Humidor.getInstance().getSigar().getMem().getRam();
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM);
        }

        return totalRam;
    }

    @Override
    public long getProcessRAMUsage(Long pid) throws OperatingSystemException {
        long processRam = 0;
        try {
            if (pid == null) {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.NO_SUCH_PROCESS);
            }
            processRam = Humidor.getInstance().getSigar().getProcMem(pid).getResident();
            processRam = processRam / (CONVERSION_FACTOR * CONVERSION_FACTOR);
        } catch (SigarException e) {
            if (e.getMessage().contains(NO_SUCH_PROCESS_MESSAGE)) {
                throw createNoSuchProcessException(pid);
            } else if (e.getMessage().contains(ACCESS_DENIED_EXCEPTION)) {
                throw createAccessDeniedException(pid);
            } else {
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_RAM_PROCESS_USAGE,
                    " of process with pid: " + pid + DOT);
            }
        }

        return processRam;
    }

    @Override
    public double getTotalUsedRAMPercentage() throws OperatingSystemException {
        double usedTotalRamPercentage = 0;
        try {
            usedTotalRamPercentage = Humidor.getInstance().getSigar().getMem().getUsedPercent() / CONVERT_PERCENTAGE_FACTOR;
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GATHER_TOTAL_RAM_PERCENTAGE);
        }

        return usedTotalRamPercentage;
    }

    @Override
    public double getProcessRAMPercentage(Long pid) throws OperatingSystemException {
        long ram = 0;
        long totalRam = 0;
        float percentage = 0;
        totalRam = getTotalRAM();
        ram = getProcessRAMUsage(pid);
        if (totalRam != 0) {
            percentage = ram / totalRam;
        }
        return percentage;
    }

    @Override
    public long getLocalDiskUsage() throws OperatingSystemException {
        return getFileSystemUsage().getUsed() / CONVERSION_FACTOR;
    }

    @Override
    public long getFreeLocalDiskSpace() throws OperatingSystemException {
        return getFileSystemUsage().getFree() / CONVERSION_FACTOR;
    }

    @Override
    public Map<Long, String> getProcesses() throws OperatingSystemException {
        Map<Long, String> processMap = new HashMap<>();
        try {
            List<Long> processList = Arrays.<Long> asList(ArrayUtils.toObject(Humidor.getInstance().getSigar().getProcList()));
            
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
        } catch (SigarException e) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_PROCESS_LIST);
        }
        
        

        return processMap;
    }

    @Override
    public Map<Long, String> getChildProcesses(Long pid) throws OperatingSystemException {

        Map<Long, String> childMap = new HashMap<>();
        List<ProcessInformation> childList = null;
        try {
            if (pid == null) {
                return Collections.emptyMap();
            }
            childList = getChildren(pid);
        } catch (SigarException e) {
            if (e.getMessage().equals(ACCESS_DENIED_EXCEPTION)) {
                throw createAccessDeniedException(pid);
            }
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CHILD_PROCESS_LIST,
                " of parent process with pid: " + pid + DOT);
        }
        if (!childList.isEmpty()) {
            for (ProcessInformation process : childList) {
                childMap.put(process.getPid(), process.getName());
            }
        }

        return childMap;
    }

    @Override
    @AllowRemoteAccess
    public synchronized SystemMonitoringDataSnapshot getCompleteSnapshot() throws OperatingSystemException {

        assertPidsGathered();
        
        if (!(timestamp < (System.currentTimeMillis() - CACHING_TIME)) && cachedModel != null) {
            return cachedModel;
        }

        final double systemCPUUsage = getTotalCPUUsage(); // valid percentage or Double.NaN
        final double cpuIdle;
        if (systemCPUUsage == Double.NaN) {
            cpuIdle = Double.NaN;
        } else {
            // do not query again, but derive from total CPU for consistency
            cpuIdle = SystemMonitoringUtils.ONE_HUNDRED_PERCENT_CPU_VALUE - systemCPUUsage;
        }

        long systemRAMUsage = (long) (totalSystemRam * getTotalUsedRAMPercentage());

        List<ProcessInformation> subProcesses;

        try {
            subProcesses = getChildren(currentPid);
        } catch (SigarException e1) {
            throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CHILD_PROCESS_LIST);
        }
        List<ProcessInformation> ownProcesses = new ArrayList<>();
        if (rceState != null) {
            ownProcesses.add(new ProcessInformation(rcePid, rceState.getName(), Collections.<ProcessInformation> emptyList(),
                getProcessCPUUsage(rcePid),
                getProcessRAMUsage(rcePid)));
        }

        if (javaw != null) {
            ownProcesses.add(new ProcessInformation(currentPid, javaw.getName(), Collections.<ProcessInformation> emptyList(),
                getProcessCPUUsage(currentPid),
                getProcessRAMUsage(currentPid)));
        }

        SystemMonitoringDataSnapshot model =
            new SystemMonitoringDataSnapshot(systemCPUUsage, systemRAMUsage, totalSystemRam, cpuIdle, subProcesses, ownProcesses);
        cachedModel = model;
        timestamp = System.currentTimeMillis();

        return model;
    }

    @Override
    public void kill(Long pid, Boolean force) throws OperatingSystemException {
        try {
            LOGGER.info("Killing process with pid: " + pid);
            if (force) {
                synchronized (SIGAR) {
                    SIGAR.kill(pid, FORCE_KILL_SIGNUM);
                }
            } else {
                synchronized (SIGAR) {
                    SIGAR.kill(pid, KILL_SIGNUM);
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
                throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_KILL_PROCESS, " with pid: " + pid + DOT);
            }
        }
    }

    @Override
    public SystemMonitoringDataSnapshot getMonitoringModel(NodeIdentifier node) throws OperatingSystemException, RemoteOperationException {
        return communicationService.getRemotableService(RemotableSystemMonitoringService.class, node).getCompleteSnapshot();
    }

    /**
     * Clears cached model (intended for tests).
     */
    protected void clearCache() {
        cachedModel = null;
    }

    private List<ProcessInformation> getChildren(long ppid) throws SigarException, OperatingSystemException {
        long[] pids = null;
        List<ProcessInformation> children = null;
        synchronized (SIGAR) {
            try {
                pids = ProcessFinder.find(SIGAR,
                    PTQLWrapper.createQuery().createQueryString(PTQLWrapper.statePPID(), PTQLWrapper.eq()) + ppid);
            } catch (SigarException e) {
                throw e;
            }
        }
        if (pids != null && pids.length > 0) {
            children = new ArrayList<>();
            for (Long child : pids) {
                ProcessInformation p;
                try {
                    p =
                        new ProcessInformation(child, getProcState(child).getName(), getChildren(child),
                            getProcessCPUUsage(child),
                            getProcessRAMUsage(child));
                } catch (OperatingSystemException e) {
                    if (e.getErrorType().equals(OperatingSystemException.ErrorType.NO_SUCH_PROCESS)) {
                        LOGGER.info("Couldn't find process with pid: " + child + ". Process might already be dead.");
                        continue;
                    }
                    throw new OperatingSystemException(OperatingSystemException.ErrorType.FAILED_TO_GET_CHILD_PROCESS_LIST,
                        " of parent process with pid: " + ppid);
                }
                children.add(p);
            }
        }
        if (children == null) {
            return Collections.emptyList();
        }

        return children;
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

    private ProcState getProcState(long pid) throws OperatingSystemException {
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

    private String generateSimpleMonitoringData() {
        String result = "";
        try {
            double nodeCpuUsage;
            long ram;
            long systemRamUsage;
            if (!(timestamp < (System.currentTimeMillis() - CACHING_TIME)) && cachedModel != null) {
                nodeCpuUsage = cachedModel.getNodeCPUusage();
                ram = cachedModel.getNodeSystemRAM();
                systemRamUsage = cachedModel.getNodeRAMUsage();
            } else {
                if (totalSystemRam != 0) {
                    ram = totalSystemRam;
                } else {
                    ram = getTotalRAM();
                }
                nodeCpuUsage = getTotalCPUUsage();
                systemRamUsage = (long) (totalSystemRam * getTotalUsedRAMPercentage());
            }
            result = StringUtils.format("System CPU usage: %.2f%%, System RAM usage: %d / %d MiB", nodeCpuUsage
                * SystemMonitoringService.PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER, systemRamUsage, ram);
        } catch (OperatingSystemException e) {
            result = "Error gathering system data: " + e.getMessage();
        }

        return result;
    }

    private String logDetailedMonitoringData() {
        String result = "";
        if (!(timestamp < (System.currentTimeMillis() - CACHING_TIME)) && cachedModel != null) {
            result = cachedModel.toString();
        } else {
            try {
                result = getCompleteSnapshot().toString();
            } catch (OperatingSystemException e) {
                LOGGER.error(e);
            }
        }

        return result;
    }

    @Override
    public Collection<AdditionalServiceDeclaration> defineAdditionalServices() {
        List<AdditionalServiceDeclaration> result = new ArrayList<AdditionalServiceDeclaration>();
        result.add(new AdditionalServiceDeclaration(PeriodicMonitoringDataContributor.class, new PeriodicMonitoringDataContributor() {

            @Override
            public Collection<String> getTopicIds() {
                return topicIdToDescriptionMap.keySet();
            }

            @Override
            public String getTopicDescription(String topicId) {
                return topicIdToDescriptionMap.get(topicId);
            }

            @Override
            public void generateOutput(String topicId, List<String> collection) {
                switch (topicId) {
                case SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_SIMPLE_SYSTEM_INFO:
                    collection.add(generateSimpleMonitoringData());
                    break;
                case SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_DETAILED_SYSTEM_INFO:
                    collection.add(logDetailedMonitoringData());
                    break;
                default:
                    throw new IllegalArgumentException("There is no topic id such as: " + topicId);
                }
            }
        }));

        return result;
    }
    
    private void assertPidsGathered() throws OperatingSystemException {
        if (rcePid == 0) {
            rcePid = getProcState(Humidor.getInstance().getSigar().getPid()).getPpid();
        }
        if (currentPid == 0) {
            currentPid = Humidor.getInstance().getSigar().getPid();
        }
    }

}
