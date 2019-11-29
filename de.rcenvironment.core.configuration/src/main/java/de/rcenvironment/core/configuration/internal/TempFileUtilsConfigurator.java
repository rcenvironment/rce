/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.utils.common.TempFileManager;

/**
 * An OSGi component stub to configure the {@link TempFileManager} singleton with settings loaded from the global
 * {@link ConfigurationService}.
 * 
 * @author Robert Mischke
 */
public class TempFileUtilsConfigurator {

    /**
     * Flag to detect erroneous duplicate bind/unbind calls.
     */
    private boolean isBound = false;

    private Log log = LogFactory.getLog(getClass());

    /**
     * Constructor for OSGi-DS; deprecated to prevent accidental use.
     */
    @Deprecated
    public TempFileUtilsConfigurator() {}

    /**
     * OSGi-DS "bind" method.
     */
    protected void bindConfigurationService(ConfigurationService newConfigurationService) throws IOException {
        if (isBound) {
            log.warn("Duplicate bind()");
        }
        // TODO (p3) deactivated; rework or delete - misc_ro
        // File globalTempDirectoryRoot = newConfigurationService.getGlobalTempDirectoryRoot();
        // TempFileManager.setupWithCustomRootDir(globalTempDirectoryRoot);
        isBound = true;
    }

    /**
     * OSGi-DS "unbind" method.
     */
    protected void unbindConfigurationService(ConfigurationService oldConfigurationService) {
        if (!isBound) {
            log.warn("Unexpected unbind()");
        }
        isBound = false;
    }

}
