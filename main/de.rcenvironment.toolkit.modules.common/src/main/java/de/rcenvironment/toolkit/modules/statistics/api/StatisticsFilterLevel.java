/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.api;

/**
 * Filter levels (similar to log levels) that define which statistics counters or value categories should be enabled.
 * 
 * @author Robert Mischke
 */
public enum StatisticsFilterLevel {

    /**
     * The standard level for release builds.
     */
    RELEASE,

    /**
     * The standard level for snapshot builds and running from development environments (e.g. Eclipse).
     */
    DEVELOPMENT,

    /**
     * The most fine-grained statistics level. Typically only enabled when some sort of of launch option is set.
     */
    DEBUG
}
