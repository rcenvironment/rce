/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadPoolManagementAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContext;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextHolder;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextMemento;
import de.rcenvironment.toolkit.modules.concurrency.setup.ConcurrencyModuleConfiguration;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionContributor;
import de.rcenvironment.toolkit.modules.introspection.api.StatusCollectionRegistry;
import de.rcenvironment.toolkit.utils.internal.StringUtils;
import de.rcenvironment.toolkit.utils.text.TextLinesReceiver;
import de.rcenvironment.toolkit.utils.text.impl.BufferingTextLinesReceiver;
import de.rcenvironment.toolkit.utils.text.impl.MultiLineOutputWrapper;

/**
 * A utility wrapper class that provides a shared ExecutorService using the {@link AsyncTaskService} interface. Its main purpose is to avoid
 * the redundant creation of thread pools for each service, which would needlessly increase the global thread count. An additional benefit
 * is the ability to reset the thread pools of the communication layer, which is useful to ensure test isolation, and to measure maximum
 * thread usage.
 * 
 * Thread monitoring and debugging is also simplified by providing recognizable thread names.
 * 
 * In addition, this method collects execution statistics about the processed {@link Callable}s and {@link Runnable}s.
 * 
 * @author Robert Mischke
 */
public final class AsyncTaskServiceImpl implements AsyncTaskService, ThreadPoolManagementAccess {

    // compatibility and test flag: declare this property to make the thread pool behave like RCE 7.0.x.
    // TODO (p3) remove in 8.0.0 or later
    @Deprecated
    private static final String SYSTEM_PROPERTY_USE_70x_THREAD_POOL_CONFIGURATION = "rce.threadpool.use70xBehavior";

    // preliminary cap to prevent excessive thread allocation; quite high as currently, network forwarding is still a blocking operation, so
    // a lower cap may bottleneck busy relay nodes in very slow networks; also, workflow execution is unbounded. this is planned to be
    // reworked, and then this value should be lowered significantly to reduce thread switching.
    private static final int DEFAULT_COMMON_THREAD_POOL_SIZE = 512;

    private static final String DEFAULT_THREAD_NAME_PREFIX = "ToolkitThreadPool-";

    private static final long IDLE_THREAD_RELEASE_TIME_SECONDS = 60; // JDK default time: 60 seconds

    // the number of scheduled/repeated tasks that can execute concurrently; adjust as necessary
    private static final int NUM_THREADS_FOR_SCHEDULED_TASKS = 4;

    private static final float NANOS_TO_MSEC_RATIO = 1000000f;

    /**
     * A simple holder for statistical data.
     * 
     * @author Robert Mischke
     */
    private final class StatisticsEntry {

        private int activeTasks;

        private int maxParallel;

        private int completedTasks;

        private int exceptionCount;

        private long maxNormalCompletionTime;

        private long totalCompletionTime;

        // only initialized once a non-null taskId is passed in
        private Map<String, Thread> activeTaskIds;

        // only initialized once a null taskId is passed in
        private Set<Thread> anonymousTaskThreads;

        private final String categoryName;

        StatisticsEntry(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getCategoryName() {
            return categoryName;
        }

        private synchronized void beforeExecution(String taskId) {
            activeTasks++;
            if (activeTasks > maxParallel) {
                maxParallel = activeTasks;
            }
            if (taskId != null) {
                if (activeTaskIds == null) {
                    activeTaskIds = new HashMap<>();
                }
                Thread replaced = activeTaskIds.put(taskId, Thread.currentThread());
                if (replaced != null) {
                    log.warn(StringUtils.format("Task id '%s' used more than once for task '%s' (existing: %s, new: %s)", taskId,
                        categoryName, replaced.getName(), Thread.currentThread().getName()), new RuntimeException());
                }
            } else {
                if (anonymousTaskThreads == null) {
                    anonymousTaskThreads = new HashSet<>();
                }
                if (!anonymousTaskThreads.add(Thread.currentThread())) {
                    // sanity check
                    log.error("Consistency error: Thread " + Thread.currentThread() + " is already in the set of active tasks");
                }
            }
        }

        private synchronized void afterExecution(String taskId, long duration, boolean exception) {
            totalCompletionTime += duration;
            completedTasks++;
            activeTasks--;
            if (taskId != null) {
                if (activeTaskIds == null) {
                    // should never happen
                    // TODO >4.0.0: change to exception
                    log.error("Consistency error: Non-null task id finished, but active set not initialized");
                    // prevent NPE until changed
                    activeTaskIds = new HashMap<>();
                }
                Thread removed = activeTaskIds.remove(taskId);
                if (removed == null) {
                    log.warn(StringUtils.format("No registered task id '%s' for task '%s'; was there an id collision before?", taskId,
                        categoryName));
                }
            } else {
                if (!anonymousTaskThreads.remove(Thread.currentThread())) {
                    // sanity check
                    log.error("Consistency error: Thread " + Thread.currentThread() + " was not in the set of active tasks");
                }
            }
            if (exception) {
                exceptionCount++;
            } else {
                if (duration > maxNormalCompletionTime) {
                    maxNormalCompletionTime = duration;
                }
            }
        }

        /**
         * Adds a String representation of this entry to the given {@link StringBuilder}.
         * 
         * @param sb the {@link StringBuilder} to append to
         */
        private void printFormatted(StringBuilder sb) {
            int numCompleted = completedTasks;
            int numActive = activeTasks;
            sb.append("Active: ");
            sb.append(numActive);
            sb.append(", Completed: ");
            sb.append(numCompleted);
            sb.append(", MaxParallel: ");
            sb.append(maxParallel);
            if (numCompleted > 0) {
                long totalTimeNanos = totalCompletionTime;
                float avgTimeMsec = totalTimeNanos / NANOS_TO_MSEC_RATIO / numCompleted;
                sb.append(", AvgTime: ");
                sb.append(avgTimeMsec);
                sb.append(" msec, Total: ");
                sb.append(totalTimeNanos / NANOS_TO_MSEC_RATIO);
                sb.append(" msec, MaxTime: ");
                sb.append(maxNormalCompletionTime / NANOS_TO_MSEC_RATIO);
                sb.append(" msec");
            }
            if (exceptionCount > 0) {
                sb.append(", Exceptions: ");
                sb.append(exceptionCount);
            }
        }

    }

    /**
     * An internal wrapper for enqueued {@link Callable}s.
     * 
     * @param <T> the callback type of the {@link Callable}s
     * 
     * @author Robert Mischke
     */
    private class WrappedCallable<T> implements Callable<T> {

        private final Callable<T> innerCallable;

        private final String taskId;

        private final ThreadContext contextObject;

        private final StatisticsEntry statisticsEntry;

        WrappedCallable(Callable<T> callable, StatisticsEntry statisticsEntry, String taskId) {
            this.innerCallable = callable;
            this.statisticsEntry = statisticsEntry;
            this.taskId = taskId;
            this.contextObject = ThreadContextHolder.getCurrentContext(); // transfer from calling thread
        }

        @Override
        public T call() throws Exception {
            final ThreadContextMemento previousThreadContext = ThreadContextHolder.setCurrentContext(contextObject); // apply

            final long startTime = System.nanoTime();
            final T result;

            statisticsEntry.beforeExecution(taskId);
            boolean exception = false;
            try {
                try {
                    result = innerCallable.call();
                } catch (RejectedExecutionException e) {
                    log.debug("Execution of Callable for task " + statisticsEntry.getCategoryName()
                        + " was rejected, typically because the thread pool is shutting down; detail information: " + e.toString());
                    exception = true;
                    throw e;
                } catch (RuntimeException e) {
                    log.warn("Unhandled exception in Callable for task " + statisticsEntry.getCategoryName(), e);
                    exception = true;
                    throw e;
                }
            } finally {
                long duration = System.nanoTime() - startTime;
                statisticsEntry.afterExecution(taskId, duration, exception);
                previousThreadContext.restore(); // restore original worker thread context (should be null)
            }
            if (Thread.interrupted()) {
                log.debug(StringUtils.format("Thread %s was interrupted after running task '%s', resetting flag", Thread
                    .currentThread().getName(), statisticsEntry.getCategoryName()));
            }
            return result;
        }

    }

    /**
     * An internal wrapper for enqueued {@link Runnable}s.
     * 
     * @author Robert Mischke
     */
    private final class WrappedRunnable implements Runnable {

        private final Runnable innerRunnable;

        private final String taskId;

        private final ThreadContext contextObject;

        private final StatisticsEntry statisticsEntry;

        WrappedRunnable(Runnable runnable, StatisticsEntry statisticsEntry, String taskId) {
            this.innerRunnable = runnable;
            this.statisticsEntry = statisticsEntry;
            this.taskId = taskId;
            this.contextObject = ThreadContextHolder.getCurrentContext(); // transfer from calling thread
        }

        @Override
        public void run() {
            final ThreadContextMemento previousThreadContext = ThreadContextHolder.setCurrentContext(contextObject); // apply
            final long startTime = System.nanoTime();
            statisticsEntry.beforeExecution(taskId);
            boolean exception = false;
            try {
                try {
                    innerRunnable.run();
                } catch (RejectedExecutionException e) {
                    log.debug("Execution of Runnable for task " + statisticsEntry.getCategoryName()
                        + " was rejected, typically because the thread pool is shutting down; detail information: " + e.toString());
                    exception = true;
                } catch (RuntimeException e) {
                    log.warn("Unhandled exception in Runnable for task " + statisticsEntry.getCategoryName(), e);
                    exception = true;
                }
            } finally {
                long duration = System.nanoTime() - startTime;
                statisticsEntry.afterExecution(taskId, duration, exception);
                previousThreadContext.restore(); // restore original worker thread context (should be null)
            }
            if (Thread.interrupted()) {
                log.debug(StringUtils.format("Thread %s was interrupted after running task '%s', resetting flag", Thread
                    .currentThread().getName(), statisticsEntry.getCategoryName()));
            }
        }
    }

    private volatile ExecutorService executorService;

    private AtomicInteger poolIndex = new AtomicInteger(0);

    private AtomicInteger threadIndex = new AtomicInteger(0);

    private ThreadGroup currentThreadGroup;

    // not final to allow re-initialization on reset()
    private Map<String, StatisticsEntry> statisticsEntriesByCategoryName;

    // final as class annotations are not expected to change on reset()
    private final Map<Class<?>, String> categoryNamesForTaskClasses;

    private ScheduledExecutorService schedulerService;

    private final Log log = LogFactory.getLog(getClass());

    private final ConcurrencyModuleConfiguration configuration;

    public AsyncTaskServiceImpl(ConcurrencyModuleConfiguration configuration, StatusCollectionRegistry statusCollectionRegistry) {
        this.configuration = configuration; // stored to reuse it from the reset() method
        this.categoryNamesForTaskClasses = Collections.synchronizedMap(new HashMap<>());
        initialize();

        statusCollectionRegistry.addContributor(new StatusCollectionContributor() {

            @Override
            public String getStandardDescription() {
                return "Asynchronous Tasks";
            }

            @Override
            public void printDefaultStateInformation(TextLinesReceiver receiver) {
                renderStatistics(false, true, receiver);
            }

            @Override
            public String getUnfinishedOperationsDescription() {
                return null;
            }

            @Override
            public void printUnfinishedOperationsInformation(TextLinesReceiver receiver) {}
        });
    }

    @Override
    public void execute(String categoryName, Runnable runnable) {
        execute(categoryName, null, runnable); // delegate with a "null" task id
    }

    @Override
    public void execute(String categoryName, String taskId, Runnable runnable) {
        try {
            getNullSafeExecutorService().execute(new WrappedRunnable(runnable, getStatisticsEntry(categoryName), taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e;
        }
    }

    @Override
    @Deprecated
    public Future<?> submit(Runnable task) {
        return submit(task, null); // delegate with a "null" task id
    }

    @Override
    public Future<?> submit(String categoryName, Runnable task) {
        return submit(categoryName, null, task); // delegate with a "null" task id
    }

    @Override
    @Deprecated
    public Future<?> submit(Runnable runnable, String taskId) {
        try {
            return getNullSafeExecutorService().submit(new WrappedRunnable(runnable, getStatisticsEntry(runnable.getClass()), taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(runnable);
            throw e;
        }
    }

    @Override
    public Future<?> submit(String categoryName, String taskId, Runnable runnable) {
        try {
            return getNullSafeExecutorService().submit(new WrappedRunnable(runnable, getStatisticsEntry(categoryName), taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e;
        }
    }

    @Override
    @Deprecated
    public <T> Future<T> submit(Callable<T> task) {
        return submit(task, null); // delegate with a "null" task id
    }

    @Override
    public <T> Future<T> submit(String categoryName, Callable<T> task) {
        return submit(categoryName, null, task); // delegate with a "null" task id
    }

    @Override
    @Deprecated
    public <T> Future<T> submit(Callable<T> task, String taskId) {
        try {
            return getNullSafeExecutorService().submit(new WrappedCallable<T>(task, getStatisticsEntry(task.getClass()), taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(task);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public <T> Future<T> submit(String categoryName, String taskId, Callable<T> task) {
        try {
            return getNullSafeExecutorService().submit(new WrappedCallable<T>(task, getStatisticsEntry(categoryName), taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    @Deprecated
    public ScheduledFuture<?> scheduleAfterDelay(Runnable runnable, long delayMsec) {
        try {
            return getNullSafeSchedulerService().schedule(new WrappedRunnable(runnable, getStatisticsEntry(runnable.getClass()), null),
                delayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(runnable);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAfterDelay(String categoryName, Runnable runnable, long delayMsec) {
        try {
            return getNullSafeSchedulerService().schedule(new WrappedRunnable(runnable, getStatisticsEntry(categoryName), null),
                delayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public <T> ScheduledFuture<T> scheduleAfterDelay(String categoryName, Callable<T> callable, long delayMsec) {
        try {
            return getNullSafeSchedulerService().schedule(new WrappedCallable<T>(callable, getStatisticsEntry(categoryName), null),
                delayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    @Deprecated
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long repetitionDelayMsec) {
        try {
            return scheduleAtFixedRateAfterDelay(runnable, repetitionDelayMsec, repetitionDelayMsec); // delegate
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(runnable);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(String categoryName, Runnable runnable, long repetitionDelayMsec) {
        try {
            return scheduleAtFixedRateAfterDelay(categoryName, runnable, repetitionDelayMsec, repetitionDelayMsec); // delegate
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedInterval(String categoryName, Runnable runnable, long repetitionDelayMsec) {
        try {
            return scheduleAtFixedIntervalAfterInitialDelay(categoryName, runnable, repetitionDelayMsec, repetitionDelayMsec); // delegate´
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    @Deprecated
    public ScheduledFuture<?> scheduleAtFixedRateAfterDelay(Runnable runnable, long initialDelayMsec, long repetitionDelayMsec) {
        try {
            return getNullSafeSchedulerService().scheduleAtFixedRate(
                new WrappedRunnable(runnable, getStatisticsEntry(runnable.getClass()), null),
                initialDelayMsec, repetitionDelayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(runnable);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRateAfterDelay(String categoryName, Runnable runnable, long initialDelayMsec,
        long repetitionDelayMsec) {
        try {
            return getNullSafeSchedulerService().scheduleAtFixedRate(new WrappedRunnable(runnable, getStatisticsEntry(categoryName), null),
                initialDelayMsec, repetitionDelayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedIntervalAfterInitialDelay(String categoryName, Runnable runnable, long initialDelayMsec,
        long repetitionDelayMsec) {
        try {
            return getNullSafeSchedulerService().scheduleWithFixedDelay(
                new WrappedRunnable(runnable, getStatisticsEntry(categoryName), null),
                initialDelayMsec, repetitionDelayMsec, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(categoryName);
            throw e; // the alternative would be returning a synthetic Future; unclear which is better
        }
    }

    @Override
    public int shutdown() {
        log.debug("Shutting down thread pool");
        List<Runnable> queued = executorService.shutdownNow();
        executorService = null;
        schedulerService.shutdown();
        schedulerService = null;
        return queued.size();
    }

    @Override
    public int reset() {
        int unfinishedCount = shutdown();
        initialize();
        return unfinishedCount;
    }

    @Override
    public int getCurrentThreadCount() {
        return currentThreadGroup.activeCount();
    }

    @Override
    public String getFormattedStatistics(final boolean addTaskIds) {
        // backwards compatibility: included inactive tasks by default
        return getFormattedStatistics(addTaskIds, true);
    }

    @Override
    public String getFormattedStatistics(final boolean addTaskIds, final boolean includeInactive) {
        final BufferingTextLinesReceiver lineBuffer = new BufferingTextLinesReceiver();
        renderStatistics(addTaskIds, includeInactive, lineBuffer);
        return new MultiLineOutputWrapper(lineBuffer.getCollectedLines()).asMultilineString(); // TODO default indent?
    }

    private void renderStatistics(final boolean addTaskIds, final boolean includeInactive, TextLinesReceiver receiver) {
        final StringBuilder lineBuffer = new StringBuilder(512); // reserve sufficient space to avoid resizing
        final Map<String, StatisticsEntry> sortedMap = new TreeMap<>();
        synchronized (statisticsEntriesByCategoryName) {
            for (StatisticsEntry statisticsEntry : statisticsEntriesByCategoryName.values()) {
                if (statisticsEntry.activeTasks != 0 || includeInactive) {
                    sortedMap.put(statisticsEntry.getCategoryName(), statisticsEntry);
                }
            }
        }
        for (Entry<String, StatisticsEntry> entry : sortedMap.entrySet()) {
            String taskName = entry.getKey();
            StatisticsEntry statsEntry = entry.getValue();
            synchronized (statsEntry) {
                receiver.addLine(taskName);
                lineBuffer.setLength(0);
                // indent: 4
                lineBuffer.append("    ");
                statsEntry.printFormatted(lineBuffer);
                receiver.addLine(lineBuffer.toString());
                if (addTaskIds) {
                    if (statsEntry.activeTaskIds != null && !statsEntry.activeTaskIds.isEmpty()) {
                        // indent: 8
                        receiver.addLine("        Named tasks:");
                        lineBuffer.setLength(0);
                        for (Entry<String, Thread> taskIdEntry : statsEntry.activeTaskIds.entrySet()) {
                            // indent: 10
                            receiver.addLine(
                                StringUtils.format("          %s [%s]", taskIdEntry.getKey(), taskIdEntry.getValue().getName()));
                        }
                    }
                    if (statsEntry.anonymousTaskThreads != null && !statsEntry.anonymousTaskThreads.isEmpty()) {
                        // indent: 8
                        receiver.addLine("        Anonymous task threads:");
                        for (Thread thread : statsEntry.anonymousTaskThreads) {
                            // indent: 10
                            receiver.addLine(
                                StringUtils.format("          [%s]", thread.getName()));
                        }
                    }
                }
            }
        }
    }

    private ExecutorService getNullSafeExecutorService() {
        final ExecutorService snapshot = executorService; // prevent race conditions between null check and return
        if (snapshot != null) {
            return snapshot;
        } else {
            // use the same error handling as if the reference still pointed to a shutdown executor
            throw new RejectedExecutionException();
        }
    }

    private ScheduledExecutorService getNullSafeSchedulerService() {
        final ScheduledExecutorService snapshot = schedulerService; // prevent race conditions between null check and return
        if (snapshot != null) {
            return snapshot;
        } else {
            // use the same error handling as if the reference still pointed to a shutdown executor
            throw new RejectedExecutionException();
        }
    }

    @Deprecated
    private void logExecutionRejectedAfterShutdown(Object task) {
        log.debug("Ignoring request to execute task of type " + task.getClass()
            + " as the thread pool has been shut down (java.util.concurrent.RejectedExecutionException)");
    }

    private void logExecutionRejectedAfterShutdown(String category) {
        log.debug("Ignoring request to execute task of category '" + category
            + "' as the thread pool has been shut down (java.util.concurrent.RejectedExecutionException)");
    }

    private void initialize() {
        String mainPrefix = DEFAULT_THREAD_NAME_PREFIX;
        if (configuration.getThreadPoolName() != null) {
            mainPrefix = configuration.getThreadPoolName() + "-";
        }
        final ThreadGroup threadGroup = new ThreadGroup(mainPrefix + "ThreadGroup");
        final String threadNamePrefix = mainPrefix + poolIndex.incrementAndGet() + "-";
        threadIndex.set(0);
        currentThreadGroup = threadGroup;
        ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(threadGroup, r, threadNamePrefix + threadIndex.incrementAndGet());
            }
        };
        if (System.getProperty(SYSTEM_PROPERTY_USE_70x_THREAD_POOL_CONFIGURATION) == null) {
            // 7.1.0+ default behavior

            // determine maximum common pool size
            int commonPoolSize = DEFAULT_COMMON_THREAD_POOL_SIZE;
            if (configuration.getThreadPoolSize() > 0) {
                commonPoolSize = configuration.getThreadPoolSize();
            }
            log.debug("Setting maximum thread pool size to " + commonPoolSize);

            // this sets up a bounded thread pool that allows threads to be released after a certain time again; note that as the
            // "core size" is used to achieve this upper bound, there is no minimum number of threads that is kept alive at any time
            executorService = new ThreadPoolExecutor(commonPoolSize, commonPoolSize,
                IDLE_THREAD_RELEASE_TIME_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
            ((ThreadPoolExecutor) executorService).allowCoreThreadTimeOut(true);

            // a separate thread pool for scheduled/repeated tasks
            schedulerService = Executors.newScheduledThreadPool(NUM_THREADS_FOR_SCHEDULED_TASKS, threadFactory);
        } else {
            // 7.0.x compatibility mode
            log.info("Using 7.0.x compatible thread pool configuration");

            executorService = Executors.newCachedThreadPool(threadFactory);
            schedulerService = Executors.newScheduledThreadPool(1, threadFactory);
        }
        statisticsEntriesByCategoryName = Collections.synchronizedMap(new HashMap<>());

        if (configuration.getPeriodicTaskLoggingIntervalMsec() > 0) {
            scheduleAtFixedRate(new Runnable() {

                @Override
                @TaskDescription("Thread pool debug logging")
                public void run() {
                    log.debug("Current combined thread pool size: " + getCurrentThreadCount() + "; Asynchronous tasks:\n"
                        + getFormattedStatistics(false, true));
                }
            }, configuration.getPeriodicTaskLoggingIntervalMsec());
        }
    }

    @Deprecated
    private StatisticsEntry getStatisticsEntry(Class<?> taskClass) {
        // safe as this is a synchronized map, so computeIfAbsent() is atomic
        final String categoryName = categoryNamesForTaskClasses.computeIfAbsent(taskClass, this::determineTaskName);
        return getStatisticsEntry(categoryName);
    }

    private StatisticsEntry getStatisticsEntry(String categoryName) {
        // safe as this is a synchronized map, so computeIfAbsent() is atomic
        return statisticsEntriesByCategoryName.computeIfAbsent(categoryName, StatisticsEntry::new);
    }

    private String determineTaskName(Class<?> taskClass) {
        Method runMethod;
        try {
            runMethod = taskClass.getMethod("run");
        } catch (NoSuchMethodException e) {
            try {
                runMethod = taskClass.getMethod("call");
            } catch (NoSuchMethodException e2) {
                throw new IllegalStateException("Task is neither Runnable nor Callable? " + taskClass.getClass());
            }
        }
        for (Annotation annotation : runMethod.getDeclaredAnnotations()) {
            if (annotation.annotationType() == TaskDescription.class) {
                return ((TaskDescription) annotation).value();
            }
        }
        final String taskClassName = taskClass.getName();
        final boolean isAnonymousNestedTestClass = taskClassName.matches("^.*Test(s)?\\$(.*\\$)?\\d+$");
        if (!isAnonymousNestedTestClass) {
            log.warn("Thread pool task " + taskClassName + " should have a @TaskDescription");
        }
        return "<" + taskClass.getName() + ">";
    }

}
