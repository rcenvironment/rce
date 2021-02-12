/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration;

/**
 * Represents an error while reading or writing configuration data.
 * 
 * @author Robert Mischke
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 1011068952653035227L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
