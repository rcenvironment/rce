/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the SharedThreadPool singleton.
 * 
 * @author Robert Mischke
 */
public class SharedThreadPoolTest {

    private static final int CALLABLES_TEST_WAIT_MSEC = 100;

    private static final int NUM_CALLABLES_FOR_GROUP_TEST = 50;

    private final Log log = LogFactory.getLog(getClass());

    private final SharedThreadPool threadPool = SharedThreadPool.getInstance();

    /**
     * Resets the {@link SharedThreadPool} before each test.
     */
    @Before
    public void resetBefore() {
        threadPool.reset();
    }

    /**
     * Resets the {@link SharedThreadPool} after each test.
     */
    @After
    public void resetAfter() {
        log.debug(threadPool.getFormattedStatistics(false));
        threadPool.reset();
    }

    /**
     * getCurrentThreadCount() test.
     * 
     * @throws InterruptedException on interruption
     */
    @Test
    public void testThreadCount() throws InterruptedException {
        assertEquals(0, threadPool.getCurrentThreadCount());
        final CountDownLatch latchIn = new CountDownLatch(1);
        final Semaphore semOut = new Semaphore(0);
        Runnable runnable = new Runnable() {

            @Override
            @TaskDescription("testThreadCount() Runnable")
            public void run() {
                try {
                    latchIn.await();
                    semOut.release();
                } catch (InterruptedException e) {
                    log.warn(e); // only log compact exception on test interruption
                }
            }
        };
        threadPool.execute(runnable);
        assertEquals(1, threadPool.getCurrentThreadCount());
        threadPool.execute(runnable);
        assertEquals(2, threadPool.getCurrentThreadCount());
        // wait for Runnables
        latchIn.countDown();
        semOut.acquire(2);
    }

    /**
     * createCallablesGroup() test.
     */
    @Test
    public void testCallablesGroup() {
        int numTasks = NUM_CALLABLES_FOR_GROUP_TEST;
        CallablesGroup<String> callablesGroup = threadPool.createCallablesGroup(String.class);
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
        assertEquals("Premature start of tasks", 0, threadPool.getCurrentThreadCount());
        List<String> results = callablesGroup.executeParallel(null);
        assertEquals("Incomplete parallelization?", numTasks, threadPool.getCurrentThreadCount());
        assertEquals(numTasks, results.size());
        for (int i = 1; i <= numTasks; i++) {
            assertEquals(results.get(i - 1), Integer.toString(i));
        }
    }

    // TODO add test for exception-throwing Callables

    // TODO add test for task canceling
}
