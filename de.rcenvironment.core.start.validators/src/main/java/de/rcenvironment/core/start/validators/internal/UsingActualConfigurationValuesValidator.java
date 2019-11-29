/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Ensures that the actual configuration values are applied.
 * 
 * @author Doreen Seider
 */
public class UsingActualConfigurationValuesValidator extends DefaultInstanceValidator {

    private ConfigurationService configurationService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Configuration values";
        
        if (configurationService.isUsingDefaultConfigurationValues()) {
            String errorMessage1 = "Failed to load configuration file. Most likely, it has syntax errors. Check the log for details.";
            String errorMessage2 = "Default configuration values will be applied.";

            return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(validationDisplayName,
                errorMessage1 + " " + errorMessage2, errorMessage1 + "\n\n" + errorMessage2);
        }
        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }
    
    protected void bindConfigurationService(ConfigurationService newService) {
        configurationService = newService;
    }

}
