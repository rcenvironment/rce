/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods useful during unit tests.
 *
 * @author Tobias Brieden
 */
public final class EclipseLaunchParameterTestUtils {

    private EclipseLaunchParameterTestUtils() {}

    /**
     * Sets the given parameters in the eclipse.commands property and triggers their re-reading.
     * 
     * TODO this approach is quite brittle; it would be better to simply create new {@link EclipseLaunchParameters} instances from input, instead
     * of modifying global state -- misc_ro
     * 
     * @param parameters The parameters that should be set.
     */
    public static void simulateLaunchParameters(String... parameters) {
        String commandString = StringUtils.join(parameters, "\n");
        System.setProperty("eclipse.commands", commandString);
        EclipseLaunchParameters.getInstance().readParameters();
        System.clearProperty("eclipse.commands");
    }
}
