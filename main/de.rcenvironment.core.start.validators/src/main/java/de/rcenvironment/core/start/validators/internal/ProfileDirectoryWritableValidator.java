/*
 * Copyright (C) 2006-2015 DLR, Germany
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
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;

/**
 * Ensures that the profile directory used is writable.
 * 
 * @author Christian Weiss
 */
public class ProfileDirectoryWritableValidator implements InstanceValidator {

    private ConfigurationService configurationService;

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "RCE profile directory";
        
        File profileDir = configurationService.getProfileDirectory();
        if (!profileDir.exists() || !profileDir.isDirectory()) {
            String errorMessage = Messages.directoryRceFolderDoesNotExist + profileDir.getAbsolutePath();
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                validationDisplayName, errorMessage);
        } else if (!profileDir.canRead() || !profileDir.canWrite()) {
            String errorMessage = Messages.directoryRceFolderNotReadWriteAble + profileDir.getAbsolutePath();
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                validationDisplayName, errorMessage);
            
        }
        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }
    
    protected void bindConfigurationService(final ConfigurationService newConfigurationService) {
        configurationService = newConfigurationService;
    }


}
