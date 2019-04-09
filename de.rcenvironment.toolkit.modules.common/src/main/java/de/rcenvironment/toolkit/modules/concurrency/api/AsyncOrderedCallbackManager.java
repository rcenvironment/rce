/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

/**
 * A concurrency mechanism to manage asynchronous callback listeners and callback execution so that each listener receives all callbacks in
 * the order they were enqueued in.
 * 
 * @param <T> the listener class (usually an interface)
 * 
 * @author Robert Mischke
 */
public interface AsyncOrderedCallbackManager<T> {

    /**
     * Adds an asynchronous listener.
     * 
     * @param listener the listener to add
     */
    void addListener(T listener);

    /**
     * Atomically adds an asynchronous listener and enqueues an asynchronous callback. The given callback is guaranteed to be the first one
     * that the listener receives. This is useful to initialize listeners using the callback method without risk of race conditions.
     * 
     * @param listener the listener to add
     * @param callback the callback to execute asynchronously
     */
    void addListenerAndEnqueueCallback(T listener, AsyncCallback<T> callback);

    /**
     * Enqueues a callback to send asynchronously to all current listeners.
     * 
     * @param callback the callback to execute asynchronously
     */
    void enqueueCallback(AsyncCallback<T> callback);

    /**
     * Removes an asynchronous listener.
     * 
     * @param listener the listener to remove
     */
    void removeListener(T listener);

    /**
     * @return the current number of registered listeners
     */
    int getListenerCount();

}
