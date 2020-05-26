/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

/**
 * A callback interface for exceptions in asynchronous tasks.
 * 
 * @author Robert Mischke
 */
public interface AsyncExceptionListener {

    /**
     * Reports an exception that occured in an asynchronous task.
     * 
     * @param e the exception
     */
    void onAsyncException(Exception e);

}
