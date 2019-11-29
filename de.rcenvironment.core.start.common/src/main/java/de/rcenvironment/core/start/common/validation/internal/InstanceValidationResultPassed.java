/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.common.validation.internal;

import java.util.Optional;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;

/**
 * An InstanceValidationResult that represents a passed validation. Has no log message, no gui message, no callback function, and provides
 * no user hint.
 * 
 * @author Alexander Weinert
 */
public class InstanceValidationResultPassed implements InstanceValidationResult {
    private final String validatorDisplayName;

    public InstanceValidationResultPassed(String validatorDisplayName) {
        this.validatorDisplayName = validatorDisplayName;
    }

    @Override
    public String getValidationDisplayName() {
        return this.validatorDisplayName;
    }

    @Override
    public InstanceValidationResultType getType() {
        return InstanceValidationResultType.PASSED;
    }

    @Override
    public String getLogMessage() {
        return null;
    }

    @Override
    public String getGuiDialogMessage() {
        return null;
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
