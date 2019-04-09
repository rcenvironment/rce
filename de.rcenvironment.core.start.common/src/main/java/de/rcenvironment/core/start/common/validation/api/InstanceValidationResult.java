/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.api;

import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Result instance startup validation performed by {@link InstanceValidator}s.
 * 
 * @author Doreen Seider
 */
public interface InstanceValidationResult {
    
    /**
     * We signal an irrecoverable error during recovery from an instance validation failure by throwing an exception. However, since
     * throwing the base class {@link Exception} leads to a host of checkstyle issues, we declare our own "dummy" exception that may be
     * thrown during recovery from instance validation failures.
     * 
     * @author Alexander Weinert
     */
    class CallbackException extends Exception {
        private static final long serialVersionUID = -4967186364314366899L;

        public CallbackException(String errorMessage, Throwable t) {
            super(errorMessage, t);
        }
    }

    /**
     * If user confirmation is required in order to proceed with the startup process, some action might be necessary in order to recover
     * from the validation failure. As this action may itself encounter an error, e.g., due to a failed disk or network operation, it may
     * throw an exception. Hence, we cannot use the built-in Function interface, but have to declare our own.
     * 
     * @author Alexander Weinert
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * This method is called if the user asks to proceed with startup and may recover from the validation failure.
         * 
         * @throws CallbackException Thrown if the recovery is unsuccessful.
         */
        void onConfirmation() throws CallbackException;
    }

    /**
     * Validation result type.
     * 
     * @author Doreen Seider
     */
    enum InstanceValidationResultType {
        /**
         * Instance passed validation.
         */
        PASSED,
        /**
         * Instance failed validation, but proceeding is allowed (in GUI mode, it should be up to the user whether to proceed or not).
         */
        FAILED_CONFIRMATION_REQUIRED,
        /**
         * Instance failed validation, but proceeding is possible if the user explicitly confirms the recovery. This necessitates another
         * round of validation to confirm that the recovery indeed resulted in a valid RCE instance.
         */
        FAILED_RECOVERY_REQUIRED,
        /**
         * Instance failed validation, it needs to be shut down.
         */
        FAILED_SHUTDOWN_REQUIRED;
    }

    /**
     * @return display name of validator, which performed the validation
     */
    String getValidationDisplayName();

    /**
     * @return {@link InstanceValidationResultType}
     */
    InstanceValidationResultType getType();

    /**
     * @return message, which should be logged (is <code>null</code> in case of {@link InstanceValidationResultType#PASSED})
     */
    String getLogMessage();

    /**
     * @return message which should be shown when running in GUI mode (is <code>null</code> in case of
     *         {@link InstanceValidationResultType#PASSED})
     */
    String getGuiDialogMessage();

    /**
     * @return callback that should be triggered if the user asks to proceed with the startup (is <code>null</code> if getType() !=
     *         {@link InstanceValidationResultType#FAILED_RECOVERY_REQUIRED}).
     */
    Callback getCallback();
}
