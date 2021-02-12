/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.configuration.bootstrap.RuntimeDetection;
import de.rcenvironment.core.monitoring.common.spi.PeriodicMonitoringDataContributor;
import de.rcenvironment.core.monitoring.system.api.LocalSystemMonitoringAggregationService;
import de.rcenvironment.core.monitoring.system.api.OperatingSystemException;
import de.rcenvironment.core.monitoring.system.api.RemotableSystemMonitoringService;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringConstants;
import de.rcenvironment.core.monitoring.system.api.SystemMonitoringDataService;
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.model.ProcessInformation;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;
import de.rcenvironment.toolkit.utils.common.DefaultTimeSource;

/**
 * Aggregates low-level system monitoring data to higher-level data structures, with internal caching where appropriate.
 * 
 * @author David Scholz (original "snapshot" code)
 * @author Robert Mischke
 * @author Doreen Seider
 */
public class SystemMonitoringAggregationServiceImpl implements RemotableSystemMonitoringService, LocalSystemMonitoringAggregationService {

    private static final int COMPLETE_SNAPSHOT_CACHE_LIFETIME_MSEC = 2000;

    private static final int SYSTEM_LOAD_INFORMATION_COLLECTION_BUFFER_SIZE = 30;

    private SystemIntegrationAdapter adapter;

    /**
     * The low-level service to fetch system data from.
     */
    private SystemMonitoringDataService systemDataService;

    /**
     * Used to register periodic background tasks.
     */
    private AsyncTaskService asyncTaskService;

    /**
     * Used to register itself as a {@link PeriodicMonitoringDataContributor}.
     */
    private ObjectBindingsService objectBindingsService;

    // description of provided data sources
    private Map<String, String> topicIdToDescriptionMap = new HashMap<>();

    private FullSystemAndProcessDataSnapshot cachedFullSnapshot;

    private long cachedFullSnapshotTimestamp = 0;

    private final Log log = LogFactory.getLog(SystemMonitoringDataServiceImpl.class);

    private ScheduledFuture<?> systemLoadCollectorFuture;

    private SystemLoadInformationCollector systemLoadInformationCollector;

    // TODO remove?
    @SuppressWarnings("unused")
    private ConcurrencyUtilsFactory concurrencyUtilsFactory;

    private CommunicationService communicationService;

    protected void activate(BundleContext bundleContext) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }
        
        Objects.requireNonNull(systemDataService);
        Objects.requireNonNull(objectBindingsService);
        Objects.requireNonNull(asyncTaskService);

        adapter = SystemIntegrationEntryPoint.getAdapter();
        Objects.requireNonNull(adapter); // split for readability

        topicIdToDescriptionMap.put(SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_SIMPLE_SYSTEM_INFO,
            "Logs basic system monitoring data (total CPU and RAM usage)");

        objectBindingsService.addBinding(PeriodicMonitoringDataContributor.class, setUpPeriodicMonitoringDataContributorAdapter(), this);

        systemLoadInformationCollector =
            new SystemLoadInformationCollector(systemDataService, SYSTEM_LOAD_INFORMATION_COLLECTION_BUFFER_SIZE, new DefaultTimeSource(),
                MINIMUM_TIME_DELTA_TO_ACCEPT_BETWEEN_UPDATES, MAXIMUM_TIME_DELTA_TO_ACCEPT_BEFORE_STARTING_OVER);
        // this is one of the few places where "scheduleAtFixedRate()" is actually appropriate -- misc_ro
        systemLoadCollectorFuture =
            asyncTaskService.scheduleAtFixedRate("System Monitoring: Collect system load information", systemLoadInformationCollector,
                SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC);
        log.debug("System load collector initialized");
    }

    protected void deactivate(BundleContext bundleContext) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }
        
        systemLoadInformationCollector = null;
        systemLoadCollectorFuture.cancel(false); // short-running; no need to interrupt
        systemLoadCollectorFuture = null;

        objectBindingsService.removeAllBindingsOfOwner(this);
    }

    protected void bindObjectBindingsService(ObjectBindingsService newInstance) {
        objectBindingsService = newInstance;
    }

    protected void bindSystemMonitoringDataService(SystemMonitoringDataService newInstance) {
        this.systemDataService = newInstance;
    }

    protected void bindAsyncTaskService(AsyncTaskService newInstance) {
        this.asyncTaskService = newInstance;
    }

    protected void bindConcurrencyUtilsFactory(ConcurrencyUtilsFactory newInstance) {
        this.concurrencyUtilsFactory = newInstance;
    }

    protected void bindCommunicationService(CommunicationService newInstance) {
        this.communicationService = newInstance;
    }

    @Override
    @AllowRemoteAccess
    public synchronized FullSystemAndProcessDataSnapshot getCompleteSnapshot() throws OperatingSystemException {

        if (hasValidCachedFullSnapshot()) {
            return cachedFullSnapshot;
        }

        FullSystemAndProcessDataSnapshot newSnapshot = createFullSnapshot();

        cachedFullSnapshot = newSnapshot;
        cachedFullSnapshotTimestamp = System.currentTimeMillis();

        return newSnapshot;
    }

    @Override
    @AllowRemoteAccess
    public SystemLoadInformation getSystemLoadInformation(Integer maxSamples) {
        // note: method is synchronized internally, no need to do it here too
        return systemLoadInformationCollector.getSystemLoadInformation(maxSamples);
    }

    @Override
    public <T extends ResolvableNodeId> Map<T, SystemLoadInformation> collectSystemMonitoringDataWithTimeLimit(
        final Set<T> nodeIds, final int timeSpanMsec, final int timeLimitMsec)
        throws InterruptedException, ExecutionException, TimeoutException {

        final Map<T, SystemLoadInformation> concurrentResultMap = new ConcurrentHashMap<>();
        final int nodeCount = nodeIds.size();
        final Semaphore finishCounter = new Semaphore(0); // not using a CDL as it does not provide a "release all" method

        for (final T nodeId : nodeIds) {
            asyncTaskService.execute("Fetch system load data from a single node", () -> {

                final RemotableSystemMonitoringService remotableService =
                    communicationService.getRemotableService(RemotableSystemMonitoringService.class, nodeId);
                SystemLoadInformation systemLoadInformation;
                try {
                    // note: the division relies on the assumption that all nodes use the same polling interval
                    systemLoadInformation =
                        remotableService.getSystemLoadInformation(timeSpanMsec / SYSTEM_LOAD_INFORMATION_COLLECTION_INTERVAL_MSEC);
                    concurrentResultMap.put(nodeId, systemLoadInformation);
                } catch (RemoteOperationException e) {
                    log.warn("Error while fetching remote system load data: " + e.toString());
                }
                finishCounter.release();

            });
        }

        // trigger standard timeout
        asyncTaskService.scheduleAfterDelay("Enforce time limit while waiting for system load information responses",
            () -> finishCounter.release(nodeCount), timeLimitMsec);

        // use twice the individual limit as a hard fallback time limit (arbitrary)
        if (!finishCounter.tryAcquire(nodeCount, timeLimitMsec * 2, TimeUnit.MILLISECONDS)) {
            // this should not usually happen, but is possible under high system load
            log.warn("Fallback time limit reached while waiting for individual system load data responses");
        }

        // create and return an immutable snapshot of the map
        synchronized (concurrentResultMap) {
            return Collections.unmodifiableMap(new HashMap<>(concurrentResultMap));
        }
    }

    /**
     * Clears cached model (intended for tests).
     */
    protected void clearFullSnapshotCache() {
        cachedFullSnapshot = null;
        cachedFullSnapshotTimestamp = 0;
    }

    private FullSystemAndProcessDataSnapshot createFullSnapshot() throws OperatingSystemException {
        final double systemCPUUsage = systemDataService.getTotalCPUUsage(); // valid percentage or Double.NaN
        final double cpuIdle;
        if (Double.isNaN(systemCPUUsage)) {
            cpuIdle = Double.NaN;
        } else {
            // do not query again, but derive from total CPU for consistency
            cpuIdle = SystemMonitoringUtils.ONE_HUNDRED_PERCENT_CPU_VALUE - systemCPUUsage;
        }

        long systemRAMUsage = systemDataService.getTotalUsedRAM();

        final List<ProcessInformation> subProcesses;
        final List<ProcessInformation> ownProcesses;

        // process info may or may not be already available
        if (adapter.areSelfPidsAndProcessStatesAvailable()) {
            subProcesses = systemDataService.getFullChildProcessInformation(adapter.getSelfJavaPid());
            ownProcesses = new ArrayList<>();
            ownProcesses.add(new ProcessInformation(adapter.getSelfJavaPid(), adapter.getSelfJavaProcessName(), Collections
                .<ProcessInformation> emptyList(),
                systemDataService.getProcessCPUUsage(adapter.getSelfJavaPid()),
                systemDataService.getProcessRAMUsage(adapter.getSelfJavaPid())));
            if (OSFamily.isLinux()) {
                ownProcesses.add(new ProcessInformation(adapter.getSelfLauncherPid(), adapter.getSelfLauncherProcessName(), Collections
                    .<ProcessInformation> emptyList(),
                    systemDataService.getProcessCPUUsage(adapter.getSelfLauncherPid()),
                    systemDataService.getProcessRAMUsage(adapter.getSelfLauncherPid())));
                // move Linux "Webkit*" processes (related to the integrated help browser) to "own processes"
                for (Iterator<ProcessInformation> it = subProcesses.iterator(); it.hasNext();) {
                    ProcessInformation proc = it.next();
                    String processName = proc.getName();
                    if (processName.startsWith("WebKit")) {
                        ownProcesses.add(proc);
                        it.remove();
                    }
                }
            } else if (OSFamily.isWindows()) {
                for (Iterator<ProcessInformation> it = subProcesses.iterator(); it.hasNext();) {
                    // eliminate "conhost" on Windows systems (see #17328)
                    String processName = it.next().getName();
                    if (processName.equals("conhost")) {
                        // as this has only been observed during Maven tests, just drop it instead of muddying the "own processes" pool
                        log.debug("Eliminated 'conhost' from the list of sub-processes; "
                            + "this is normal during integration tests, please report any other sightings");
                        it.remove();
                    }
                }
            } else {
                throw new IllegalStateException();
            }

        } else {
            subProcesses = new ArrayList<>();
            ownProcesses = new ArrayList<>();
        }

        return new FullSystemAndProcessDataSnapshot(systemCPUUsage, systemRAMUsage, systemDataService.getTotalSystemRAM(),
            cpuIdle, subProcesses, ownProcesses);
    }

    private String createSimpleSystemMonitoringSummary() {
        try {
            double nodeCpuUsage;
            long ram;
            long systemRamUsage;
            if (hasValidCachedFullSnapshot()) {
                nodeCpuUsage = cachedFullSnapshot.getNodeCPUusage();
                ram = cachedFullSnapshot.getNodeSystemRAM();
                systemRamUsage = cachedFullSnapshot.getNodeRAMUsage();
            } else {
                ram = systemDataService.getTotalSystemRAM();
                nodeCpuUsage = systemDataService.getTotalCPUUsage();
                systemRamUsage = systemDataService.getTotalUsedRAM();
            }
            return StringUtils.format("System CPU usage: %.2f%%, System RAM usage: %d / %d MiB", nodeCpuUsage
                * SystemMonitoringConstants.PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER, systemRamUsage, ram);
        } catch (OperatingSystemException e) {
            return "Error gathering system data: " + e.getMessage();
        }
    }

    private boolean hasValidCachedFullSnapshot() {
        return cachedFullSnapshotTimestamp >= (System.currentTimeMillis() - COMPLETE_SNAPSHOT_CACHE_LIFETIME_MSEC)
            && cachedFullSnapshot != null;
    }

    private String logDetailedMonitoringData() {
        if (hasValidCachedFullSnapshot()) {
            return cachedFullSnapshot.toString();
        } else {
            try {
                return getCompleteSnapshot().toString();
            } catch (OperatingSystemException e) {
                log.error(e);
                return "<error>";
            }
        }
    }

    private PeriodicMonitoringDataContributor setUpPeriodicMonitoringDataContributorAdapter() {
        return new PeriodicMonitoringDataContributor() {

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
                    collection.add(createSimpleSystemMonitoringSummary());
                    break;
                case SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_DETAILED_SYSTEM_INFO:
                    collection.add(logDetailedMonitoringData());
                    break;
                default:
                    throw new IllegalArgumentException("There is no topic id such as: " + topicId);
                }
            }
        };
    }

}
