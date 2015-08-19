/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.concurrent;

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
