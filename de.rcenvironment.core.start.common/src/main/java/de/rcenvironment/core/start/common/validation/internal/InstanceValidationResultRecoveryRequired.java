/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import java.util.Optional;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Result instance startup validation performed by {@link InstanceValidator}s.
 * 
 * @author Doreen Seider
 * @author Alexander Weinert
 */
public class InstanceValidationResultRecoveryRequired implements InstanceValidationResult {
    
    private final String validatorDisplayName;
    
    private final String guiDialogMessage;
    
    private final String logMessage;
    
    private final Optional<String> userHint;
    
    private final Callback callback;

    public InstanceValidationResultRecoveryRequired(String validatorDisplayName,
        String logMessage, String guiDialogMessage, String userHint, Callback callback) {
        this.validatorDisplayName = validatorDisplayName;
        this.logMessage = logMessage;
        this.userHint = Optional.ofNullable(userHint);
        this.guiDialogMessage = guiDialogMessage;
        this.callback = callback;
    }

    @Override
    public String getValidationDisplayName() {
        return validatorDisplayName;
    }
    
    @Override
    public InstanceValidationResultType getType() {
        return InstanceValidationResultType.FAILED_RECOVERY_REQUIRED;
    }

    @Override
    public String getGuiDialogMessage() {
        return guiDialogMessage;
    }

    @Override
    public String getLogMessage() {
        return logMessage;
    }
    
    @Override
    public Callback getCallback() {
        return callback;
    }
    
    @Override
    public Optional<String> getUserHint() {
        return userHint;
    }

}
