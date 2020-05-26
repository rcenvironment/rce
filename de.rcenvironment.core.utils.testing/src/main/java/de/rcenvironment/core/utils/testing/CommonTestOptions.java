/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.testing;

/**
 * Simple holder for runtime test options, typically set by system properties. Currently, the only option is to enable "extended testing",
 * which means that "higher intensity" versions of tests should be enabled, for example by increasing iteration counts or test sizes.
 * 
 * To enable this option in standalone test runs, add <code>-Drce.tests.runExtended</code> to the test run. To set this option while running
 * tests from an IDE, set the {@link #DEV_OPTION_ENABLE_RUN_EXTENDED} constant below; make sure you don't commit this change by accident (it
 * must always be "false" in version control).
 * 
 * @author Robert Mischke
 */
public final class CommonTestOptions {

    private static final String SYSTEM_PROPERTY_RUN_EXTENDED = "rce.tests.runExtended";

    // set this to "true" to run extended tests from your IDE; make sure to NOT commit this change
    private static final boolean DEV_OPTION_ENABLE_RUN_EXTENDED = false;

    // immutable singleton instance
    private static final CommonTestOptions sharedInstance = new CommonTestOptions();

    private final boolean withExtendedTests;

    public CommonTestOptions() {
        withExtendedTests = DEV_OPTION_ENABLE_RUN_EXTENDED || System.getProperty(SYSTEM_PROPERTY_RUN_EXTENDED) != null;
    }

    /**
     * @return true if "extended" tests should be run; this can either mean that certain tests should be included, or that any
     *         "higher intensity" versions of tests should be enabled, for example by increasing iteration counts or test sizes
     */
    public static boolean isExtendedTestingEnabled() {
        return sharedInstance.withExtendedTests;
    }

    /**
     * Convenience method to select between two integer values. Returns the first value if "standard" testing is configured, or the second
     * value if "extended" testing is configured.
     * 
     * @param standardValue the value to return in "standard" testing mode
     * @param extendedValue the value to return in "extended" testing mode
     * @return the first parameter value if "standard" testing is configured, or the second parameter value if "extended" testing is
     *         configured
     */
    public static int selectStandardOrExtendedValue(int standardValue, int extendedValue) {
        if (sharedInstance.withExtendedTests) {
            return extendedValue;
        } else {
            return standardValue;
        }
    }
}
