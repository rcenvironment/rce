/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * A simple module-internal holder for the services provided by the module, used to reduce parameter passing to utility objects created on
 * the fly.
 * 
 * @author Robert Mischke
 */
final class ConcurrencyUtilsServiceHolder {

    private final AsyncTaskService asyncTaskService;

    private final ConcurrencyUtilsFactory concurrencyUtilsFactory;

    private final StatisticsTrackerService statisticsTrackerService;

    ConcurrencyUtilsServiceHolder(AsyncTaskService asyncTaskService, StatisticsTrackerService statisticsTrackerService,
        ConcurrencyUtilsFactory concurrencyUtilsFactory) {
        this.asyncTaskService = asyncTaskService;
        this.concurrencyUtilsFactory = concurrencyUtilsFactory;
        this.statisticsTrackerService = statisticsTrackerService;
    }

    public AsyncTaskService getAsyncTaskService() {
        return asyncTaskService;
    }

    public ConcurrencyUtilsFactory getConcurrencyUtilsFactory() {
        return concurrencyUtilsFactory;
    }

    public StatisticsTrackerService getStatisticsTrackerService() {
        return statisticsTrackerService;
    }

}
