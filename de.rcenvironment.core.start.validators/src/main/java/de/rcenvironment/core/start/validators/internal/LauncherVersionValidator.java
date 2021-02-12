/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.launcher.integration.RCELauncherIntegration;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Checks if the appropriate launcher is used, or if an older one is used.
 *
 * @author Tobias Brieden
 */
public class LauncherVersionValidator extends DefaultInstanceValidator {

    private static final String VALIDATION_DISPLAY_NAME = "Launcher Version";

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public InstanceValidationResult validate() {

        String runningLauncherVersionStr = System.getProperty(RCELauncherIntegration.PROP_RCE_LAUNCHER_VERSION);
        if (runningLauncherVersionStr == null) {
            // RCE is currently running with a launcher which does not write its version number (RCE <= 8.0.2)

            // If the running launcher is from RCE 8.0.0 - RCE 8.0.2, the early logging might not be available. But there is already a
            // warning written by the InsertOldLogAppender in this case.

            // If the running launcher is from RCE < 8.0.0, certain bugs might not be fixed.

            log.warn("RCE was started with an old launcher version.");
            return InstanceValidationResultFactory.createResultForPassed(VALIDATION_DISPLAY_NAME);
        }

        try {
            int runningLauncherVersion = Integer.parseInt(runningLauncherVersionStr);

            if (runningLauncherVersion != RCELauncherIntegration.LAUNCHER_VERSION) {
                log.warn(StringUtils.format("RCE was started with launcher version %d but version %d was expected.", runningLauncherVersion,
                    RCELauncherIntegration.LAUNCHER_VERSION));
            }

            // TODO add appropriate behavior for version mismatches

            return InstanceValidationResultFactory.createResultForPassed(VALIDATION_DISPLAY_NAME);
        } catch (NumberFormatException e) {
            return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(VALIDATION_DISPLAY_NAME,
                "RCE was started with an unkown launcher. This might result in unkown behaviour.",
                "RCE was started with an unkown launcher. This might result in unkown behaviour.");
        }

    }

}
