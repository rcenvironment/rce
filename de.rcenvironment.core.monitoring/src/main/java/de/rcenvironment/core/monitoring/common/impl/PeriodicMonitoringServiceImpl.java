/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.common.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.CommandLineArguments;
import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.monitoring.common.spi.PeriodicMonitoringDataContributor;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsConsumer;
import de.rcenvironment.toolkit.modules.objectbindings.api.ObjectBindingsService;

/**
 * Implementation of a background task that collects all existing {@link PeriodicMonitoringDataContributor}s, and periodically calls those
 * that are enabled by configuration.
 * 
 * Note that this class is registered as an OSGi-DS component, but provices no service interface at the moment.
 * 
 * @author Robert Mischke
 */
public class PeriodicMonitoringServiceImpl {

    private static final int DEFAULT_MONITORING_INTERVAL_SECS = 15;

    private ConfigurationService configurationService;

    private final Map<String, PeriodicMonitoringDataContributor> contributors = new HashMap<>();

    private final Set<String> activeTopics = new HashSet<>();

    private final Log log = LogFactory.getLog(getClass());

    private ScheduledFuture<?> taskFuture;

    private int intervalSec = DEFAULT_MONITORING_INTERVAL_SECS;

    private ObjectBindingsService objectBindingsService;

    /**
     * The periodic background task performing the actual data collection and dispatch (for example, writing the generated lines to a log
     * file).
     * 
     * @author Robert Mischke
     */
    private final class BackgroundTask implements Runnable {

        @Override
        public void run() {
            final List<String> output;
            synchronized (activeTopics) {
                if (activeTopics.isEmpty()) {
                    return;
                }
                output = new ArrayList<>();
                synchronized (contributors) {
                    // note: using iterator to allow removal of invalid ids
                    for (Iterator<String> iter = activeTopics.iterator(); iter.hasNext();) {
                        final String id = iter.next();
                        PeriodicMonitoringDataContributor contributor = contributors.get(id);
                        if (contributor != null) {
                            contributor.generateOutput(id, output);
                        } else {
                            log.warn("No monitoring contributor found for configured monitoring id '" + id + "'; deactivating id");
                            iter.remove();
                        }
                    }
                }
            }
            for (String line : output) {
                log.info(line);
            }
        }
    }

    /**
     * OSGi-DS life cycle method.
     */
    public void activate() {
        if (!CommandLineArguments.isNormalOperationRequested()) {
            log.debug("Not running in standard mode - not starting background monitoring");
            return;
        }
        synchronized (activeTopics) {
            loadConfigurationData();
            logActiveTopics();
        }

        // always start the task, as topics may be enabled at a later time
        // TODO leave at this initial delay or start immediately?
        taskFuture = ConcurrencyUtils.getAsyncTaskService()
            .scheduleAtFixedInterval("Periodic background monitoring task", new BackgroundTask(), TimeUnit.SECONDS.toMillis(intervalSec));

        objectBindingsService.setConsumer(PeriodicMonitoringDataContributor.class,
            new ObjectBindingsConsumer<PeriodicMonitoringDataContributor>() {

                @Override
                public void addInstance(PeriodicMonitoringDataContributor instance) {
                    addContributor(instance);
                }

                @Override
                public void removeInstance(PeriodicMonitoringDataContributor instance) {
                    removeContributor(instance);
                }
            });
    }

    /**
     * OSGi-DS life cycle method.
     */
    public void deactivate() {
        if (taskFuture != null) {
            taskFuture.cancel(false);
            taskFuture = null;
        }
    }

    private void addContributor(PeriodicMonitoringDataContributor contributor) {
        synchronized (contributors) {
            for (String id : contributor.getTopicIds()) {
                // log.debug("Registering monitoring id " + id);
                contributors.put(id, contributor);
                logAvailableMonitoringId(id, contributor);
            }
        }
    }

    private void removeContributor(PeriodicMonitoringDataContributor contributor) {
        synchronized (contributors) {
            for (String id : contributor.getTopicIds()) {
                // note: this assumes unique topic id
                log.debug("Unregistering monitoring id " + id);
                contributors.remove(id);
            }
        }
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the provided service instance
     */
    public void bindConfigurationService(ConfigurationService newInstance) {
        configurationService = newInstance;
    }

    /**
     * OSGi-DS bind method.
     * 
     * @param newInstance the provided service instance
     */
    public void bindObjectBindingsService(ObjectBindingsService newInstance) {
        objectBindingsService = newInstance;
    }

    private void loadConfigurationData() {
        synchronized (activeTopics) {
            activeTopics.clear();
            ConfigurationSegment configurationSegment = configurationService.getConfigurationSegment("backgroundMonitoring");
            if (configurationSegment != null) {
                // TODO preliminary format
                String topicsString = configurationSegment.getString("enabledIds", "");
                if (topicsString != null) {
                    for (String rawId : topicsString.split(",")) {
                        final String id = rawId.trim();
                        if (id.isEmpty()) {
                            continue;
                        }
                        activeTopics.add(id);
                    }
                }
                intervalSec = configurationSegment.getInteger("intervalSeconds", DEFAULT_MONITORING_INTERVAL_SECS);
                if (intervalSec < 1) {
                    log.warn(StringUtils.format("Resource monitoring interval (configuration value 'intervalSeconds') is invalid: "
                        + "it is %d seconds but must be >= 1 second; default value of %d seconds is applied",
                        intervalSec, DEFAULT_MONITORING_INTERVAL_SECS));
                    intervalSec = DEFAULT_MONITORING_INTERVAL_SECS;
                }
                if (activeTopics.isEmpty()) {
                    log.debug("No monitoring topics configured");
                }
            } else {
                intervalSec = DEFAULT_MONITORING_INTERVAL_SECS;
            }
        }
    }

    private void logAvailableMonitoringId(String id, PeriodicMonitoringDataContributor contributor) {
        log.debug(StringUtils.format("Monitoring topic available: \"%s\" (\"%s\")", id,
            contributor.getTopicDescription(id)));
    }

    private void logActiveTopics() {
        log.debug("Active monitoring topics: " + Arrays.toString(activeTopics.toArray()));
    }

}
