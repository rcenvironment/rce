/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.toolkitbridge.internal;

import de.rcenvironment.core.utils.common.VersionUtils;
import de.rcenvironment.toolkit.core.api.Toolkit;
import de.rcenvironment.toolkit.core.api.ToolkitException;
import de.rcenvironment.toolkit.core.setup.ToolkitSetup;
import de.rcenvironment.toolkit.modules.concurrency.setup.ConcurrencyModule;
import de.rcenvironment.toolkit.modules.concurrency.setup.ConcurrencyModuleConfiguration;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.setup.StatisticsModule;

/**
 * Defines the RCE-specific {@link Toolkit} setup, ie the selection of toolkit modules and their configuration.
 * 
 * @author Robert Mischke
 */
public final class LiveToolkitConfiguration extends DefaultToolkitConfiguration {

    // system property to override the default thread pool size
    private static final String SYSTEM_PROPERTY_COMMMON_THREAD_POOL_SIZE = "rce.threadpool.common.size";

    // system property to schedule a task that periodically logs thread pool information (similar to the "tasks -a" command);
    // this is useful in cases where the main pool is too congested to execute new console commands
    private static final String SYSTEM_PROPERTY_ENABLE_PERIODIC_DEBUG_TASK_LOGGING = "rce.threadpool.enableDebugLogging";

    // TODO >=8.0.0: make this configurable via property, too?
    private static final int PERIODIC_DEBUG_TASK_LOGGING_INTERVAL_MSEC = 60 * 1000;

    @Override
    public void configure(ToolkitSetup setup) throws ToolkitException {
        super.configure(setup); // inherit default modules

        // set concurrency parameters
        applyConcurrencySettings(setup.configureModule(ConcurrencyModule.class));

        // // set statistics tracking level
        final StatisticsFilterLevel statisticsLevel;
        if (VersionUtils.isReleaseOrReleaseCandidateBuild()) {
            statisticsLevel = StatisticsFilterLevel.RELEASE;
        } else {
            statisticsLevel = StatisticsFilterLevel.DEVELOPMENT; // edit here to temporarily set DEBUG level
        }
        setup.configureModule(StatisticsModule.class).setStatisticsFilterLevel(statisticsLevel);
    }

    private void applyConcurrencySettings(ConcurrencyModuleConfiguration configuration) {

        if (System.getProperty(SYSTEM_PROPERTY_COMMMON_THREAD_POOL_SIZE) != null) {
            int commonPoolSize = Integer.parseInt(System.getProperty(SYSTEM_PROPERTY_COMMMON_THREAD_POOL_SIZE));
            if (commonPoolSize < 1) {
                throw new IllegalArgumentException("Invalid thread pool size value: " + commonPoolSize);
            }
            configuration.setThreadPoolSize(commonPoolSize);
        }

        configuration.setThreadPoolName("MainThreadPool");

        if (System.getProperty(SYSTEM_PROPERTY_ENABLE_PERIODIC_DEBUG_TASK_LOGGING) != null) {
            configuration.setPeriodicTaskLoggingIntervalMsec(PERIODIC_DEBUG_TASK_LOGGING_INTERVAL_MSEC);
        }

    }
}
