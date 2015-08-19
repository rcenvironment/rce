/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.testing;

import java.io.File;
import java.io.FileInputStream;
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
            if (!paramFile.isFile()) {
                throw new IOException("Expected to find test parameter file " + paramFile.getAbsolutePath());
            }
            try (InputStream is = new FileInputStream(paramFile)) {
                properties.load(is);
            }
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

    @Override
    public String getNonEmptyString(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            throw new AssertionError(String.format("Required test property '%s' is undefined or empty", key));
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
            throw new AssertionError(String.format("Configured test file '%s' does not exist", value.getAbsolutePath()));
        }
        return value;
    }

    @Override
    public File getExistingDir(String key) {
        File value = getDefinedFileOrDir(key);
        if (!value.isDirectory()) {
            throw new AssertionError(String.format("Configured test directory '%s' does not exist", value.getAbsolutePath()));
        }
        return value;
    }

    @Override
    public int getOptionalInteger(String key, int defaultValue) {
        return StringUtils.nullSafeParseInt(properties.getProperty(key), defaultValue);
    }
}
