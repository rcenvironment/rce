/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

import org.apache.commons.logging.LogFactory;

/**
 * Utility class to detect the improper use of the main or GUI thread.
 * 
 * @author Robert Mischke
 */
public final class ThreadGuard {

    /**
     * Defines the possible reactions.
     * 
     * @author Robert Mischke
     */
    public enum Policy {
        /**
         * Do nothing.
         */
        DISABLED,
        /**
         * Only log a stack trace, but do not actually throw a {@link RuntimeException}.
         */
        LOG_STACKTRACE,
        /**
         * Throw a {@link RuntimeException}.
         */
        THROW_EXCEPTION
    }

    // the behaviour setting; adapt for debugging and/or release as needed
    private static final Policy BEHAVIOUR_ON_DETECTION = Policy.LOG_STACKTRACE;

    private static volatile Thread forbiddenThread;

    private ThreadGuard() {}

    /**
     * Checks whether the current thread is the one previously set with {@link #setForbiddenThread(Thread)}. If it is, the behaviour is
     * dependent on the "policy" setting of this class; it may do nothing, log a warning, or throw a {@link RuntimeException}. If no thread
     * was set via {@link #setForbiddenThread(Thread)}, this call has no effect.
     */
    public static void checkForForbiddenThread() {
        if (forbiddenThread == null || BEHAVIOUR_ON_DETECTION == Policy.DISABLED) {
            return;
        }
        if (Thread.currentThread() == forbiddenThread) {
            IllegalStateException exception = new IllegalStateException("Operation run in forbidden thread (usually the GUI thread)");
            if (BEHAVIOUR_ON_DETECTION == Policy.THROW_EXCEPTION) {
                throw exception;
            }
            if (BEHAVIOUR_ON_DETECTION == Policy.LOG_STACKTRACE) {
                LogFactory.getLog(ThreadGuard.class).warn("Operation run in forbidden thread, "
                    + "creating a stack trace (no exception was thrown)", exception);
            }
        }
    }

    public static void setForbiddenThread(Thread forbiddenThread) {
        ThreadGuard.forbiddenThread = forbiddenThread;
    }

}
