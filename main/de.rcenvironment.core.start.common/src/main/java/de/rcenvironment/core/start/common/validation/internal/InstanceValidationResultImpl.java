/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
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
    
    private String validatorDisplayName;
    
    private InstanceValidationResultType type;
    
    private String guiDialogMessage;
    
    private String logMessage;

    public InstanceValidationResultImpl(String validatorDisplayName, InstanceValidationResultType type,
        String logMessage, String guiDialogMessage) {
        this.validatorDisplayName = validatorDisplayName;
        this.type = type;
        this.logMessage = logMessage;
        this.guiDialogMessage = guiDialogMessage;
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

}
