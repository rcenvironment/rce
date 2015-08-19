/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link AsyncOrderedExecutionQueue} unit tests.
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedExecutionQueueTest {

    private static final int SHORT_TEST_TIMEOUT = 1000;

    private final Log log = LogFactory.getLog(getClass());

    private AsyncOrderedExecutionQueue queue;

    /**
     * Common test setup.
     */
    @Before
    public void setUp() {
        SharedThreadPool.getInstance().reset();
        queue = new AsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED, SharedThreadPool.getInstance());
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
        queue.cancel(true);
        assertEquals(1, executed.get());
    }

    /**
     * Tests that the cancel operation does not hang if the queue is already empty.
     * 
     * @throws TimeoutException should not happen, as the test timeout is shorter
     */
    @Test(timeout = SHORT_TEST_TIMEOUT)
    public void testCancelOnEmptyQueueDoesNotBlock() throws TimeoutException {
        queue.cancel(true);
    }

}
