/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import org.apache.commons.logging.Log;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility methods for component execution.
 * 
 * @author Doreen Seider
 */
public final class ComponentExecutionUtils {
    
    protected static final int WAIT_UNIL_RETRY_MSEC = 10000;

    protected static final int MAX_RETRIES = 5;
    
    // non final for test purposes
    protected static int waitUntilRetryMsec = WAIT_UNIL_RETRY_MSEC;

    private static final int THOUSAND = 1000;
    
    private ComponentExecutionUtils() {}
    
    protected static void logCallbackSuccessAfterFailure(Log log, String logMessage, int failureCount) {
        if (failureCount > 0) {
            log.debug(StringUtils.format(logMessage + " succeeded after %d retries", failureCount));
        }
    }
    
    protected static void waitForRetryAfterCallbackFailure(Log log, int failureCount, String logMessage, String cause) {
        int waitInterval = waitUntilRetryMsec * failureCount;
        String message = StringUtils.format(logMessage + ", retrying in %ds", waitInterval / THOUSAND);
        log.warn(StringUtils.format("%s; failure count is %d (threshold: %d); cause: %s",
            message, failureCount, MAX_RETRIES, cause));
        try {
            Thread.sleep(waitInterval);
        } catch (InterruptedException e1) {
            log.error(StringUtils.format("Waiting for retry (%s) was interrupted: %s", logMessage, e1.toString()));
        }
    }
    
    protected static void logCallbackFailureAfterRetriesExceeded(Log log, String logMessage, Exception e) {
        log.error(logMessage + "; maximum number of failures (" + MAX_RETRIES
            + ") exceeded; last cause: " + e.toString());
    }

}
