/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.statistics.setup;

import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.utils.CompactStacktraceBuilder;

/**
 * Configuration settings for the "statistics" module.
 * 
 * @author Robert Mischke
 */
public class StatisticsModuleConfiguration {

    private StatisticsFilterLevel statisticsFilterLevel = StatisticsFilterLevel.RELEASE; // default

    private CompactStacktraceBuilder configuredCompactStacktraceBuilder; // may be null for default

    public StatisticsFilterLevel getStatisticsFilterLevel() {
        return statisticsFilterLevel;
    }

    /**
     * @param filterLevel the new global {@link StatisticsFilterLevel} to set
     * @return this instance (for call chaining)
     */
    public StatisticsModuleConfiguration setStatisticsFilterLevel(StatisticsFilterLevel filterLevel) {
        this.statisticsFilterLevel = filterLevel;
        return this;
    }

    /**
     * Configures the {@link CompactStacktraceBuilder} used by {@link CounterCategory#countStacktrace()}.
     * 
     * @param matchRegexp a regular expression that each stack trace element's class must match to be included; all other elements are
     *        omitted, while still adding the separators that would glue them together, which shows how many stacktrace steps were skipped
     * @param separator a string used to "glue" the stack trace element's class names together
     * @param includeMethodNames whether to include method names in the output, or just the line numbers
     * @return this instance (for call chaining)
     */
    public StatisticsModuleConfiguration configureDefaultCompactStacktraces(String matchRegexp,
        String separator, boolean includeMethodNames) {
        configuredCompactStacktraceBuilder = new CompactStacktraceBuilder(matchRegexp, separator, includeMethodNames);
        return this;
    }

    /**
     * @return the configured {@link CompactStacktraceBuilder} if present, otherwise a {@link CompactStacktraceBuilder} with a pattern
     *         matching all classes.
     */
    public CompactStacktraceBuilder getDefaultCompactStacktraceBuilder() {
        if (configuredCompactStacktraceBuilder == null) {
            // if nothing is configured, fall back to logging everything
            configuredCompactStacktraceBuilder = new CompactStacktraceBuilder(".*");
        }
        return configuredCompactStacktraceBuilder;
    }

}
