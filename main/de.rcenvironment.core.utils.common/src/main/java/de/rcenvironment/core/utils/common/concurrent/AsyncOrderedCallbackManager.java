/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StatsCounter;

/**
 * Utility class to manage asynchronous callback listeners and callback execution so that each listener receives all callbacks in the order
 * they were enqueued in.
 * 
 * @param <T> the listener class (usually an interface)
 * 
 * @author Robert Mischke
 */
public class AsyncOrderedCallbackManager<T> {

    /**
     * Wrapper class to queue callbacks per listener.
     * 
     * @author Robert Mischke
     */
    private final class AsyncOrderedCallbackQueue extends AsyncOrderedExecutionQueue {

        private final T listener;

        private AsyncOrderedCallbackQueue(final T listener, final AsyncCallbackExceptionPolicy exceptionPolicy,
            final ThreadPool threadPool) {
            super(exceptionPolicy, threadPool);
            this.listener = listener;
        }

        private void enqueue(final AsyncCallback<T> callback) {
            super.enqueue(new Runnable() {

                @Override
                public void run() {
                    callback.performCallback(listener);
                    StatsCounter.count(AsyncOrderedExecutionQueue.STATS_COUNTER_SHARED_CATEGORY_NAME, "Asynchronous callback");
                }
            });
        }
    }

    private final ThreadPool threadPool;

    private final AsyncCallbackExceptionPolicy exceptionPolicy;

    private final Map<T, AsyncOrderedCallbackQueue> listenerMap = new HashMap<T, AsyncOrderedCallbackQueue>();

    private final Log log = LogFactory.getLog(getClass());

    public AsyncOrderedCallbackManager(ThreadPool threadPool, AsyncCallbackExceptionPolicy exceptionPolicy) {
        this.threadPool = threadPool;
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
    public void addListener(T listener) {
        synchronized (listenerMap) {
            // TODO check if already present?
            listenerMap.put(listener, new AsyncOrderedCallbackQueue(listener, exceptionPolicy, threadPool));
        }
    }

    /**
     * Atomically adds an asynchronous listener and enqueues an asynchronous callback. The given callback is guaranteed to be the first one
     * that the listener receives. This is useful to initialize listeners using the callback method without risk of race conditions.
     * 
     * @param listener the listener to add
     * @param callback the callback to execute asynchronously
     */
    public void addListenerAndEnqueueCallback(T listener, AsyncCallback<T> callback) {
        synchronized (listenerMap) {
            AsyncOrderedCallbackQueue singleListenerQueue = new AsyncOrderedCallbackQueue(listener, exceptionPolicy, threadPool);
            listenerMap.put(listener, singleListenerQueue);
            singleListenerQueue.enqueue(callback);
        }
    }

    /**
     * Enqueues a callback to send asynchronously to all current listeners.
     * 
     * @param callback the callback to execute asynchronously
     */
    public void enqueueCallback(AsyncCallback<T> callback) {
        // TODO lock contention could probably be reduced; keep it simple for now - misc_ro
        synchronized (listenerMap) {
            for (AsyncOrderedCallbackQueue wrappedListener : listenerMap.values()) {
                wrappedListener.enqueue(callback);
            }
        }
    }

    /**
     * Removes an asynchronous listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(T listener) {
        // TODO review: cancel already-enqueued callbacks?
        synchronized (listenerMap) {
            listenerMap.remove(listener);
        }
    }

}
