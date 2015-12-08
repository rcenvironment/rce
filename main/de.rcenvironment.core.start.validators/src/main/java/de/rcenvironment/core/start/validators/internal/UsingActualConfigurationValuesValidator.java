/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Ensures that the actual configuration values are applied.
 * 
 * @author Doreen Seider
 */
public class UsingActualConfigurationValuesValidator implements InstanceValidator {

    private ConfigurationService configurationService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Configuration values";
        
        if (configurationService.isUsingDefaultConfigurationValues()) {
            String errorMessage1 = "Failed to load configuration file. Most likely, it has syntax errors. Check the log for details.";
            String errorMessage2 = "Default configuration values will be applied.";

            return InstanceValidationResultFactory.createResultForFailureWhichAllowesToProceed(validationDisplayName,
                errorMessage1 + " " + errorMessage2, errorMessage1 + "\n\n" + errorMessage2);
        }
        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }
    
    protected void bindConfigurationService(ConfigurationService newService) {
        configurationService = newService;
    }

}
