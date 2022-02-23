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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;

/**
 * {@link AsyncOrderedExecutionQueueImpl} unit tests.
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedExecutionQueueImplTest extends AbstractConcurrencyModuleTest {

    private static final int MEDIUM_TEST_TIMEOUT = 10000;

    private static final int SHORT_TEST_TIMEOUT = 1000;

    private final Log log = LogFactory.getLog(getClass());

    private AsyncOrderedExecutionQueue queue;

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
        getThreadPoolManagement().reset();
        queue = getConcurrencyUtilsFactory().createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);
    }

    /**
     * Tests canceling a filled queue.
     * 
     * @throws TimeoutException if canceling hangs unexpectedly
     */
    @Test
    public void testCancel() throws TimeoutException {
        final int jobCount = 10;
        final int jobDuration = 500;

        final AtomicInteger executed = new AtomicInteger();
        for (int i = 0; i < jobCount; i++) {
            queue.enqueue(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(jobDuration);
                        executed.incrementAndGet();
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            });
        }
        try {
            Thread.sleep(jobDuration / 2);
        } catch (InterruptedException e) {
            Assert.fail(e.toString());
        }
        assertEquals(0, executed.get());
        queue.cancelAndWaitForLastRunningTask();
        assertEquals(1, executed.get());
    }

    /**
     * Tests that the cancel operation does not hang if the queue is already empty.
     * 
     * @throws TimeoutException should not happen, as the test timeout is shorter
     */
    @Test(timeout = SHORT_TEST_TIMEOUT)
    public void testCancelOnEmptyQueueDoesNotBlock() throws TimeoutException {
        queue.cancelAndWaitForLastRunningTask();
    }

    /**
     * Tests canceling a filled queue.
     * 
     * @throws TimeoutException if canceling hangs unexpectedly
     */
    @Test
    public void testCancelFromInsideTaskExecution() throws TimeoutException {
        final int totalJobCount = 10;
        final int iterationToCancelIn = 5; // one-based index
        final int waitForCompletionTime = 200;

        final AtomicInteger executed = new AtomicInteger();
        for (int i = 1; i <= totalJobCount; i++) {
            final int i2 = i;
            queue.enqueue(new Runnable() {

                @Override
                public void run() {
                    executed.incrementAndGet();
                    if (i2 == iterationToCancelIn) {
                        queue.enqueue(new Runnable() {

                            @Override
                            public void run() {
                                // crude but functional way to trigger test failure
                                executed.addAndGet(totalJobCount);
                            }
                        });
                        queue.cancelAsync();
                        queue.enqueue(new Runnable() {

                            @Override
                            public void run() {
                                // crude but functional way to trigger test failure
                                executed.addAndGet(totalJobCount);
                            }
                        });
                    }
                }
            });
        }
        try {
            Thread.sleep(waitForCompletionTime);
        } catch (InterruptedException e) {
            Assert.fail(e.toString());
        }
        // test expectation: no other task was executed after the one that cancel() was called from
        assertEquals(iterationToCancelIn, executed.get());
    }

    /**
     * Verifies that the queue has sufficient mass throughput.
     * 
     * @throws InterruptedException on test interruption
     */
    @Test
    public void massThroughput() throws InterruptedException {
        final int count = 1 * 1000 * 1000;
        final CountDownLatch cdl = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            queue.enqueue(new Runnable() {

                @Override
                public void run() {
                    cdl.countDown();
                }
            });
        }
        assertTrue(cdl.await(MEDIUM_TEST_TIMEOUT, TimeUnit.MILLISECONDS));
    }

}
