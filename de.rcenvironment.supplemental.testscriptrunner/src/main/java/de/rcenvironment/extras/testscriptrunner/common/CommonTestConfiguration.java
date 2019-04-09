/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.common;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.utils.testing.ParameterizedTestUtils;
import de.rcenvironment.core.utils.testing.TestParametersProvider;

/**
 * Configuration aspects that are common to all test suites, and constant over all test or execution steps.
 * 
 * @author Robert Mischke
 */
public final class CommonTestConfiguration {

    private static CommonTestConfiguration instance; // set by initialize()

    private final TestParametersProvider testParameters;

    /**
     * Private constructor.
     * 
     * @param propertiesFileLocation the property file's location (as a string)
     * @throws IOException if loading or parsing the file fails
     */
    private CommonTestConfiguration(String propertiesFileLocation) throws IOException {
        testParameters = new ParameterizedTestUtils().readPropertiesFile(new File(propertiesFileLocation));
    }

    /**
     * Loads/initialized the configuration from the given properties file.
     * 
     * @param propertiesFileLocation the property file's location (as a string)
     * @throws IOException if loading or parsing the file fails
     */
    public static void initialize(String propertiesFileLocation) throws IOException {
        instance = new CommonTestConfiguration(propertiesFileLocation);
    }

    public static TestParametersProvider getParameters() {
        return getInstance().testParameters;
    }

    private static CommonTestConfiguration getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Tried to access common configuration before calling initialize()");
        }
        return instance;
    }

}
