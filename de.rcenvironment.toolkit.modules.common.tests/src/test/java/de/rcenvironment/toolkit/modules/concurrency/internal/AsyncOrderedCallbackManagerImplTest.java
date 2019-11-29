/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;

/**
 * Unit test for {@link AsyncOrderedCallbackManagerImpl}.
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedCallbackManagerImplTest extends AbstractConcurrencyModuleTest {

    private static final int TEST_TIMEOUT = 10000;

    private static final int CALLBACK_COUNT = 1000;

    private static final int NONE = -1;

    private static final int NUM_LISTENERS = 200;

    private volatile CountDownLatch cdl;

    /**
     * A callback interface for exceptions in asynchronous tasks.
     * 
     * @author Robert Mischke
     */
    public class TestListener {

        private int last = NONE;

        private final Random random = new Random();

        public TestListener(int listenerId) {}

        /**
         * Verifying callback method.
         * 
         * @param i test value
         */
        public void onNewValue(int i) {
            // verify sequence
            if (last == NONE) {
                Assert.assertEquals(0, i);
            } else {
                Assert.assertEquals(last + 1, i);
            }
            last = i;
            try {
                // add some jitter
                Thread.sleep(random.nextInt(5));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (i == CALLBACK_COUNT - 1) {
                cdl.countDown();
            }
        }
    }

    /**
     * Verifies that callbacks are received in-order by multiple listeners.
     * 
     * @throws InterruptedException on test cancellation
     */
    @Test(timeout = TEST_TIMEOUT)
    public void testMultithreadedCallbackBehaviour() throws InterruptedException {
        AsyncOrderedCallbackManager<TestListener> callbackManager =
            getConcurrencyUtilsFactory().createAsyncOrderedCallbackManager(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);
        cdl = new CountDownLatch(NUM_LISTENERS);
        for (int listenerId = 0; listenerId < NUM_LISTENERS; listenerId++) {
            callbackManager.addListener(new TestListener(listenerId));
        }
        for (int i = 0; i < CALLBACK_COUNT; i++) {
            final int i2 = i;
            callbackManager.enqueueCallback(new AsyncCallback<TestListener>() {

                @Override
                public void performCallback(TestListener listener) {
                    listener.onNewValue(i2);
                }
            });
        }
        cdl.await();
        // LogFactory.getLog(getClass()).info(SharedThreadPool.getInstance().getFormattedStatistics());
    }

    // TODO add test to verify that a blocking listener does not stop callbacks to other listeners

    // TODO add test for listener exception handling
}
