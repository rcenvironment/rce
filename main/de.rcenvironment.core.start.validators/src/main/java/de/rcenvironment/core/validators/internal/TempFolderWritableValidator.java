/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
package de.rcenvironment.core.validators.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.start.common.validation.PlatformMessage;
import de.rcenvironment.core.start.common.validation.PlatformValidator;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.TempFileService;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Validates that the instance's managed temp directory provided by TempFileService can actually be written to. This is simply tested by
 * requesting a temporary file, and checking if it was successfully created.
 * 
 * @author Robert Mischke
 */
public class TempFolderWritableValidator implements PlatformValidator {

    private ConfigurationService configurationService;

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public Collection<PlatformMessage> validatePlatform() {
        final Collection<PlatformMessage> result = new LinkedList<PlatformMessage>();

        TempFileService tempFileService = TempFileServiceAccess.getInstance();
        log.debug("Initializing temp file service and creating a test file");
        File tempFile;
        try {
            tempFile = tempFileService.createTempFileFromPattern("check.*.tmp");
            if (tempFile.canRead()) {
                // all ok, no error
                return result;
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
        result.add(new PlatformMessage(PlatformMessage.Type.ERROR, ValidatorsBundleActivator.bundleSymbolicName,
            StringUtils.format(Messages.failedToCreateTempFile, configurationService.getParentTempDirectoryRoot().getAbsolutePath())));

        return result;
    }

    protected void bindConfigurationService(ConfigurationService configIn) {
        configurationService = configIn;
    }

}
