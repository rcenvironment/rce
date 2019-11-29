/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

import java.util.concurrent.TimeoutException;

/**
 * A queue where {@link Runnable}s can be added to be executed asynchronously, but in the order they were enqueued in, with no overlap
 * between the individual {@link Runnable}s. Execution is delegated to a {@link AsyncTaskService} provided at creation.
 * 
 * @author Robert Mischke
 */
public interface AsyncOrderedExecutionQueue {

    /**
     * The common category name to count statistics for tasks executed through this class under. This simplifies the monitoring of the tasks
     * are executed via this mechanism. - misc_ro
     * <p>
     * Example: StatsCounter.count(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME, "my custom task name")
     */
    @Deprecated
    // TODO find a better place/solution for this
    String STATS_COUNTER_SHARED_CATEGORY_NAME = "AsyncOrderedExecutionQueue dispatch";

    /**
     * Enqueues a {@link Runnable} task for execution. All tasks are guaranteed to be executed in the order they were enqueued in.
     * 
     * @param task the task to enqueue
     */
    void enqueue(Runnable task);

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. This call does not interrupt the
     * currently running task, if one exists, but it does not wait for its completion either.
     */
    void cancelAsync();

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. If a task is currently running, it is
     * not interrupted, but this method will wait until it completes by itself (with a timeout).
     * 
     * @throws TimeoutException if an internal time limit (currently 30 seconds) is exceeded while waiting for the current task to complete
     */
    void cancelAndWaitForLastRunningTask() throws TimeoutException;

    /**
     * Gracefully cancels this queue. All pending elements are removed, and no more tasks are started. This call does not interrupt the
     * currently running task, if one exists.
     * 
     * @param waitForShutdown if true, this task waits until the last running job (if it exists) has finished; otherwise, this method always
     *        returns immediately
     * @throws TimeoutException if an internal time limit (currently 30 seconds) is exceeded while waiting
     */

    // use the above methods for clarity
    void cancel(boolean waitForShutdown) throws TimeoutException;

}
