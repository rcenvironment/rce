/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
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
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Validates that the instance's managed temp directory provided by TempFileService can actually be written to. This is simply tested by
 * requesting a temporary file, and checking if it was successfully created.
 * 
 * @author Robert Mischke
 */
public class TempDirectoryWritableValidator extends DefaultInstanceValidator {

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
            boolean canRead = tempFile.canRead();
            // Clean up test file. Even if we are not allowed to read the file, we are still able to delete it, since this depends on the
            // granted permissions wrt. the parent directory.
            tempFileService.disposeManagedTempDirOrFile(tempFile);

            if (canRead) {
                // all ok, no error
                return InstanceValidationResultFactory.createResultForPassed(validationDisplayName);
            } else {
                // unlikely case, but that's what validation is for...
                log.error("Creating a temporary test file succeeded, but was not readable afterwards; "
                    + "a validation error will be generated");
            }
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
