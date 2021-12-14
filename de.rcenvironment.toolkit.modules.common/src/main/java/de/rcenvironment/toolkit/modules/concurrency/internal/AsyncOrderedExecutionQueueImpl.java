/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.modules.statistics.api.CounterCategory;
import de.rcenvironment.toolkit.modules.statistics.api.StatisticsFilterLevel;

/**
 * Default {@link AsyncOrderedExecutionQueue} implementation.
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedExecutionQueueImpl implements AsyncOrderedExecutionQueue {

    private static final String ASYNC_TASK_DESCRIPTION = AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME;

    private static final int MAXIMUM_QUEUE_CANCEL_WAIT_SECONDS = 30;

    private final CountDownLatch cancelCompleteLatch = new CountDownLatch(1);

    /**
     * A {@link Runnable} that dispatches the first element from the queue, and enqueues itself again if the queue is not empty afterwards.
     * 
     * @author Robert Mischke
     */
    private final class DispatchRunnable implements Runnable {

        @Override
        public void run() {
            // dispatch all currently queued elements in the same execution to avoid thread switching and thread pool overhead
            while (dispatchSingleElement()) {
                // sometimes, CheckStyle rules just don't make sense...
                @SuppressWarnings("unused") int i = 0; // (this should be eliminated by the compiler)
            }
        }

        /**
         * @return true if dispatching should continue, ie the queue is neither empty nor canceled
         */
        private boolean dispatchSingleElement() {
            final Runnable preExecutionFirst;
            synchronized (queue) {
                preExecutionFirst = queue.peekFirst();
            }
            // cancel marker reached? ("poison pill" approach)
            if (preExecutionFirst == null) {
                log.debug("Queue cancelled, discarding queued trigger; queue id: " + getLogId());
                cancelCompleteLatch.countDown();
                // note: leaving marker in queue for other queued dispatchers
                return false; // do not continue dispatching
            }
            try {
                if (elementCounter.isEnabled()) {
                    elementCounter.countClass(preExecutionFirst);
                }
                preExecutionFirst.run();
            } catch (RuntimeException e) {
                switch (exceptionPolicy) {
                case LOG_AND_CANCEL_LISTENER:
                    log.error("Error in asynchronous callback; shutting down queue (as defined by exception policy); queue id: "
                        + getLogId(), e);
                    AsyncOrderedExecutionQueueImpl.this.cancelAsync();
                    return false; // do not continue dispatching
                case LOG_AND_PROCEED:
                    log.error("Error in asynchronous callback; continuing (as defined by exception policy); queue id: "
                        + getLogId(), e);
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
            synchronized (queue) {
                // do not remove the first element in case it is the poison pill to ensure proper cancellation
                Runnable postExecutionFirst = queue.peekFirst();
                if (postExecutionFirst == null) {
                    log.debug("Queue cancelled during a task's execution; stopping dispatcher "
                        + "and waiting for the running task to complete; queue id: " + getLogId());
                    cancelCompleteLatch.countDown();
                    return false; // do not continue dispatching
                }

                postExecutionFirst = queue.removeFirst();
                if (preExecutionFirst != postExecutionFirst) {
                    throw new IllegalStateException("Queue corruption (queue id: " + getLogId() + ")");
                }
                // if this was not the last queued callback, continue dispatching
                return !queue.isEmpty();
            }
        }

    }

    private final AsyncCallbackExceptionPolicy exceptionPolicy;

    private final AsyncTaskService threadPool;

    private final Deque<Runnable> queue;

    private final Runnable dispatchRunnable;

    private final CounterCategory elementCounter;

    private final Log log = LogFactory.getLog(getClass());

    public AsyncOrderedExecutionQueueImpl(final AsyncCallbackExceptionPolicy exceptionPolicy,
        final ConcurrencyUtilsServiceHolder internalServiceHolder) {
        this.exceptionPolicy = exceptionPolicy;
        this.threadPool = internalServiceHolder.getAsyncTaskService();
        this.queue = new LinkedList<Runnable>();
        this.dispatchRunnable = new DispatchRunnable();
        this.elementCounter = internalServiceHolder.getStatisticsTrackerService().getCounterCategory(
            "AsyncOrderedExecutionQueue elements dispatched", StatisticsFilterLevel.DEVELOPMENT);
    }

    /**
     * Enqueues a {@link Runnable} task for execution. All tasks are guaranteed to be executed in the order they were enqueued in.
     * 
     * @param task the task to enqueue
     */
    @Override
    public void enqueue(final Runnable task) {
        boolean isFirst;
        synchronized (queue) {
            queue.addLast(task);
            // if this is the first queued callback, enqueue a new dispatch task
            isFirst = queue.size() == 1;
        }
        if (isFirst) {
            threadPool.execute(ASYNC_TASK_DESCRIPTION, dispatchRunnable);
        }
    }

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. This call does not interrupt the
     * currently running task, if one exists, but it does not wait for its completion either.
     */
    @Override
    public void cancelAsync() {
        synchronized (queue) {
            if (queue.isEmpty()) {
                cancelCompleteLatch.countDown();
            }
            queue.clear();
            queue.add(null); // add cancel marker
        }
    }

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. If a task is currently running, it is
     * not interrupted, but this method will wait until it completes by itself (with a timeout).
     * 
     * @throws TimeoutException if an internal time limit (currently 30 seconds) is exceeded while waiting for the current task to complete
     */
    @Override
    public void cancelAndWaitForLastRunningTask() throws TimeoutException {
        cancelAsync();
        try {
            if (!cancelCompleteLatch.await(MAXIMUM_QUEUE_CANCEL_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new TimeoutException("Maximum wait time for queue shutdown exceeded");
            }
        } catch (InterruptedException e) {
            log.warn("Thread interrupted while waiting for queue shutdown; queue id: " + getLogId());
        }
    }

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. This call does not interrupt the
     * currently running task, if one exists.
     * 
     * @param waitForShutdown if true, this task waits until the last running job (if it exists) has finished; otherwise, this method always
     *        returns immediately
     * @throws TimeoutException if an internal time limit (currently 30 seconds) is exceeded while waiting
     */
    @Override
    @Deprecated
    // use the above methods for clarity
    public void cancel(boolean waitForShutdown) throws TimeoutException {
        if (waitForShutdown) {
            cancelAndWaitForLastRunningTask();
        } else {
            cancelAsync();
        }
    }

    private int getLogId() {
        return System.identityHashCode(this);
    }
}
