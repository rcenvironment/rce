/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Result instance startup validation performed by {@link InstanceValidator}s.
 * 
 * @author Doreen Seider
 */
public class InstanceValidationResultImpl implements InstanceValidationResult {
    
    private final String validatorDisplayName;
    
    private final InstanceValidationResultType type;
    
    private final String guiDialogMessage;
    
    private final String logMessage;
    
    private final Callback callback;

    public InstanceValidationResultImpl(String validatorDisplayName, InstanceValidationResultType type,
        String logMessage, String guiDialogMessage, Callback callback) {
        this.validatorDisplayName = validatorDisplayName;
        this.type = type;
        this.logMessage = logMessage;
        this.guiDialogMessage = guiDialogMessage;
        this.callback = callback;
    }

    @Override
    public String getValidationDisplayName() {
        return validatorDisplayName;
    }
    
    @Override
    public InstanceValidationResultType getType() {
        return type;
    }

    @Override
    public String getGuiDialogMessage() {
        return guiDialogMessage;
    }

    @Override
    public String getLogMessage() {
        return logMessage;
    }
    
    public Callback getCallback() {
        return callback;
    }

}
