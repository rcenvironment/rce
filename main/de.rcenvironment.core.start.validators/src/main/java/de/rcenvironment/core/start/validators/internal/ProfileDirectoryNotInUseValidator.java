/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Validator to prevent accidental use of same instance data directory if multiple RCE instances are running on the same machine. The check
 * is based on a lock file in the data directory of each instance, which is performed by the {@link ConfigurationService} implementation.
 * 
 * @author Jan Flink
 * @author Robert Mischke
 */
public class ProfileDirectoryNotInUseValidator extends DefaultInstanceValidator {

    private static ConfigurationService configService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "Profile directory lock";
        
        if (!configService.isIntendedProfileDirectorySuccessfullyLocked()) {
            String errorMessage = StringUtils.format(Messages.instanceIdAlreadyInUse, configService
                .getOriginalProfileDirectory().getAbsolutePath());
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                validationDisplayName, errorMessage, errorMessage);
        }
        
        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }

    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }


}
