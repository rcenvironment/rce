/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration.bootstrap;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods useful during unit tests.
 *
 * @author Tobias Brieden
 */
public final class LaunchParameterTestUtils {
    
    private LaunchParameterTestUtils() {}
    
    /**
     * Sets the given parameters in the eclipse.commands property and triggers their re-reading.
     * 
     * @param parameters The parameters that should be set.
     */
    public static void setParameters(String... parameters) {
        String commandString = StringUtils.join(parameters, "\n");
        System.setProperty("eclipse.commands", commandString);
        LaunchParameters.getInstance().readParameters();
        System.clearProperty("eclipse.commands");
    }
}
