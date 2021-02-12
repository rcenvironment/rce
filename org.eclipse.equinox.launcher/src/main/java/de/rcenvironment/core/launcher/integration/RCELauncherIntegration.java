/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.launcher.integration;

import org.eclipse.equinox.launcher.RCELauncherHelper;

/**
 * Provides constants for integrating the RCE launcher with the main application via system properties.
 * 
 * IMPORTANT: This file is currently duplicated in the core.start.validators bundle as the RCE runtime cannot access the launcher bundle.
 *
 * @author Robert Mischke (extracted out of {@link RCELauncherHelper} by Tobias Rodehutskors)
 */
public final class RCELauncherIntegration {

    /**
     * Indicates the semantic version of the RCE launcher. This number should only be updated if the code within the launcher changes.
     * 
     * This version number may be necessary during an update of RCE. After the update, RCE needs to be restarted. During the restart
     * process, the rce.ini is not reevaluated and therefore, the new launcher is not used. This might be a problem, if later code assumes
     * that the launcher guarantees certain preconditions.
     * 
     * If this version number is increased, additional code needs to be added to the LauncherVersionValidator class to handle the update
     * case.
     */
    public static final int LAUNCHER_VERSION = 810;

    /**
     * System property for the launcher version.
     */
    public static final String PROP_RCE_LAUNCHER_VERSION = "de.rcenvironment.launcher.version";

    private RCELauncherIntegration() {

    }

}
