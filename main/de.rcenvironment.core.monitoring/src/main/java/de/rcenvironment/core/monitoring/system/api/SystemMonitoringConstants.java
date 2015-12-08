/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.monitoring.system.api;

import de.rcenvironment.core.monitoring.system.internal.SystemMonitoringServiceImpl;

/**
 * 
 * Utility class for {@link SystemMonitoringServiceImpl}.
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

    private SystemMonitoringConstants() {}

}
