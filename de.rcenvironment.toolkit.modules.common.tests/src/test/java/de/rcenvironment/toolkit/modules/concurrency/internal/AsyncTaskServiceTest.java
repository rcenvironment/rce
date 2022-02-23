/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncTaskService;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;
import de.rcenvironment.toolkit.modules.concurrency.api.ThreadPoolManagementAccess;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContext;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextBuilder;
import de.rcenvironment.toolkit.modules.concurrency.api.threadcontext.ThreadContextHolder;

/**
 * Tests for the shared thread pool represented by {@link AsyncTaskService}.
 * 
 * @author Robert Mischke
 */
public class AsyncTaskServiceTest extends AbstractConcurrencyModuleTest {

    private static final int CALLABLES_TEST_WAIT_MSEC = 100;

    private static final int NUM_CALLABLES_FOR_GROUP_TEST = 50;

    private AsyncTaskService threadPool;

    private ThreadPoolManagementAccess threadPoolManagement;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Resets the {@link AsyncTaskServiceImpl} before each test.
     */
    @Before
    public void resetBefore() {
        threadPool = getAsyncTaskService();
        threadPoolManagement = getThreadPoolManagement();
        threadPoolManagement.reset();
    }

    /**
     * Resets the {@link AsyncTaskServiceImpl} after each test.
     */
    @After
    public void resetAfter() {
        log.debug(threadPoolManagement.getFormattedStatistics(false));
        threadPoolManagement.reset();
    }

    /**
     * getCurrentThreadCount() test.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void threadCount() throws InterruptedException {
        assertEquals(0, threadPoolManagement.getCurrentThreadCount());
        final CountDownLatch latchIn = new CountDownLatch(1);
        final Semaphore semOut = new Semaphore(0);
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    latchIn.await();
                    semOut.release();
                } catch (InterruptedException e) {
                    log.warn(e); // only log compact exception on test interruption
                }
            }
        };
        threadPool.execute("testThreadCount() Runnable", runnable);
        assertEquals(1, threadPoolManagement.getCurrentThreadCount());
        threadPool.execute("testThreadCount() Runnable", runnable);
        assertEquals(2, threadPoolManagement.getCurrentThreadCount());
        // wait for Runnables
        latchIn.countDown();
        semOut.acquire(2);
    }

    /**
     * createCallablesGroup() test.
     */
    @Test
    public void callablesGroup() {
        int numTasks = NUM_CALLABLES_FOR_GROUP_TEST;
        CallablesGroup<String> callablesGroup = getConcurrencyUtilsFactory().createCallablesGroup(String.class);
        final Random random = new Random();
        for (int i = 1; i <= numTasks; i++) {
            final String result = Integer.toString(i);
            callablesGroup.add(new Callable<String>() {

                @Override
                @TaskDescription("testCallablesGroup() Callable")
                public String call() throws Exception {
                    // minimum wait prevents thread reuse; causes n threads to be spawned
                    // random wait causes tasks to finish out of sequence
                    Thread.sleep(CALLABLES_TEST_WAIT_MSEC + random.nextInt(CALLABLES_TEST_WAIT_MSEC));
                    return result;
                }

            });
        }
        assertEquals("Premature start of tasks", 0, threadPoolManagement.getCurrentThreadCount());
        List<String> results = callablesGroup.executeParallel(null);
        assertEquals("Incomplete parallelization?", numTasks, threadPoolManagement.getCurrentThreadCount());
        assertEquals(numTasks, results.size());
        for (int i = 1; i <= numTasks; i++) {
            assertEquals(results.get(i - 1), Integer.toString(i));
        }
    }

    // TODO add test for exception-throwing Callables

    // TODO add test for uncaught exceptions from Runnables and Callables

    // TODO add test for task canceling

    /**
     * Verifies that there is sufficient task throughput (and therefore, no unusual congestion) in the common thread pool.
     * 
     * @throws InterruptedException on test interruption
     */
    @Test
    public void commonPoolTaskThroughput() throws InterruptedException {
        final int taskCount = 10000; // must be significantly greater than the max thread pool size
        final int waitTimeForCompletion = 5000;

        final CountDownLatch counter = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            threadPool.execute("Test task", () -> {

                sleep(10);
                counter.countDown();
            });
        }
        assertTrue(counter.await(waitTimeForCompletion, TimeUnit.MILLISECONDS));

        final int waitTimeBeforeShutdown = 500;
        sleep(waitTimeBeforeShutdown); // prevent irrelevant shutdown warnings
    }

    /**
     * Verifies that scheduled tasks are actually executed concurrently, ie in more than one thread.
     */
    @Test
    public void scheduledTasksRunConcurrently() {
        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean success = new AtomicBoolean();
        // test time scale; adjust if test fails on slow machines
        final int waitTimeSlice = 300;
        threadPool.scheduleAfterDelay(new Runnable() {

            @Override
            public void run() {
                log.debug("Running in thread "
                    + Thread.currentThread().getName());
                counter.incrementAndGet();
                sleep(2 * waitTimeSlice);
                if (counter.get() == 2) {
                    // only reached if the second task ran in parallel
                    success.set(true);
                }
            }
        }, 0);
        threadPool.scheduleAfterDelay(new Runnable() {

            @Override
            public void run() {
                log.debug("Running in thread "
                    + Thread.currentThread().getName());
                sleep(1 * waitTimeSlice);
                counter.incrementAndGet();
            }
        }, 0);
        sleep(3 * waitTimeSlice);
        assertEquals(Boolean.TRUE, success.get());
    }

    /**
     * Verifies that the calling thread's {@link ThreadContext} is transfered to the spawned tasks' threads.
     * 
     * @throws Exception none expected
     */
    @Test
    public void threadContextTransfer() throws Exception {
        final String testValue1 = "test1";
        final ThreadContext initialContext = ThreadContextBuilder.empty().setAspect(String.class, testValue1).build();

        ThreadContextHolder.setCurrentContext(initialContext);

        verifyContextStateInsideTasks(initialContext, testValue1);

        // intentionally testing null context after a custom context was already set
        ThreadContextHolder.setCurrentContext(null);
        verifyContextStateInsideTasks(null, null);
    }

    private void verifyContextStateInsideTasks(final ThreadContext expectedContext, final String expectedValue)
        throws InterruptedException, ExecutionException, TimeoutException {

        // test Runnable
        final CountDownLatch test1CDL = new CountDownLatch(1);
        threadPool.execute("Runnable context test", () -> {
            ThreadContextHolder.getCurrentContext();
            ThreadContextHolder.getCurrentContextAspect(String.class);
            test1CDL.countDown();
        });

        test1CDL.await(1, TimeUnit.SECONDS);

        // test Callable
        final Future<String> test2Future = threadPool.submit(new Callable<String>() {

            @Override
            @TaskDescription("Callable context test")
            public String call() throws Exception {
                assertEquals(expectedContext, ThreadContextHolder.getCurrentContext());
                return ThreadContextHolder.getCurrentContextAspect(String.class);
            }
        });
        test2Future.get(1, TimeUnit.SECONDS);
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
