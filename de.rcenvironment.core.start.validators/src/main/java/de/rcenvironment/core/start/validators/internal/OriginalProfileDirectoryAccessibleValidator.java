/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.io.File;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Verifies that the user specified profile folder can be accessed (it was created and is readable and writeable). This might be not the
 * case if 
 *
 * @author Tobias Rodehutskors
 */
public class OriginalProfileDirectoryAccessibleValidator extends DefaultInstanceValidator {

    private static ConfigurationService configService;

    @Override
    public InstanceValidationResult validate() {
        File originalProfileDir = configService.getOriginalProfileDirectory();

        if (!originalProfileDir.exists() || !originalProfileDir.isDirectory()
            || !originalProfileDir.canRead() || !originalProfileDir.canWrite()) {

            String errorMessage = "The specified profile folder " + originalProfileDir.getAbsolutePath()
                + " is either not readable or not writeable. Probably the proper permissions are not granted to your user account."
                + " Choose another profile directory. (See the user guide for more information about the profile directory.)";
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                "Profile folder not accessible.", errorMessage, errorMessage);
        }

        return InstanceValidationResultFactory.createResultForPassed("Original profile directory is accessible.");
    }

    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

}
