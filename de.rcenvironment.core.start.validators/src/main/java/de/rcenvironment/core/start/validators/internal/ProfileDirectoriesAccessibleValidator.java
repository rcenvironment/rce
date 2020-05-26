/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileException;
import de.rcenvironment.core.configuration.bootstrap.profile.ProfileUtils;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult.InstanceValidationResultType;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Verifies that both the common profile directory as well as the user specified profile folder can be accessed (it was created and is
 * readable and writeable).
 *
 * @author Tobias Rodehutskors
 * @author Alexander Weinert
 */
@Component(service = InstanceValidator.class)
public class ProfileDirectoriesAccessibleValidator extends DefaultInstanceValidator {

    private static ConfigurationService configService;

    @Override
    public InstanceValidationResult validate() {
        File commonProfileDir;
        try {
            commonProfileDir = ProfileUtils.getProfilesParentDirectory().toPath().resolve("common").toFile();
        } catch (ProfileException e) {
            final String errorMessage = "Could not open common profile.";
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown("Common profile not accessible",
                errorMessage);
        }
        final InstanceValidationResult commonProfileDirValidationResult = validateProfileDir(commonProfileDir);
        
        if (!InstanceValidationResultType.PASSED.equals(commonProfileDirValidationResult.getType())) {
            return commonProfileDirValidationResult;
        }
        
        final File originalProfileDir = configService.getOriginalProfileDirectory();
        return validateProfileDir(originalProfileDir);
    }

    private InstanceValidationResult validateProfileDir(File originalProfileDir) {
        if (!originalProfileDir.exists() || !originalProfileDir.isDirectory()
            || !originalProfileDir.canRead() || !originalProfileDir.canWrite()) {

            String errorMessage = "The specified profile folder " + originalProfileDir.getAbsolutePath()
                + " is either not readable or not writeable. Probably the proper permissions are not granted to your user account."
                + " Choose another profile directory. (See the user guide for more information about the profile directory.)";
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                "Profile folder not accessible.", errorMessage, errorMessage);
        } else {
            return InstanceValidationResultFactory.createResultForPassed("Original profile directory is accessible.");
        }
    }

    @Reference
    protected void bindConfigurationService(ConfigurationService configIn) {
        configService = configIn;
    }

}
