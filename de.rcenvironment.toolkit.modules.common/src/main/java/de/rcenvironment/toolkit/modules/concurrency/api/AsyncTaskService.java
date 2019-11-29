/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
 * without additional boilerplate code in task objects. (TODO update/expand this description)
 * 
 * @author Robert Mischke
 */
public interface AsyncTaskService {

    /**
     * Adds a task for asynchronous execution.
     * 
     * @param task the {@link Runnable} to execute
     * 
     * @see ExecutorService#execute(Runnable)
     */
    @Deprecated
    void execute(Runnable task);

    /**
     * Adds a task for asynchronous execution.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param task the {@link Runnable} or lambda to execute
     * 
     * @see ExecutorService#execute(Runnable)
     */
    void execute(String categoryName, Runnable task);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * @param task the {@link Runnable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     * 
     * @see ExecutorService#execute(Runnable)
     */
    @Deprecated
    void execute(Runnable task, String taskId);

    /**
     * Adds a task for asynchronous execution.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param taskId an optional id to associate with this specific task instance; leave null unless needed to identify individual tasks
     * @param task the {@link Runnable} or lambda to execute
     * 
     * @see ExecutorService#execute(Runnable)
     */
    void execute(String categoryName, String taskId, Runnable task);

    /**
     * Adds a task for asynchronous execution.
     * <p>
     * In contrast to {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its
     * completion, or attempt to cancel it.
     * 
     * @param task the {@link Runnable} to execute
     * @return the created {@link Future}
     * 
     * @see ExecutorService#submit(Runnable)
     */
    @Deprecated
    Future<?> submit(Runnable task);

    /**
     * Adds a task for asynchronous execution.
     * <p>
     * In contrast to {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its
     * completion, or attempt to cancel it.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param task the {@link Runnable} to execute
     * @return the created {@link Future}
     * 
     * @see ExecutorService#submit(Runnable)
     */
    Future<?> submit(String categoryName, Runnable task);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param task the {@link Runnable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     * @return the created {@link Future}
     * 
     * @see ExecutorService#submit(Runnable)
     */
    @Deprecated
    Future<?> submit(Runnable task, String taskId);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param taskId an optional id to associate with this specific task instance; leave null unless needed to identify individual tasks
     * @param task the {@link Runnable} to execute
     * @return the created {@link Future}
     * 
     * @see ExecutorService#submit(Runnable)
     */
    Future<?> submit(String categoryName, String taskId, Runnable task);

    /**
     * Adds a task for asynchronous execution.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param task the {@link Callable} to execute
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     * 
     * @see ExecutorService#submit(Callable)
     */
    @Deprecated
    <T> Future<T> submit(Callable<T> task);

    /**
     * Adds a task for asynchronous execution.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param task the {@link Callable} to execute
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     * 
     * @see ExecutorService#submit(Callable)
     */
    <T> Future<T> submit(String categoryName, Callable<T> task);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param task the {@link Callable} to execute
     * @param taskId the task id to attach to this task (can be null to disable); task ids for the same {@link Runnable} must be unique
     *        while they are active
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     * 
     * @see ExecutorService#submit(Callable).
     */
    @Deprecated
    <T> Future<T> submit(Callable<T> task, String taskId);

    /**
     * Adds a task for asynchronous execution, with an additional task id to identify the task in monitoring and debug output.
     * 
     * Unlike {@link #execute()}, this method also returns a {@link Future} for the task, which can be used to wait for its completion, or
     * attempt to cancel it.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param taskId an optional id to associate with this specific task instance; leave null unless needed to identify individual tasks
     * @param task the {@link Callable} to execute
     * @param <T> the return type of the {@link Callable}
     * @return the result of the {@link Callable}
     * 
     * @see ExecutorService#submit(Callable).
     */
    <T> Future<T> submit(String categoryName, String taskId, Callable<T> task);

    /**
     * A simplified version of {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)} for scheduling a single-time task for
     * execution after a specified delay. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param task the {@link Runnable} to execute periodically
     * @param delayMsec the delay before the first execution
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    @Deprecated
    ScheduledFuture<?> scheduleAfterDelay(Runnable task, long delayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)} for scheduling a single-time task for
     * execution after a specified delay. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param task the {@link Runnable} to execute periodically
     * @param delayMsec the delay before the first execution
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    ScheduledFuture<?> scheduleAfterDelay(String categoryName, Runnable task, long delayMsec);

    /**
     * A variant of {@link #scheduleAfterDelay(Runnable, long)} that schedules a {@link Callable} for use cases where a return value is
     * required. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Callable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param <T> the return value's type
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param callable the {@link Callable} to execute periodically
     * @param delayMsec the delay before the first execution
     * @return a {@link ScheduledFuture} that can be used to wait for the task's completion, on which it returns 'null'; TODO clarify: is it
     *         possible to cancel tasks via this?
     */
    <T> ScheduledFuture<T> scheduleAfterDelay(String categoryName, Callable<T> callable, long delayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}, and the initial and repetition delays are set to the
     * same value.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param repetitionDelayMsec the delay before the first and between the starting times of subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    @Deprecated
    ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long repetitionDelayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}, and the initial and repetition delays are set to the
     * same value.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param runnable the {@link Runnable} to execute periodically
     * @param repetitionDelayMsec the delay before the first and between the starting times of subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedRate(String categoryName, Runnable runnable, long repetitionDelayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}, and the initial and repetition delays are set to the
     * same value.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param runnable the {@link Runnable} to execute periodically
     * @param repetitionDelayMsec the delay before the first execution, and between the end of the last and the start of the next execution
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedInterval(String categoryName, Runnable runnable, long repetitionDelayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param initialDelayMsec the delay before the first execution
     * @param repetitionDelayMsec the delay between the starting times of subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    @Deprecated
    ScheduledFuture<?> scheduleAtFixedRateAfterDelay(Runnable runnable, long initialDelayMsec, long repetitionDelayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} for scheduling periodic
     * background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param initialDelayMsec the delay before the first execution
     * @param repetitionDelayMsec the delay between the starting times of subsequent executions
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedRateAfterDelay(String categoryName, Runnable runnable, long initialDelayMsec,
        long repetitionDelayMsec);

    /**
     * A simplified version of {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} for scheduling
     * periodic background tasks. The {@link TimeUnit} is always {@link TimeUnit#MILLISECONDS}.
     * 
     * {@link Runnable}s scheduled with this method are included in the thread pool statistics.
     * 
     * @param runnable the {@link Runnable} to execute periodically
     * @param categoryName a human-readable short description of this type of task; used in statistics and monitoring output
     * @param initialDelayMsec the delay before the first execution
     * @param repetitionDelayMsec the delay between the end of the last and the start of the next execution
     * @return a {@link ScheduledFuture} that can be used to cancel the task
     */
    ScheduledFuture<?> scheduleAtFixedIntervalAfterInitialDelay(String categoryName, Runnable runnable, long initialDelayMsec,
        long repetitionDelayMsec);

}
