/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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
import de.rcenvironment.core.monitoring.system.api.model.FullSystemAndProcessDataSnapshot;
import de.rcenvironment.core.monitoring.system.api.model.SystemLoadInformation;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.security.AllowRemoteAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;

/**
 * Aggregates low-level system monitoring data to higher-level data structures, with internal caching where appropriate.
 * 
 * @author David Scholz (original "snapshot" code)
 * @author Robert Mischke
 * @author Doreen Seider
 * @author Dominik Schneider
 */
public class SystemMonitoringAggregationServiceImpl implements RemotableSystemMonitoringService, LocalSystemMonitoringAggregationService {

    private static final int MAX_ITEMS = 20;

    private static final int INTERVAL = 3000;

    private SystemIntegrationAdapter adapter;

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

    private SystemMonitoringDataCollector dataCollector;

    private SystemMonitoringDataProcessor dataProcessor;

    private final Log log = LogFactory.getLog(SystemMonitoringAggregationServiceImpl.class);

    private CommunicationService communicationService;

    protected void activate(BundleContext bundleContext) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }

        Objects.requireNonNull(objectBindingsService);
        Objects.requireNonNull(asyncTaskService);

        adapter = this.getAdapter();
        Objects.requireNonNull(adapter); // split for readability

        topicIdToDescriptionMap.put(SystemMonitoringConstants.PERIODIC_MONITORING_TOPIC_SIMPLE_SYSTEM_INFO,
            "Logs basic system monitoring data (total CPU and RAM usage)");

        objectBindingsService.addBinding(PeriodicMonitoringDataContributor.class, setUpPeriodicMonitoringDataContributorAdapter(), this);

        this.dataCollector = new SystemMonitoringDataCollector(this.adapter, this.asyncTaskService);
        this.dataCollector.startCollection(MAX_ITEMS, INTERVAL);
        log.debug("System data collector initialized and collection started");

        this.dataProcessor = new SystemMonitoringDataProcessor(this.dataCollector.getRingBuffer(), this.adapter);

    }

    protected SystemIntegrationAdapter getAdapter() {
        return SystemIntegrationEntryPoint.getAdapter();
    }

    protected void deactivate(BundleContext bundleContext) {
        if (RuntimeDetection.isImplicitServiceActivationDenied()) {
            // do not activate this service if is was spawned as part of a default test environment
            return;
        }

        objectBindingsService.removeAllBindingsOfOwner(this);
    }

    protected void bindObjectBindingsService(ObjectBindingsService newInstance) {
        objectBindingsService = newInstance;
    }

    protected void bindAsyncTaskService(AsyncTaskService newInstance) {
        this.asyncTaskService = newInstance;
    }

    protected void bindCommunicationService(CommunicationService newInstance) {
        this.communicationService = newInstance;
    }

    @Override
    @AllowRemoteAccess
    public synchronized FullSystemAndProcessDataSnapshot getCompleteSnapshot() throws OperatingSystemException {
        return createFullSnapshot();
    }

    @Override
    @AllowRemoteAccess
    public SystemLoadInformation getSystemLoadInformation(Integer maxSamples) {
        // note: method is synchronized internally, no need to do it here too
        return this.dataProcessor.getSystemLoadInformation(maxSamples);
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

    private FullSystemAndProcessDataSnapshot createFullSnapshot() {
        this.dataCollector.resetPartialCollectionFallback();
        return this.dataProcessor.createFullSystemSnapshot();
    }

    private String createSimpleSystemMonitoringSummary() {
        FullSystemAndProcessDataSnapshot snapshot = createFullSnapshot();
        double nodeCpuUsage = snapshot.getNodeCPUusage();
        long ram = snapshot.getNodeRAMUsage();
        long systemRamUsage = snapshot.getNodeRAMUsage();
        return StringUtils.format("System CPU usage: %.2f%%, System RAM usage: %d / %d MiB", nodeCpuUsage
            * SystemMonitoringConstants.PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER, systemRamUsage, ram);
    }

    private String logDetailedMonitoringData() {
        return createFullSnapshot().toString();
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
