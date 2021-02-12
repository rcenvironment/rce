/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;

/**
 * Ensures the length of the path to RCE's installation directory is not too long for the file system.
 * 
 * @author Sascha Zur
 */
public class InstallationDirectoryPathLengthValidator extends DefaultInstanceValidator {
    
    private static final int MAX_LENGTH_WINDOWS_7_UNIX = 255;
    
    private static final int MAX_LENGTH_PLUGIN_NAMES = 96; //Optimizer
        
    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "RCE installation directory (path length)";
        
        String rceInstallationPath =  System.getProperty("eclipse.home.location");
       
        if (rceInstallationPath.length() + MAX_LENGTH_PLUGIN_NAMES > MAX_LENGTH_WINDOWS_7_UNIX){
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
                validationDisplayName, Messages.directoryRceFolderPathTooLong + ": " + rceInstallationPath,
                Messages.directoryRceFolderPathTooLong + ": \n" + rceInstallationPath);
        }
         
        return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
    }
    
}
