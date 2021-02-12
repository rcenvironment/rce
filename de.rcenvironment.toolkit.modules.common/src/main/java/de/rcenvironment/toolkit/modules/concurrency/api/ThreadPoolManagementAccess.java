/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.toolkit.modules.concurrency.api;

/**
 * Exposes methods to query the thread pool for statistics, and to actively reset the pool itself (especially for unit and integration
 * tests).
 * 
 * @author Robert Mischke
 */
public interface ThreadPoolManagementAccess {

    /**
     * Shuts down the internal executor. This should terminate all well-behaved threads and tasks (ie, those that properly react to
     * interruption).
     * 
     * Note that task statistics are not cleared by shutdown, and can still be fetched afterwards.
     * 
     * @return the number of enqueued tasks that were never started; equal to the return value of ExecutorService#shutdown().
     */
    int shutdown();

    /**
     * Intended for unit tests; shuts down the internal executor and replaces it with a new one. This should terminate all well-behaved
     * threads and tasks (ie, those that properly react to interruption).
     * 
     * Note that no synchronization is performed when replacing the internal executor; it is up to the caller to ensure proper thread
     * visibility.
     * 
     * @return the number of enqueued tasks that were never started; should usually be zero
     */
    int reset();

    /**
     * @return the approximate thread count of the current pool
     * 
     * @see {@link ThreadGroup#activeCount()}.
     */
    int getCurrentThreadCount();

    /**
     * Returns a human-readable String representation of the collected statistics.
     * 
     * @param addTaskIds true if the ids of tasks that provide them should be included
     * 
     * @return a String representation of the collected statistics
     */
    String getFormattedStatistics(boolean addTaskIds);

    /**
     * Returns a human-readable String representation of the collected statistics.
     * 
     * @param addTaskIds true if the ids of tasks that provide them should be included
     * @param includeInactive if tasks with a zero "active" count should be included
     * 
     * @return a String representation of the collected statistics
     */
    String getFormattedStatistics(boolean addTaskIds, boolean includeInactive);

}
