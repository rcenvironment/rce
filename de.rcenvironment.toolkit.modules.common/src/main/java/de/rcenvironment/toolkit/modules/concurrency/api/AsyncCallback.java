/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

/**
 * An asynchronous callback performer that is passed the actual callback receiver/listener at
 * runtime.
 * 
 * @param <T> the actual callback type (usually an interface)
 * 
 * @author Robert Mischke
 */
public interface AsyncCallback<T> {

    /**
     * Performs the callback operation on the given listener.
     * 
     * @param listener the callback receiver/listener
     */
    void performCallback(T listener);
}
