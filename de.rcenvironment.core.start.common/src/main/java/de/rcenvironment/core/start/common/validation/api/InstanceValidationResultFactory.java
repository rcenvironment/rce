/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.api;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.internal.InstanceValidationResultImpl;


/**
 * Creates {@link InstanceValidationResultFactory}s.
 * 
 * @author Doreen Seider
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
            logMessage, logMessage);
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
            logMessage, guiDialogMessage);
    }

    /**
     * Creates a result for the case of an failure, which allows the instance to proceed running.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @param logMessage message, which should be logged
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForFailureWhichAllowesToProceed(String validationDisplayName, String logMessage) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED, 
            logMessage, logMessage);
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
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.FAILED_PROCEEDING_ALLOWED, 
            logMessage, guiDialogMessage);
    }

    /**
     * Creates a result for the case of passing.
     * 
     * @param validationDisplayName display name of the validator, which generates the validation result
     * @return instance of {@link InstanceValidationResult}
     */
    public static InstanceValidationResult createResultForPassed(String validationDisplayName) {
        return new InstanceValidationResultImpl(validationDisplayName, InstanceValidationResultType.PASSED, "", "");
    }

}
