/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.datamanagement.backend.MetaDataBackendService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Validator to check if the meta data backend has been started correctly.
 *
 * @author Brigitte Boden
 */
public class MetaDataBackendValidator extends DefaultInstanceValidator {

    private static final String ERROR_PREFIX = "Failed to initialize database.";
    
    private MetaDataBackendService metadataBackendService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Database";
        if (metadataBackendService.isMetaDataBackendOk()) {
            return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
        } else {
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(validationDisplayName,
                ERROR_PREFIX + "\n\n" + metadataBackendService.getMetaDataBackendStartErrorMessage());
        }
    }

    protected void bindMetaDataBackendService(MetaDataBackendService newService) {
        this.metadataBackendService = newService;
    }
}
