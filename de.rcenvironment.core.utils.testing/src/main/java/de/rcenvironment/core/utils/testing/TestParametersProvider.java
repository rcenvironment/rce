/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.testing;

import java.io.File;

/**
 * Generic interface for test configuration.
 * 
 * @author Robert Mischke
 */
public interface TestParametersProvider {

    /**
     * Returns a parameter string. If it is undefined or empty, an {@link AssertionError} is thrown.
     * 
     * @param key the parameter key
     * @return the unmodified parameter String value
     */
    String getNonEmptyString(String key);
    
    /**
     * Returns a parameter string, or null, if the parameter is undefined.
     * 
     * @param key the parameter key
     * @return the unmodified parameter String value
     */
    String getOptionalString(String key);

    /**
     * Returns a parameter string, converted to a {@link File}. If it is undefined or empty, an {@link AssertionError} is thrown.
     * 
     * Note: The naming of this method was chosen this way as "NonEmptyFileOrDir" (which would be consistent with the other methods) would
     * be misleading for files/dirs.
     * 
     * @param key the parameter key
     * @return a {@link File} created from the unmodified parameter String value
     */
    File getDefinedFileOrDir(String key);

    /**
     * Like {@link #getDefinedFileOrDir(String)}, but additionally checks that the resulting {@link File} points to an existing file.
     * Otherwise, an {@link AssertionError} is thrown.
     * 
     * @param key the parameter key
     * @return a {@link File} created from the unmodified parameter String value
     */
    File getExistingFile(String key);

    /**
     * Like {@link #getDefinedFileOrDir(String)}, but additionally checks that the resulting {@link File} points to an existing directory.
     * Otherwise, an {@link AssertionError} is thrown.
     * 
     * @param key the parameter key
     * @return a {@link File} created from the unmodified parameter String value
     */
    File getExistingDir(String key);

    /**
     * Returns the integer value of the given parameter. If it is undefined, or cannot be parsed, the given default value is returned
     * instead.
     * 
     * @param key the parameter key
     * @param defaultValue the fallback/default value
     * @return the parsed parameter value, or the given default value
     */
    int getOptionalInteger(String key, int defaultValue);
    
    /**
     * Returns the integer value of the given parameter. If it is undefined, or cannot be parsed, an {@link AssertionError} is thrown.
     * 
     * @param key the parameter key
     * @return the parsed parameter value
     */
    int getExistingInteger(String key);

}
