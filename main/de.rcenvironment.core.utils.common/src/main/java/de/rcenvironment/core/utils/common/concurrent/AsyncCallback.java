/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

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
