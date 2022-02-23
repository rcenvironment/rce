/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.api;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.Callback;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultShutdownRequired;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultConfirmationRequired;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultRecoveryRequired;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultPassed;


/**
 * Creates {@link InstanceValidationResultFactory}s.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert (Validation failure with user confirmation, adaptation to subclasses of InstanceValidationResult)
 */
public final class InstanceValidationResultFactory {
    
    private InstanceValidationResultFactory() {}
    
    /**
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @param logMessage Message to be logged. If running in GUI mode, this message is shown to the user.
     * @return An {@link InstanceValidationResult} that represents the result of a failed validation that requires the instance to shut
     *         down.
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresInstanceShutdown(String validationDisplayName,
        String logMessage) {
        return new InstanceValidationResultShutdownRequired(validationDisplayName, logMessage, logMessage);
    }
    
    /**
     * Creates a result for the case of an failure, which required the instance to shut down.
     * 
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @param logMessage Message to be logged.
     * @param guiDialogMessage Message to be shown to the user if running in GUI mode.
     * @return An {@link InstanceValidationResult} that represents the result of a failed validation that requires the instance to shut
     *         down.
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresInstanceShutdown(String validationDisplayName,
        String logMessage, String guiDialogMessage) {
        return new InstanceValidationResultShutdownRequired(validationDisplayName, logMessage, guiDialogMessage);
    }

    /**
     * Creates a result for the case of an failure, which allows the instance to proceed running. If this instance is running in GUI mode,
     * then the given logMessage is shown to the user.
     * 
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @param logMessage Message to be logged.
     * @return An {@link InstanceValidationResult} that represents the result of a failed validation which still allows continuation of the
     *         startup process.
     */
    public static InstanceValidationResult createResultForFailureWhichAllowsToProceed(String validationDisplayName, String logMessage) {
        return new InstanceValidationResultConfirmationRequired(validationDisplayName, logMessage, logMessage);
    }
    
    /**
     * Creates a result for the case of an failure which allows the instance to continue running after user confirmation.
     * 
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @param logMessage Message that should be logged.
     * @param guiDialogMessage Message to be shown to the user if running in GUI mode.
     * @return An {@link InstanceValidationResultFactory} that represents the result of a failed validation which still allows continuation
     *         of the startup process.
     */
    public static InstanceValidationResult createResultForFailureWhichAllowsToProceed(String validationDisplayName, String logMessage,
        String guiDialogMessage) {
        return new InstanceValidationResultConfirmationRequired(validationDisplayName, logMessage, guiDialogMessage);
    }

    /**
     * Creates a result for the case of passing.
     * 
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @return An {@link InstanceValidationResult} that represents the result of a passed validation.
     */
    public static InstanceValidationResult createResultForPassed(String validationDisplayName) {
        return new InstanceValidationResultPassed(validationDisplayName);
    }

    /**
     * Creates a result for the case of a failure, which is recoverable, but requires explicit confirmation by the user in order to do so.
     * 
     * @param validationDisplayName Display name of the validator which generated the validation result.
     * @param logMessage The message to be logged for this failure.
     * @param guiDialogMessage The message to be delivered to the user in order to query them whether or not to proceed.
     * @param userHint The hint to be shown to the user if user could not be asked for confirmation.
     * @param onProceed The callback to be triggered if the user asks to proceed with the startup. Should recover from the validation
     *        failure.
     * @return An {@link InstanceValidationResult} that represents the result of a validation that failed, but may attempt to recover from
     *         that failure after user confirmation.
     */
    public static InstanceValidationResult createResultForFailureWhichRequiresUserConfirmation(String validationDisplayName,
        String logMessage, String guiDialogMessage, String userHint, Callback onProceed) {
        return new InstanceValidationResultRecoveryRequired(validationDisplayName, logMessage, guiDialogMessage, userHint, onProceed);
    }

}
