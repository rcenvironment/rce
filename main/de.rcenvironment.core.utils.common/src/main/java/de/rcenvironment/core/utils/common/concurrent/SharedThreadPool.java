/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A utility wrapper class that provides a shared ExecutorService using the {@link ThreadPool} interface. Its main purpose is to avoid the
 * redundant creation of thread pools for each service, which would needlessly increase the global thread count. An additional benefit is
 * the ability to reset the thread pools of the communication layer, which is useful to ensure test isolation, and to measure maximum thread
 * usage.
 * 
 * Thread monitoring and debugging is also simplified by providing recognizable thread names.
 * 
 * In addition, this method collects execution statistics about the processed {@link Callable}s and {@link Runnable}s.
 * 
 * @author Robert Mischke
 */
public final class SharedThreadPool implements ThreadPool {

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

        private final Class<?> taskClass;

        private final String taskName;

        public StatisticsEntry(Class<?> taskClass) {
            this.taskClass = taskClass;
            this.taskName = determineTaskName();
        }

        public String getTaskName() {
            return taskName;
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
                        taskName, replaced.getName(), Thread.currentThread().getName()), new RuntimeException());
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
                        taskName));
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
                sb.append(" msec, MaxTime: ");
                sb.append(maxNormalCompletionTime / NANOS_TO_MSEC_RATIO);
                sb.append(" msec");
            }
            if (exceptionCount > 0) {
                sb.append(", Exceptions: ");
                sb.append(exceptionCount);
            }
        }

        private String determineTaskName() {
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
            log.warn("Thread pool task " + taskClass.getName() + " should have a @TaskDescription");
            return "<" + taskClass.getName() + ">";
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

        public WrappedCallable(Callable<T> callable, String taskId) {
            this.innerCallable = callable;
            this.taskId = taskId;
        }

        @Override
        public T call() throws Exception {
            StatisticsEntry statisticsEntry = getStatisticsEntry(innerCallable.getClass());
            statisticsEntry.beforeExecution(taskId);
            T result;
            long startTime = System.nanoTime();
            boolean exception = false;
            try {
                try {
                    result = innerCallable.call();
                } catch (RuntimeException e) {
                    log.warn("Unhandled exception in Callable for task " + statisticsEntry.getTaskName(), e);
                    exception = true;
                    throw e;
                }
            } finally {
                long duration = System.nanoTime() - startTime;
                statisticsEntry.afterExecution(taskId, duration, exception);
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

        public WrappedRunnable(Runnable runnable, String taskId) {
            this.innerRunnable = runnable;
            this.taskId = taskId;
        }

        @Override
        public void run() {
            StatisticsEntry statisticsEntry = getStatisticsEntry(innerRunnable.getClass());
            statisticsEntry.beforeExecution(taskId);
            long startTime = System.nanoTime();
            boolean exception = false;
            try {
                try {
                    innerRunnable.run();
                } catch (RuntimeException e) {
                    log.warn("Unhandled exception in Runnable for task " + statisticsEntry.getTaskName(), e);
                    exception = true;
                }
            } finally {
                long duration = System.nanoTime() - startTime;
                statisticsEntry.afterExecution(taskId, duration, exception);
            }
        }
    }

    /**
     * Default implementation of {@link RunnablesGroup}. This current implementation simply delegates to {@link CallablesGroupImpl}.
     * 
     * @author Robert Mischke
     */
    private final class RunnablesGroupImpl extends CallablesGroupImpl<RuntimeException> implements RunnablesGroup {

        @Override
        public void add(final Runnable task) {
            add(new Callable<RuntimeException>() {

                @Override
                public RuntimeException call() throws Exception {
                    // CHECKSTYLE:DISABLE (IllegalCatch) - Throwables should not slip through, e.g. for unit assertion tests
                    try {
                        task.run();
                        return null;
                    } catch (Throwable e) {
                        return wrapIfNecessary(e);
                    }
                    // CHECKSTYLE:ENABLE (IllegalCatch)
                }

                private RuntimeException wrapIfNecessary(Throwable e) {
                    if (e instanceof RuntimeException) {
                        log.debug("Caught asynchronous exception", e);
                        return (RuntimeException) e;
                    } else {
                        log.error("Non-RTE throwable caught:", e);
                        return new RuntimeException(e);
                    }
                }
            });
        }

        @Override
        public List<RuntimeException> executeParallel() {
            return super.executeParallel(new AsyncExceptionListener() {

                @Override
                public void onAsyncException(Exception e) {
                    log.error("Uncaught exception in RunnablesGroup", e);
                }
            });
        }
    }

    /**
     * Default implementation of {@link CallablesGroup}.
     * 
     * @author Robert Mischke
     */
    private class CallablesGroupImpl<T> implements CallablesGroup<T> {

        private List<Callable<T>> tasks = new ArrayList<Callable<T>>();

        private Map<Callable<T>, String> taskIds = new HashMap<>();

        @Override
        public void add(Callable<T> task) {
            tasks.add(task);
        }

        @Override
        public void add(Callable<T> task, String taskId) {
            add(task);
            String previousValue = taskIds.put(task, taskId);
            if (previousValue != null) {
                log.warn("Add the same task instance again, but with a different task id; the new id (" + taskId
                    + ") takes precedence over the old id (" + previousValue + ")");
            }
        }

        @Override
        public List<T> executeParallel(AsyncExceptionListener exceptionListener) {
            // this should usually not be called from the GUI thread
            ThreadGuard.checkForForbiddenThread();
            List<Future<T>> futures = new ArrayList<Future<T>>();
            for (Callable<T> task : tasks) {
                futures.add(submit(task, taskIds.get(task)));
            }
            List<T> results = new ArrayList<T>();
            // note: this approach matches the order of results to the order of added tasks
            for (Future<T> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException e) {
                    results.add(null);
                    if (exceptionListener != null) {
                        exceptionListener.onAsyncException(e);
                    }
                } catch (ExecutionException e) {
                    results.add(null);
                    if (exceptionListener != null) {
                        exceptionListener.onAsyncException(e);
                    }
                }
            }
            return results;
        }
    }

    private static final String THREAD_NAME_PREFIX = "SharedThreadPool-";

    private static final SharedThreadPool SHARED_INSTANCE = new SharedThreadPool();

    private volatile ExecutorService executorService;

    private AtomicInteger poolIndex = new AtomicInteger(0);

    private AtomicInteger threadIndex = new AtomicInteger(0);

    private ThreadGroup currentThreadGroup;

    private Map<Class<?>, StatisticsEntry> statisticsMap;

    private ScheduledExecutorService schedulerService;

    private final Log log = LogFactory.getLog(getClass());

    private SharedThreadPool() {
        initialize();
    }

    public static SharedThreadPool getInstance() {
        return SHARED_INSTANCE;
    }

    @Override
    public void execute(Runnable task) {
        execute(task, null);
    }

    @Override
    public void execute(Runnable task, String taskId) {
        try {
            getNullSafeExecutorService().execute(new WrappedRunnable(task, taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(task);
            throw e;
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public Future<?> submit(Runnable task, String taskId) {
        try {
            return getNullSafeExecutorService().submit(new WrappedRunnable(task, taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(task);
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return submit(task, null);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task, String taskId) {
        try {
            return getNullSafeExecutorService().submit(new WrappedCallable<T>(task, taskId));
        } catch (RejectedExecutionException e) {
            logExecutionRejectedAfterShutdown(task);
            throw e;
        }
    }

    @Override
    public <T> CallablesGroup<T> createCallablesGroup(Class<T> clazz) {
        return new CallablesGroupImpl<T>();
    }

    @Override
    public RunnablesGroup createRunnablesGroup() {
        return new RunnablesGroupImpl();
    }

    @Override
    public ScheduledFuture<?> scheduleAfterDelay(Runnable runnable, long delayMsec) {
        return schedulerService.schedule(new WrappedRunnable(runnable, null), delayMsec,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long repetitionDelayMsec) {
        return schedulerService.scheduleAtFixedRate(new WrappedRunnable(runnable, null), repetitionDelayMsec, repetitionDelayMsec,
            TimeUnit.MILLISECONDS);
    }

    /**
     * Intended for unit tests; shuts down the internal executor and replaces it with a new one. This should terminate all well-behaved
     * threads and tasks (ie, those that properly react to interruption).
     * 
     * Note that no synchronization is performed when replacing the internal executor; it is up to the caller to ensure proper thread
     * visibility.
     * 
     * @return the number of enqueued tasks that were never started; should usually be zero
     */
    public int reset() {
        List<Runnable> queued = executorService.shutdownNow();
        executorService = null;
        schedulerService.shutdown();
        schedulerService = null;
        initialize();
        return queued.size();
    }

    /**
     * @return the approximate thread count of the current pool
     * 
     * @see {@link ThreadGroup#activeCount()}.
     */
    public int getCurrentThreadCount() {
        return currentThreadGroup.activeCount();
    }

    /**
     * Returns a human-readable String representation of the collected statistics.
     * 
     * @param addTaskIds true if the ids of tasks that provide them should be included
     * 
     * @return a String representation of the collected statistics
     */
    public String getFormattedStatistics(boolean addTaskIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("Asynchronous tasks:\n");
        Map<String, StatisticsEntry> sortedMap = new TreeMap<String, StatisticsEntry>();
        synchronized (statisticsMap) {
            // TODO change to values()? - misc_ro
            for (Entry<Class<?>, StatisticsEntry> entry : statisticsMap.entrySet()) {
                StatisticsEntry statisticsEntry = entry.getValue();
                sortedMap.put(statisticsEntry.getTaskName(), statisticsEntry);
            }
        }
        for (Entry<String, StatisticsEntry> entry : sortedMap.entrySet()) {
            String taskName = entry.getKey();
            StatisticsEntry statsEntry = entry.getValue();
            synchronized (statsEntry) {
                sb.append("  ");
                sb.append(taskName);
                sb.append("\n      ");
                statsEntry.printFormatted(sb);
                sb.append("\n");
                if (addTaskIds) {
                    if (statsEntry.activeTaskIds != null && !statsEntry.activeTaskIds.isEmpty()) {
                        sb.append("      Named tasks:\n");
                        for (Entry<String, Thread> taskIdEntry : statsEntry.activeTaskIds.entrySet()) {
                            sb.append(StringUtils.format("          %s [%s]\n", taskIdEntry.getKey(), taskIdEntry.getValue().getName()));
                        }
                    }
                    if (statsEntry.anonymousTaskThreads != null && !statsEntry.anonymousTaskThreads.isEmpty()) {
                        sb.append("      Anonymous task threads:\n");
                        for (Thread thread : statsEntry.anonymousTaskThreads) {
                            sb.append(StringUtils.format("          [%s]\n", thread.getName()));
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private ExecutorService getNullSafeExecutorService() {
        if (executorService != null) {
            return executorService;
        } else {
            // use the same error handling as if the reference still pointed to a shutdown executor
            throw new RejectedExecutionException();
        }
    }

    private void logExecutionRejectedAfterShutdown(Object task) {
        log.warn("Ignoring request to execute task of type " + task.getClass()
            + " as the thread pool has been shut down (java.util.concurrent.RejectedExecutionException)");
    }

    private void initialize() {
        final ThreadGroup threadGroup = new ThreadGroup(THREAD_NAME_PREFIX + "ThreadGroup");
        final String threadNamePrefix = THREAD_NAME_PREFIX + poolIndex.incrementAndGet() + "-";
        threadIndex.set(0);
        currentThreadGroup = threadGroup;
        ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(threadGroup, r, threadNamePrefix + threadIndex.incrementAndGet());
            }
        };
        executorService = Executors.newCachedThreadPool(threadFactory);
        schedulerService = Executors.newScheduledThreadPool(1, threadFactory);
        statisticsMap = Collections.synchronizedMap(new HashMap<Class<?>, StatisticsEntry>());
    }

    private StatisticsEntry getStatisticsEntry(Class<?> r) {
        // TODO >4.0.0: use ThreadsafeAutoCreationMap; not changing as release is imminent - misc_ro
        StatisticsEntry statisticsEntry = statisticsMap.get(r);
        if (statisticsEntry == null) {
            // NOTE: while this looks similar to the double-checked locking anti-pattern,
            // it should be safe as statisticsMap is a synchronizedMap; the synchronized block only
            // serves to prevent race conditions <b>between</b> the already-synchronized calls
            synchronized (statisticsMap) {
                statisticsEntry = statisticsMap.get(r);
                statisticsEntry = createEntryIfNotPresent(r, statisticsEntry);
            }
        }
        return statisticsEntry;
    }

    /**
     * A workaround method to circumvent the (well-intentioned) CheckStyle double-checked locking prevention.
     */
    private StatisticsEntry createEntryIfNotPresent(Class<?> r, StatisticsEntry statisticsEntry) {
        if (statisticsEntry == null) {
            statisticsEntry = new StatisticsEntry(r);
            statisticsMap.put(r, statisticsEntry);
        }
        return statisticsEntry;
    }

}
