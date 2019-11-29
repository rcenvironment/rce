/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api;

import java.util.concurrent.Callable;

/**
 * A factory for instances of concurrency-related utility classes. Typically, these instances contain a reference to central services like
 * {@link AsyncTaskService}, which makes it cleaner to construct them through a factory than manually passing the service instance into each
 * constructor.
 * 
 * @author Robert Mischke
 * 
 */
public interface ConcurrencyUtilsFactory {

    /**
     * Creates a new {@link AsyncOrderedExecutionQueue}.
     * 
     * @param exceptionPolicy the {@link AsyncCallbackExceptionPolicy} defining the behavior when the callback handler throws an uncaught
     *        exception
     * @return the new instance
     */
    AsyncOrderedExecutionQueue createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy exceptionPolicy);

    /**
     * Creates a new {@link AsyncOrderedCallbackManager}.
     * 
     * @param exceptionPolicy the {@link AsyncCallbackExceptionPolicy} defining the behavior when a callback handler throws an uncaught
     *        exception
     * 
     * @param <T> the callback/listener type to manage
     * @return the new instance
     */
    <T> AsyncOrderedCallbackManager<T> createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy exceptionPolicy);

    /**
     * Creates a new {@link BatchAggregator}.
     * 
     * @param maxBatchSize the upper size limit of the batch; for a limit of n, adding the n-th element to an empty queue triggers the batch
     *        to be sent out (unless the latency limit is reached first)
     * @param maxLatency the maximum latency after the first element of a new batch is added until the batch is sent out (unless the batch
     *        size limit is reached first); this defines the maximum delay that the batch processor applies to a single element's throughput
     * @param processor the receiver of the batched elements
     * @param <T> the type of elements
     * @return the new instance
     */
    <T> BatchAggregator<T> createBatchAggregator(int maxBatchSize, long maxLatency, BatchProcessor<T> processor);

    /**
     * Creates a new {@link RunnablesGroup}.
     * 
     * @return the new instance
     */
    RunnablesGroup createRunnablesGroup();

    /**
     * Creates a {@link CallablesGroup}.
     * 
     * @param clazz the return type of the {@link Callable}s to execute
     * @param <T> the return type of the {@link Callable}s to execute
     * @return the new instance
     */
    <T> CallablesGroup<T> createCallablesGroup(Class<T> clazz);

    /**
     * Creates a {@link BlockingResponseMapper}.
     * 
     * @param <TKey> the association key type
     * @param <TResponse> the response type
     * @return the new instance
     */
    <TKey, TResponse> BlockingResponseMapper<TKey, TResponse> createBlockingResponseMapper();
}
