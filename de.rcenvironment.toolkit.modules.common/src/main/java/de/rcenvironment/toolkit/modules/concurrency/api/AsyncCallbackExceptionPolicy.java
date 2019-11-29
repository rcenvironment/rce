/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

/**
 * Defines the behaviour when a callback target throws a {@link RuntimeException}.
 * 
 * @author Robert Mischke
 */
public enum AsyncCallbackExceptionPolicy {
    /**
     * Log the exception, but continue with the next callback.
     */
    LOG_AND_PROCEED,
    /**
     * Log the exception, unregister the listener and discard all queued callbacks.
     */
    LOG_AND_CANCEL_LISTENER
}
