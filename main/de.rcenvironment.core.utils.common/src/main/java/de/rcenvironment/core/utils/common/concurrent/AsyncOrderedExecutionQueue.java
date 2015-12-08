/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A queue where {@link Runnable}s can be added to be executed asynchronously, but in the order they were enqueued in, with no overlap
 * between the individual {@link Runnable}s. Execution is delegated to a {@link ThreadPool} provided at creation.
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedExecutionQueue {

    /**
     * The common category name to count statistics for tasks executed through this class under. This simplifies the monitoring of the tasks
     * are executed via this mechanism. - misc_ro
     * <p>
     * Example: StatsCounter.count(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME, "my custom task name")
     */
    public static final String STATS_COUNTER_SHARED_CATEGORY_NAME = "AsyncOrderedExecutionQueue dispatch";

    private static final String ASYNC_TASK_DESCRIPTION = STATS_COUNTER_SHARED_CATEGORY_NAME;

    private static final int MAXIMUM_QUEUE_CANCEL_WAIT_SECONDS = 30;

    private final CountDownLatch cancelCompleteLatch = new CountDownLatch(1);

    /**
     * A {@link Runnable} that dispatches the first element from the queue, and enqueues itself again if the queue is not empty afterwards.
     * 
     * @author Robert Mischke
     */
    private final class DispatchRunnable implements Runnable {

        @Override
        @TaskDescription(ASYNC_TASK_DESCRIPTION)
        public void run() {
            final Runnable preExecutionFirst;
            synchronized (queue) {
                preExecutionFirst = queue.peekFirst();
            }
            // cancel marker reached? ("poison pill" approach)
            if (preExecutionFirst == null) {
                log.debug("Queue cancelled, discarding queued trigger");
                cancelCompleteLatch.countDown();
                // leave marker in queue for other queued dispatchers
                return;
            }
            try {
                preExecutionFirst.run();
            } catch (RuntimeException e) {
                switch (exceptionPolicy) {
                case LOG_AND_CANCEL_LISTENER:
                    log.error("Error in asynchronous callback; shutting down queue (as defined by exception policy)", e);
                    try {
                        AsyncOrderedExecutionQueue.this.cancel(false);
                    } catch (TimeoutException e2) {
                        log.error("Timeout exceeded while cancelling queue after a task exception", e2);
                    }
                    return; // do not enqueue again
                case LOG_AND_PROCEED:
                    log.error("Error in asynchronous callback; continuing (as defined by exception policy)", e);
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
            synchronized (queue) {
                Runnable postExecutionFirst = queue.removeFirst();
                if (preExecutionFirst != postExecutionFirst) {
                    if (postExecutionFirst == null) {
                        log.debug("Queue cancelled during a task's execution; stopping dispatcher "
                            + "and waiting for the running task to complete");
                        cancelCompleteLatch.countDown();
                        return; // do not enqueue again
                    } else {
                        throw new IllegalStateException("Queue corruption");
                    }
                }
                // if this was not the last queued callback, enqueue the dispatcher again
                if (!queue.isEmpty()) {
                    // note: this approach gives more fairness between concurrent queues, but is slightly less efficient than
                    // executing all queued Runnables each time a DispatchRunnable is executed - misc_ro
                    threadPool.execute(dispatchRunnable); // == this
                }
            }
        }
    }

    private final AsyncCallbackExceptionPolicy exceptionPolicy;

    private final ThreadPool threadPool;

    private final Deque<Runnable> queue;

    private final Runnable dispatchRunnable;

    private final Log log = LogFactory.getLog(getClass());

    public AsyncOrderedExecutionQueue(final AsyncCallbackExceptionPolicy exceptionPolicy, final ThreadPool threadPool) {
        this.exceptionPolicy = exceptionPolicy;
        this.threadPool = threadPool;
        this.queue = new LinkedList<Runnable>();
        this.dispatchRunnable = new DispatchRunnable();
    }

    /**
     * Enqueues a {@link Runnable} task for execution. All tasks are guaranteed to be executed in the order they were enqueued in.
     * 
     * @param task the task to enqueue
     */
    public void enqueue(final Runnable task) {
        boolean isFirst;
        synchronized (queue) {
            queue.addLast(task);
            // if this is the first queued callback, enqueue a new dispatch task
            isFirst = queue.size() == 1;
        }
        if (isFirst) {
            threadPool.execute(dispatchRunnable);
        }
    }

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. This call does not interrupt the
     * currently running task, if one exists, but it does not wait for its completion either.
     */
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
    public void cancelAndWaitForLastRunningTask() throws TimeoutException {
        cancelAsync();
        try {
            if (!cancelCompleteLatch.await(MAXIMUM_QUEUE_CANCEL_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new TimeoutException("Maximum wait time for queue shutdown exceeded");
            }
        } catch (InterruptedException e) {
            log.warn("Thread interrupted while waiting for queue shutdown");
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
    @Deprecated
    // use the above methods for clarity
    public void cancel(boolean waitForShutdown) throws TimeoutException {
        if (waitForShutdown) {
            cancelAndWaitForLastRunningTask();
        } else {
            cancelAsync();
        }
    }

}
