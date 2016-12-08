/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchAggregator;
import de.rcenvironment.toolkit.modules.concurrency.api.BatchProcessor;
import de.rcenvironment.toolkit.modules.concurrency.api.ConcurrencyUtilsFactory;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * A generic class to group sequentially-generated elements into ordered batches. A batch is returned (to a given {@link BatchProcessor})
 * when a maximum number of elements is reached, or after a specified time has elapsed since the batch was created. Batches are created
 * implicitly when an element is added while no batch is already active.
 * 
 * @param <T> the element type to aggregate
 * 
 * @author Robert Mischke
 */
public class BatchAggregatorImpl<T> implements BatchAggregator<T> {

    /**
     * A Runnable to check for maximum latency timeouts.
     * 
     * @author Robert Mischke
     */
    private final class MaxLatencyTimerCallback implements Runnable {

        private List<T> relevantBatch;

        MaxLatencyTimerCallback(List<T> relevantBatch) {
            this.relevantBatch = relevantBatch;
        }

        @Override
        @TaskDescription("BatchAggregator Max Latency Timer")
        public void run() {
            // delegate for proper synchronization
            onMaxLatencyTimerCallback(relevantBatch);
        }
    }

    /**
     * A Runnable to dispatch a completed batch to the configured {@link BatchProcessor}.
     * 
     * @param <T> the element type of the batch that should be dispatched;
     * 
     * @author Robert Mischke
     */
    // TODO this class was static before making statisticsTrackerService a field; review
    private final class DispatchRunnable<T> implements Runnable {

        private static final String ASYNC_TASK_DESCRIPTION = "BatchAggregator dispatch";

        private final List<T> detachedBatchReference;

        private final BatchProcessor<T> processor;

        private DispatchRunnable(List<T> detachedBatchReference, BatchProcessor<T> processor) {
            this.detachedBatchReference = detachedBatchReference;
            this.processor = processor;
        }

        @Override
        @TaskDescription(ASYNC_TASK_DESCRIPTION)
        public void run() {
            try {
                processor.processBatch(detachedBatchReference);
            } catch (RuntimeException e) {
                // the best we can do here is log the error and discard the batch
                // synchronized so that the proper logger is always "seen"
                synchronized (BatchAggregatorImpl.class) {
                    log.error("Uncaught exception in batch processor " + processor, e);
                }
                throw e; // allow then async queue to react on the exception as well
            }
            // TODO improve fetching
            internalServicesHolder.getStatisticsTrackerService()
                .getCounterCategory(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME).count(
                    ASYNC_TASK_DESCRIPTION);
        }
    }

    // not made final as it needs to be replaced from a unit test
    private static Log log = LogFactory.getLog(BatchAggregatorImpl.class);

    private List<T> currentBatch;

    // private Deque<List<T>> outputQueue = new LinkedList<List<T>>();

    private final int maxBatchSize;

    private final long maxLatency;

    private final BatchProcessor<T> processor;

    private final AsyncOrderedExecutionQueue dispatchQueue;

    private final ConcurrencyUtilsServiceHolder internalServicesHolder;

    private final ConcurrencyUtilsFactory concurrencyUtilsFactory;

    public BatchAggregatorImpl(int maxBatchSize, long maxLatency, BatchProcessor<T> processor,
        ConcurrencyUtilsServiceHolder internalServicesHolder) {
        this.maxBatchSize = maxBatchSize;
        this.maxLatency = maxLatency;
        this.processor = processor;
        this.internalServicesHolder = internalServicesHolder;
        this.concurrencyUtilsFactory = internalServicesHolder.getConcurrencyUtilsFactory();
        // TODO @5.0: policy chosen for backwards compatibility; review/make configurable?
        this.dispatchQueue =
            concurrencyUtilsFactory.createAsyncOrderedExecutionQueue(
                AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);
    }

    /**
     * Adds an element for aggregation. May trigger the internal creation of a new batch or the sending of a finished batch when the the
     * maximum size limit is reached.
     * 
     * @param element the element to add
     */
    @Override
    public synchronized void enqueue(T element) {
        if (currentBatch == null) {
            startNewBatch();
        }

        currentBatch.add(element);

        int size = currentBatch.size();
        if (size >= maxBatchSize) {
            // sanity check
            if (size > maxBatchSize) {
                throw new IllegalArgumentException("maxBatchSize exceeded?");
            }
            // send current batch; the next incoming element will start a new one
            endCurrentBatchAndEnqueueForProcessing();
        }
    }

    /**
     * Logger access for unit tests.
     */
    protected static synchronized void setLogger(Log newLog) {
        BatchAggregatorImpl.log = newLog;
    }

    /**
     * Logger access for unit tests.
     */
    protected static synchronized Log getLogger() {
        return log;
    }

    private synchronized void onMaxLatencyTimerCallback(List<T> relevantBatch) {
        if (currentBatch != relevantBatch) {
            // in this case, the batch associated with the calling TimerTask
            // was already sent out because max size was reached
            return;
        }
        endCurrentBatchAndEnqueueForProcessing();
    }

    private void startNewBatch() {
        currentBatch = new ArrayList<T>();
        internalServicesHolder.getAsyncTaskService().scheduleAfterDelay(new MaxLatencyTimerCallback(currentBatch), maxLatency);
    }

    private void endCurrentBatchAndEnqueueForProcessing() {
        // note: it is important that this Runnable copies the current reference, and does not accidentally use the field! - misc_ro
        dispatchQueue.enqueue(new DispatchRunnable<T>(currentBatch, processor));
        currentBatch = null;
    }

}
