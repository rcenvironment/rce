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

/**
 * An InstanceValidationResult that represents a failed validation which requires a shutdown. Has no callback function and provides no user
 * hint.
 * 
 * @author Alexander Weinert
 */
public class InstanceValidationResultShutdownRequired implements InstanceValidationResult {
    private final String validatorDisplayName;
    
    private final String logMessage;
    
    private final String guiMessage;

    public InstanceValidationResultShutdownRequired(String validatorDisplayName, String logMessage, String guiMessage) {
        this.validatorDisplayName = validatorDisplayName;
        this.logMessage = logMessage;
        this.guiMessage = guiMessage;
    }

    @Override
    public String getValidationDisplayName() {
        return this.validatorDisplayName;
    }

    @Override
    public InstanceValidationResultType getType() {
        return InstanceValidationResultType.FAILED_SHUTDOWN_REQUIRED;
    }

    @Override
    public String getLogMessage() {
        return this.logMessage;
    }

    @Override
    public String getGuiDialogMessage() {
        return this.guiMessage;
    }

    @Override
    public Callback getCallback() {
        return null;
    }

    @Override
    public Optional<String> getUserHint() {
        return Optional.empty();
    }

}
