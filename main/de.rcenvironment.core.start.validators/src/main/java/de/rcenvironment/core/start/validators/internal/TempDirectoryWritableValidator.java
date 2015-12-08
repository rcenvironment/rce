/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.start.validators.internal;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.InstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Validates that the instance's managed temp directory provided by TempFileService can actually be written to. This is simply tested by
 * requesting a temporary file, and checking if it was successfully created.
 * 
 * @author Robert Mischke
 */
public class TempDirectoryWritableValidator implements InstanceValidator {

    private ConfigurationService configurationService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public InstanceValidationResult validate() {
        final String validationDisplayName = "RCE temp directory";
        
        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        log.debug("Initializing temp file service and creating a test file");
        File tempFile;
        try {
            tempFile = tempFileService.createTempFileFromPattern("check.*.tmp");
            if (tempFile.canRead()) {
                // all ok, no error
                return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
            } else {
                // unlikely case, but that's what validation is for...
                log.error("Creating a temporary test file succeeded, but was not readable afterwards; "
                    + "a validation error will be generated");
            }
            // clean up test file
            tempFileService.disposeManagedTempDirOrFile(tempFile);
        } catch (IOException e) {
            log.error("Error creating a temporary test file; a validation error will be generated", e);
        }

        String errorMessage = StringUtils.format(Messages.failedToCreateTempFile, 
            configurationService.getParentTempDirectoryRoot().getAbsolutePath());
        return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(
            validationDisplayName, errorMessage);
    }
    
    protected void bindConfigurationService(ConfigurationService configIn) {
        configurationService = configIn;
    }

}
