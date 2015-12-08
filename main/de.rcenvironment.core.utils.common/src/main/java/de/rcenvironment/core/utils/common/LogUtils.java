/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import org.apache.commons.logging.Log;

/**
 * Provides common utilities for logging.
 * 
 * @author Robert Mischke
 */
public final class LogUtils {

    private static final String REMOTE_SERVICE_CALL_EXCEPTION_ID_PREFIX = "E#";

    private static final RestartSafeIncreasingValueGenerator sharedRestartSafeUniqueIdGenerator = new RestartSafeIncreasingValueGenerator();

    private LogUtils() {}

    /**
     * Logs a message-only error and returns a node-unique log marker for it.
     * 
     * @param log the logger instance to log with (so the correct source class is shown)
     * @param logMessage the error message
     * @return the generated log marker/id
     */
    public static String logErrorAndAssignUniqueMarker(Log log, String logMessage) {
        return logExceptionWithStacktraceAndAssignUniqueMarker(log, logMessage, null);
    }

    /**
     * Logs an error with a message and the full stacktrace of an (optional) {@link Throwable} and returns a node-unique log marker for it.
     * 
     * @param log the logger instance to log with (so the correct source class is shown)
     * @param logMessage the error message (mandatory)
     * @param throwable the causing {@link Throwable}; may be null
     * @return the generated log marker/id
     */
    public static String logExceptionWithStacktraceAndAssignUniqueMarker(Log log, String logMessage, Throwable throwable) {
        final String errorId = generateNewErrorId();
        if (throwable != null) {
            log.error(StringUtils.format("%s: %s:", errorId, logMessage), throwable);
        } else {
            log.error(StringUtils.format("%s: %s", errorId, logMessage));
        }
        return errorId;
    }

    /**
     * Logs an error with a message and the toString() form of an optional {@link Throwable} and returns a node-unique log marker for it.
     * 
     * @param log the logger instance to log with (so the correct source class is shown)
     * @param logMessage the error message (mandatory)
     * @param throwable the causing {@link Throwable}; may be null
     * @return the generated log marker/id
     */
    public static String logExceptionAsSingleLineAndAssignUniqueMarker(Log log, String logMessage, Throwable throwable) {
        final String errorId = generateNewErrorId();
        if (throwable != null) {
            log.error(StringUtils.format("%s: %s: %s", errorId, logMessage, throwable.toString()));
        } else {
            log.error(StringUtils.format("%s: %s", errorId, logMessage));
        }
        return errorId;
    }

    private static String generateNewErrorId() {
        return REMOTE_SERVICE_CALL_EXCEPTION_ID_PREFIX + sharedRestartSafeUniqueIdGenerator.invalidateAndGet();
    }

}
