/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallback;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedCallbackManager;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;

/**
 * Default {@link AsyncOrderedCallbackManager} implementation.
 * 
 * @param <T> the listener class (usually an interface)
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedCallbackManagerImpl<T> implements AsyncOrderedCallbackManager<T> {

    /**
     * Wrapper class to queue callbacks per listener.
     * 
     * @author Robert Mischke
     */
    private final class InternalAsyncOrderedCallbackQueue extends AsyncOrderedExecutionQueueImpl {

        private final T listener;

        private InternalAsyncOrderedCallbackQueue(final T listener, final AsyncCallbackExceptionPolicy exceptionPolicy) {
            super(exceptionPolicy, internalServiceHolder);
            this.listener = listener;
        }

        private void enqueue(final AsyncCallback<T> callback) {
            super.enqueue(new Runnable() {

                @Override
                public void run() {
                    callback.performCallback(listener);
                    // TODO improve fetching
                    internalServiceHolder.getStatisticsTrackerService()
                        .getCounterCategory(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME).count(
                            "Asynchronous callback");
                }
            });
        }
    }

    private final AsyncCallbackExceptionPolicy exceptionPolicy;

    private final Map<T, InternalAsyncOrderedCallbackQueue> listenerMap = new HashMap<T, InternalAsyncOrderedCallbackQueue>();

    private final Log log = LogFactory.getLog(getClass());

    private ConcurrencyUtilsServiceHolder internalServiceHolder;

    public AsyncOrderedCallbackManagerImpl(ConcurrencyUtilsServiceHolder internalServiceHolder,
        AsyncCallbackExceptionPolicy exceptionPolicy) {
        this(exceptionPolicy, internalServiceHolder);
    }

    public AsyncOrderedCallbackManagerImpl(AsyncCallbackExceptionPolicy exceptionPolicy,
        ConcurrencyUtilsServiceHolder internalServiceHolder) {
        this.internalServiceHolder = internalServiceHolder;
        this.exceptionPolicy = exceptionPolicy;
        // explicitly check for known policies
        if (exceptionPolicy != AsyncCallbackExceptionPolicy.LOG_AND_PROCEED
            && exceptionPolicy != AsyncCallbackExceptionPolicy.LOG_AND_CANCEL_LISTENER) {
            log.warn(exceptionPolicy + " policy not implemented yet");
        }
    }

    /**
     * Adds an asynchronous listener.
     * 
     * @param listener the listener to add
     */
    @Override
    public void addListener(T listener) {
        synchronized (listenerMap) {
            // TODO check if already present?
            listenerMap.put(listener, new InternalAsyncOrderedCallbackQueue(listener, exceptionPolicy));
        }
    }

    /**
     * Atomically adds an asynchronous listener and enqueues an asynchronous callback. The given callback is guaranteed to be the first one
     * that the listener receives. This is useful to initialize listeners using the callback method without risk of race conditions.
     * 
     * @param listener the listener to add
     * @param callback the callback to execute asynchronously
     */
    @Override
    public void addListenerAndEnqueueCallback(T listener, AsyncCallback<T> callback) {
        synchronized (listenerMap) {
            InternalAsyncOrderedCallbackQueue singleListenerQueue =
                new InternalAsyncOrderedCallbackQueue(listener, exceptionPolicy);
            listenerMap.put(listener, singleListenerQueue);
            singleListenerQueue.enqueue(callback);
        }
    }

    /**
     * Enqueues a callback to send asynchronously to all current listeners.
     * 
     * @param callback the callback to execute asynchronously
     */
    @Override
    public void enqueueCallback(AsyncCallback<T> callback) {
        // TODO lock contention could probably be reduced; keep it simple for now - misc_ro
        synchronized (listenerMap) {
            for (InternalAsyncOrderedCallbackQueue wrappedListener : listenerMap.values()) {
                wrappedListener.enqueue(callback);
            }
        }
    }

    /**
     * Removes an asynchronous listener.
     * 
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(T listener) {
        // TODO review: cancel already-enqueued callbacks?
        synchronized (listenerMap) {
            listenerMap.remove(listener);
        }
    }

}
