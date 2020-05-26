/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.internal;

import org.apache.commons.logging.Log;

import de.rcenvironment.core.component.api.ComponentConstants;
import de.rcenvironment.core.component.execution.api.ComponentExecutionContext;
import de.rcenvironment.core.component.model.configuration.api.ConfigurationDescription;
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
    
    protected static boolean isManualOutputVerificationRequired(ConfigurationDescription configDesc) {
        return Boolean
            .valueOf(configDesc.getComponentConfigurationDefinition().getReadOnlyConfiguration()
                .getValue(ComponentConstants.COMPONENT_CONFIG_KEY_REQUIRES_OUTPUT_APPROVAL))
            && !Boolean.valueOf(configDesc.getConfigurationValue(ComponentConstants.COMPONENT_CONFIG_KEY_IS_MOCK_MODE));
    }
    
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
    
    protected static String getStringWithInfoAboutComponentAndWorkflowUpperCase(ComponentExecutionContext compExeCtx) {
        return StringUtils.format("Component '%s' (%s) of workflow '%s' (%s)",
            compExeCtx.getInstanceName(),
            compExeCtx.getExecutionIdentifier(),
            compExeCtx.getWorkflowInstanceName(),
            compExeCtx.getWorkflowExecutionIdentifier());
    }
    
    protected static String getStringWithInfoAboutComponentAndWorkflowLowerCase(ComponentExecutionContext compExeCtx) {
        return StringUtils.format("component '%s' (%s) of workflow '%s' (%s)",
            compExeCtx.getInstanceName(),
            compExeCtx.getExecutionIdentifier(),
            compExeCtx.getWorkflowInstanceName(),
            compExeCtx.getWorkflowExecutionIdentifier());
    }

}
