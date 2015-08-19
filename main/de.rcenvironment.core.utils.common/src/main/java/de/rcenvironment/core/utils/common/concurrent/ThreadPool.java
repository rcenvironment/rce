/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interface for abstract thread pool operations, like enqueueing asynchronous tasks, or scheduling repeated background tasks. Semantically
 * similar to {@link ExecutorService}, but simplified to a smaller set of methods and parameters.
 * 
 * @author Robert Mischke
 */
public interface ThreadPool {

    /**
     * Adds a task for asynchronous execcution.
     * 
     * @see ExecutorService#execute(Runnable)
     * 
     * @param task the {@link Runnable} to execute
     */
    void execute(Runnable task);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * @see ExecutorService#execute(Runnable)
     * 
     * @param task the {@link Runnable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     */
    void execute(Runnable task, String taskId);

    /**
     * Adds a task for asynchronous execution. Unlike {@link #execute(Runnable)}, this variant also returns a {@link Future} for the task,
     * which can be used to wait for its completion, or attempt to cancel it.
     * 
     * @see ExecutorService#submit(Runnable)
     * 
     * @param task the {@link Runnable} to execute
     * @return the created {@link Future}
     */
    Future<?> submit(Runnable task);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * Unlike {@link #execute(Runnable, String)}, this variant also returns a {@link Future} for the task, which can be used to wait for its
     * completion, or attempt to cancel it.
     * 
     * @see ExecutorService#submit(Runnable)
     * 
     * @param task the {@link Runnable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     * @return the created {@link Future}
     */
    Future<?> submit(Runnable task, String taskId);

    /**
     * @see ExecutorService#submit(Callable)
     * 
     * @param task the {@link Callable} to execute
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * @see ExecutorService#submit(Callable). This variant allows to specify an additional task id to identify the task in monitoring and
     *      debug output.
     * 
     * @param task the {@link Callable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     */
    <T> Future<T> submit(Callable<T> task, String taskId);

    /**
     * @return a enw {@link RunnablesGroup} backed by this thread pool.
     * 
     * @see #createCallablesGroup(Class)
     */
    RunnablesGroup createRunnablesGroup();

    /**
     * Creates a {@link CallablesGroup} that uses the internal thread pool.
     * 
     * @param clazz the return type of the {@link Callable}s to execute
     * @param <T> the return type of the {@link Callable}s to execute
     * @return the {@link CallablesGroup} instance
     */
    <T> CallablesGroup<T> createCallablesGroup(Class<T> clazz);

    /**
     * A simplified version of {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)} for scheduling a single-time task for
     * execution after a specified delay. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param delayMsec the delay before the first and between subsequent executions
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    ScheduledFuture<?> scheduleAfterDelay(Runnable runnable, long delayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}, and the initial and repetition delays are set to the
     * same value.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param repetitionDelayMsec the delay before the first and between subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long repetitionDelayMsec);

}
