/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.setup;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;

/**
 * Configuration parameters for a {@link ConcurrencyModule}.
 * 
 * @author Robert Mischke
 */
public class ConcurrencyModuleConfiguration {

    private int threadPoolSize = 0;

    private String threadPoolName = null;

    private int periodicTaskLoggingIntervalMsec = 0;

    /**
     * @param value the maximum number of threads in the thread pool; a value of 0 (the default) selects the implementation's default value
     * @return this configuration instance (for call chaining)
     */
    public ConcurrencyModuleConfiguration setThreadPoolSize(int value) {
        this.threadPoolSize = value;
        return this;
    }

    /**
     * @param value the base name of the pooled threads; a value of null (the default) selects the implementation's default value
     * @return this configuration instance (for call chaining)
     */
    public ConcurrencyModuleConfiguration setThreadPoolName(String value) {
        this.threadPoolName = value;
        return this;
    }

    /**
     * @param value the interval, in msec, between logging the current {@link AsyncTaskService} state on the DEBUG level; a value of 0 (the
     *        default) disables it
     * @return this configuration instance (for call chaining)
     */
    public ConcurrencyModuleConfiguration setPeriodicTaskLoggingIntervalMsec(int value) {
        this.periodicTaskLoggingIntervalMsec = value;
        return this;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getPeriodicTaskLoggingIntervalMsec() {
        return periodicTaskLoggingIntervalMsec;
    }
}
