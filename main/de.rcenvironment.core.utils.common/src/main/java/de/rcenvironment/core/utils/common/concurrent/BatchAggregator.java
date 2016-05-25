/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StatsCounter;

/**
 * A generic class to group sequentially-generated elements into ordered batches. A batch is returned (to a given {@link BatchProcessor})
 * when a maximum number of elements is reached, or after a specified time has elapsed since the batch was created. Batches are created
 * implicitly when an element is added while no batch is already active.
 * 
 * @param <T> the element type to aggregate
 * 
 * @author Robert Mischke
 */
public class BatchAggregator<T> {

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
    private static final class DispatchRunnable<T> implements Runnable {

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
                synchronized (BatchAggregator.class) {
                    log.error("Uncaught exception in batch processor " + processor, e);
                }
                throw e; // allow then async queue to react on the exception as well
            }
            StatsCounter.count(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME, ASYNC_TASK_DESCRIPTION);
        }
    }

    // not made final as it needs to be replaced from a unit test
    private static Log log = LogFactory.getLog(BatchAggregator.class);

    private static ThreadPool threadPool = SharedThreadPool.getInstance();

    private List<T> currentBatch;

    // private Deque<List<T>> outputQueue = new LinkedList<List<T>>();

    private final int maxBatchSize;

    private final long maxLatency;

    private final BatchProcessor<T> processor;

    private final AsyncOrderedExecutionQueue dispatchQueue;

    /**
     * Receiver interface for generated batches.
     * 
     * @param <TT> the element type of the received batches; should match the associated {@link BatchAggregator}
     * 
     * @author Robert Mischke
     */
    public interface BatchProcessor<TT> {

        /**
         * Callback method for a single generated batch.
         * 
         * @param batch the generated batch following the FIFO principle
         */
        void processBatch(List<TT> batch);
    }

    public BatchAggregator(int maxBatchSize, long maxLatency, BatchProcessor<T> processor) {
        this.maxBatchSize = maxBatchSize;
        this.maxLatency = maxLatency;
        this.processor = processor;
        // TODO @5.0: policy chosen for backwards compatibility; review/make configurable?
        this.dispatchQueue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, threadPool);
    }

    /**
     * Adds an element for aggregation. May trigger the internal creation of a new batch or the sending of a finished batch when the the
     * maximum size limit is reached.
     * 
     * @param element the element to add
     */
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
        BatchAggregator.log = newLog;
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
        threadPool.scheduleAfterDelay(new MaxLatencyTimerCallback(currentBatch), maxLatency);
    }

    private void endCurrentBatchAndEnqueueForProcessing() {
        // note: it is important that this Runnable copies the current reference, and does not accidentally use the field! - misc_ro
        dispatchQueue.enqueue(new DispatchRunnable<T>(currentBatch, processor));
        currentBatch = null;
    }

}
