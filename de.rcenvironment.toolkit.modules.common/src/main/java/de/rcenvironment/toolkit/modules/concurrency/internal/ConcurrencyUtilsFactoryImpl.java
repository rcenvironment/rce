/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchProcessor;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsTrackerService;

/**
 * {@link ConcurrencyUtilsFactory} implementation.
 * 
 * @author Robert Mischke
 */
public final class ConcurrencyUtilsFactoryImpl implements ConcurrencyUtilsFactory {

    private final AsyncTaskService asyncTaskService;

    private final ConcurrencyUtilsServiceHolder internalServiceHolder;

    private final CounterCategory counterCategory;

    public ConcurrencyUtilsFactoryImpl(AsyncTaskService asyncTaskService, StatisticsTrackerService statisticsTrackerService) {
        this.asyncTaskService = asyncTaskService;
        this.internalServiceHolder = new ConcurrencyUtilsServiceHolder(asyncTaskService, statisticsTrackerService, this);
        // note: the counterCategory.countStacktrace() calls below have virtually zero overhead if it is disabled by the global filter
        // level; as stacktrace generation only happens after this check, no isEnabled() check is necessary around it
        this.counterCategory =
            statisticsTrackerService.getCounterCategory("ConcurrencyUtilsFactory requests", StatisticsFilterLevel.DEBUG);
    }

    @Override
    public AsyncOrderedExecutionQueue createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy exceptionPolicy) {
        counterCategory.countStacktrace();
        return new AsyncOrderedExecutionQueueImpl(exceptionPolicy, internalServiceHolder);
    }

    @Override
    public <T> AsyncOrderedCallbackManager<T> createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy exceptionPolicy) {
        counterCategory.countStacktrace();
        return new AsyncOrderedCallbackManagerImpl<T>(exceptionPolicy, internalServiceHolder);
    }

    @Override
    public <T> BatchAggregator<T> createBatchAggregator(int maxBatchSize, long maxLatency, BatchProcessor<T> processor) {
        counterCategory.countStacktrace();
        return new BatchAggregatorImpl<T>(maxBatchSize, maxLatency, processor, internalServiceHolder);
    }

    @Override
    public <T> CallablesGroup<T> createCallablesGroup(Class<T> clazz) {
        counterCategory.countStacktrace();
        return new CallablesGroupImpl<T>(asyncTaskService);
    }

    @Override
    public RunnablesGroup createRunnablesGroup() {
        counterCategory.countStacktrace();
        return new RunnablesGroupImpl(asyncTaskService);
    }
}
