/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
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
     * Validation result type.
     * 
     * @author Doreen Seider
     */
    public enum InstanceValidationResultType {
        /**
         * Instance passed validation.
         */
        PASSED,
        /**
         * Instance failed validation, but proceeding is allowed (in GUI mode, it should be up to the user whether to proceed or not).
         */
        FAILED_PROCEEDING_ALLOWED,
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
     * @return message, which should be shown when running in GUI mode (is <code>null</code> in case of
     *         {@link InstanceValidationResultType#PASSED})
     */
    String getGuiDialogMessage();

}
