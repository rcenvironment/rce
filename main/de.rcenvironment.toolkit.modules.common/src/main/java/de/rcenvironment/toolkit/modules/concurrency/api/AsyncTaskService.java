/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

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
 * Additionally, this service provides features to attach an opaque {@link Object} to the current {@link Thread}, which is then propagated
 * to all asynchronous tasks spawned from that {@link Thread}. This allows to forward context information to decoupled method invocations
 * without additional boilerplate code in task objects.
 * 
 * @author Robert Mischke
 */
public interface AsyncTaskService {

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
     * A simplified version of {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)} for scheduling a single-time task for
     * execution after a specified delay. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param delayMsec the delay before the first execution
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    ScheduledFuture<?> scheduleAfterDelay(Runnable runnable, long delayMsec);

    /**
     * A variant of {@link #scheduleAfterDelay(Runnable, long)} that schedules a {@link Callable} for use cases where a return value is
     * required. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Callable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param <T> the return value's type
     * @param callable the {@link Callable} to execute periodically
     * @param delayMsec the delay before the first execution
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    <T> ScheduledFuture<T> scheduleAfterDelay(Callable<T> callable, long delayMsec);

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

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param initialDelayMsec the delay before the first execution
     * @param repetitionDelayMsec the delay between subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedRateAfterDelay(Runnable runnable, long initialDelayMsec, long repetitionDelayMsec);

}
