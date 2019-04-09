/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.api;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.Callback;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultImpl;


/**
 * Creates {@link InstanceValidationResultFactory}s.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert (Validation failure with user confirmation)
 */
public final class InstanceValidationResultFactory {
    
    private InstanceValidationResultFactory() {}
    
    /**
     * Creates a result for the case of an failure, which required the instance to shut down.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @param logMessage message, which should be logged
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresInstanceShutdown(String validationDisplayName,
        String logMessage) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED, 
            logMessage, logMessage, null);
    }
    
    /**
     * Creates a result for the case of an failure, which required the instance to shut down.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @param logMessage message, which should be logged
     * @param guiDialogMessage message, which should be shown when running in GUI mode
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresInstanceShutdown(String validationDisplayName,
        String logMessage, String guiDialogMessage) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED, 
            logMessage, guiDialogMessage, null);
    }

    /**
     * Creates a result for the case of an failure, which allows the instance to proceed running.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @param logMessage message, which should be logged
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichAllowesToProceed(String validationDisplayName, String logMessage) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED, 
            logMessage, logMessage, null);
    }
    
    /**
     * Creates a result for the case of an failure, which allows the instance to proceed running.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @param logMessage message, which should be logged
     * @param guiDialogMessage message, which should be shown when running in GUI mode
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichAllowesToProceed(String validationDisplayName, String logMessage,
        String guiDialogMessage) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_CONFIRMATION_REQUIRED, 
            logMessage, guiDialogMessage, null);
    }

    /**
     * Creates a result for the case of passing.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForPassed(String validationDisplayName) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.PASSED, "", "", null);
    }

    /**
     * Creates a result for the case of a failure, which is recoverable, but requires explicit confirmation by the user in order to do so.
     * 
     * @param validationDisplayName Display name of the validator which generates the validation result.
     * @param logMessage The message to be logged for this failure
     * @param guiDialogMessage The message to be delivered to the user in order to query them whether or not to proceed
     * @param onProceed The callback to be triggered if the user asks to proceed with the startup. Should recover from the validation
     *        failure.
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresUserConfirmation(String validationDisplayName,
        String logMessage, String guiDialogMessage, Callback onProceed) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_RECOVERY_REQUIRED,
            logMessage, guiDialogMessage, onProceed);
    }

}
