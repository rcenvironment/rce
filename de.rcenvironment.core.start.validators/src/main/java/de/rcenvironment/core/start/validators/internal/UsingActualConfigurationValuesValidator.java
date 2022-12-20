/*
 * Copyright 2006-2022 DLR, Germany
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
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Ensures that the actual configuration values are applied.
 * 
 * @author Doreen Seider
 * @author Kathrin Schaffert (added configuration file path to message)
 */
public class UsingActualConfigurationValuesValidator extends DefaultInstanceValidator {

    private ConfigurationService configurationService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Configuration values";

        if (configurationService.isUsingDefaultConfigurationValues()) {
            String errorMessage1 =
                StringUtils.format("Failed to load configuration file: %s \nMost likely, it has syntax errors. Check the log for details.",
                    configurationService.getProfileConfigurationFile().getAbsoluteFile());
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
