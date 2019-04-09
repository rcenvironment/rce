/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import de.rcenvironment.core.monitoring.common.spi.PeriodicMonitoringDataContributor;

/**
 * External constants for {@link SystemMonitoringDataService}, {@link RemotableSystemMonitoringService}, and their
 * {@link PeriodicMonitoringDataContributor}s.
 * 
 * @author David Scholz
 * @author Robert Mischke
 */
public final class SystemMonitoringConstants {

    /**
     * Logs simple monitoring data such as node cpu usage and node system ram.
     */
    public static final String PERIODIC_MONITORING_TOPIC_SIMPLE_SYSTEM_INFO = "basic_system_data";

    /**
     * Logs monitoring data in more detail such as resource allocation of a single process.
     */
    public static final String PERIODIC_MONITORING_TOPIC_DETAILED_SYSTEM_INFO = "detailed_system_data";

    /**
     * The multiplier to convert an internal percentage value to human-readable percentage values (0..100).
     */
    public static final double PERCENTAGE_TO_DISPLAY_VALUE_MULTIPLIER = 100.0;

    /**
     * The default value if no CPU load data has been acquired yet. (Note: probably not used in all possible places yet.)
     */
    public static final double CPU_LOAD_UNKNOWN_DEFAULT = Double.NaN;

    /**
     * The default value if no RAM data of the requested type has been acquired yet. (Note: probably not used in all possible places yet.)
     */
    public static final int RAM_UNKNOWN_DEFAULT = -1;

    private SystemMonitoringConstants() {}

}
