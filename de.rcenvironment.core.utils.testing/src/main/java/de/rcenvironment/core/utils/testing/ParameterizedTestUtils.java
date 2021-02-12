/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Utility methods for parameterized unit, integration and manual tests.
 * 
 * @author Robert Mischke
 */
public class ParameterizedTestUtils implements TestParametersProvider {

    private static final ThreadLocal<File> TEST_PARAMS_DIRECTORY_THREADLOCAL = new ThreadLocal<>();

    private Properties properties;

    /**
     * Sets the directory to load test configuration files from. As tests may be run in parallel with different configuration directories,
     * this only sets it for tests run from the current thread.
     * 
     * @param paramsDir the parameters directory; may be null
     */
    public static void setTestParameterDirectoryForCurrentThread(File paramsDir) {
        TEST_PARAMS_DIRECTORY_THREADLOCAL.set(paramsDir);
    }

    /**
     * Reads a specific property file resource.
     * 
     * @param paramFile the location of the parameter/properties file to load
     * @return 'self' (for easy command chaining)
     * @throws IOException on load failure, e.g. on a non-existing file
     */
    public TestParametersProvider readPropertiesFile(File paramFile) throws IOException {

        properties = new Properties();

        verifyAndLoadPropertiesFile(paramFile);

        return this; // for easy command chaining
    }

    /**
     * Reads a property file resouce with the name of the given class and a ".properties" suffix. If no such file is found, an
     * {@link IOException} is thrown.
     * 
     * @param testClass the test class to load properties for
     * @return 'self' (for easy command chaining)
     * @throws IOException on load failure
     */
    public TestParametersProvider readDefaultPropertiesFile(Class<?> testClass) throws IOException {

        File paramsDir = TEST_PARAMS_DIRECTORY_THREADLOCAL.get();

        properties = new Properties();
        String resourceName = testClass.getSimpleName() + ".properties";

        if (paramsDir != null) {
            // standalone use case: read from file in parameter directory
            File paramFile = new File(paramsDir, resourceName);
            verifyAndLoadPropertiesFile(paramFile);
        } else {
            // development/IDE use case: read from workspace file as resource
            InputStream propertiesStream = testClass.getResourceAsStream("/" + resourceName);
            if (propertiesStream == null) {
                throw new IOException("Failed to load test configuration as resouce: " + resourceName);
            }
            properties.load(propertiesStream);
        }

        return this; // for easy command chaining
    }

    /**
     * Reads a property file resource with the specified name. If no such file is found, an {@link IOException} is thrown.
     * 
     * @param resourceName the classpath location of the file resource
     * @return 'self' (for easy command chaining)
     * @throws IOException on load failure
     */
    public TestParametersProvider readPropertiesFile(String resourceName) throws IOException {

        properties = new Properties();

        File paramFile = new File(resourceName);
        verifyAndLoadPropertiesFile(paramFile);

        return this; // for easy command chaining
    }

    @Override
    public String getNonEmptyString(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new AssertionError(StringUtils.format("Required test property '%s' is undefined or empty", key));
        }
        return value;
    }

    @Override
    public File getDefinedFileOrDir(String key) {
        String value = getNonEmptyString(key);
        return new File(value).getAbsoluteFile();
    }

    @Override
    public File getExistingFile(String key) {
        File value = getDefinedFileOrDir(key);
        if (!value.isFile()) {
            throw new AssertionError(StringUtils.format("Configured test file '%s' does not exist", value.getAbsolutePath()));
        }
        return value;
    }

    @Override
    public File getExistingDir(String key) {
        File value = getDefinedFileOrDir(key);
        if (!value.isDirectory()) {
            throw new AssertionError(StringUtils.format("Configured test directory '%s' does not exist", value.getAbsolutePath()));
        }
        return value;
    }

    @Override
    public int getOptionalInteger(String key, int defaultValue) {
        return StringUtils.nullSafeParseInt(properties.getProperty(key), defaultValue);
    }

    @Override
    public int getExistingInteger(String key) {
        String value = getNonEmptyString(key);
        int intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AssertionError(StringUtils.format("Value %s for required test property %s is not an Integer.", key, value));
        }
        return intValue;
    }

    private void verifyAndLoadPropertiesFile(File paramFile) throws IOException, FileNotFoundException {
        if (!paramFile.isFile()) {
            throw new IOException("Expected to find test parameter file " + paramFile.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(paramFile)) {
            properties.load(is);
        }
    }

    @Override
    public String getOptionalString(String key) {
        return properties.getProperty(key);
    }
}
