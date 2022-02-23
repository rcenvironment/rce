/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;

import de.rcenvironment.core.configuration.bootstrap.EclipseLaunchParameters;

/**
 * Tests for {@link CommandLineArguments}.
 *
 * @author Tobias Brieden
 */
public class CommandLineArgumentsTest {

    // TODO copied from LaunchParamterTestUtils
    /**
     * Sets the given parameters in the eclipse.commands property and triggers their re-reading.
     * 
     * @param parameters The parameters that should be set.
     */
    public static void setParameters(String... parameters) {
        String commandString = StringUtils.join(parameters, "\n");
        System.setProperty("eclipse.commands", commandString);
        EclipseLaunchParameters.getInstance().readParameters();
    }

    /**
     * Make sure that there are no consumed parameters left over from this test.
     */
    @After
    public void tearDown() {
        System.clearProperty("eclipse.commands");
        EclipseLaunchParameters.getInstance().readParameters();
    }

    private void initTest(String... parameters) {
        CommandLineArgumentsTest.setParameters(parameters);
        String[] tokens = EclipseLaunchParameters.getInstance().getFilteredTokensAsArray();
        CommandLineArguments.initialize(tokens);
    }

    /**
     * Tests if an arbitrary option is read.
     *
     */
    @Test
    public void testIfOptionIsRead() {
        initTest("--showAdvancedTab");

        assertTrue(CommandLineArguments.isShowAdvancedTab());
    }

    /**
     * Tests that an option after the profile option is not removed.
     *
     */
    @Test
    public void testIfOptionAfterProfileOptionIsNotRemoved() {
        initTest("-p", "--showAdvancedTab");

        assertTrue(CommandLineArguments.isShowAdvancedTab());
    }
}
