/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration.bootstrap;


/**
 * Indicates a problem while retrieving the value for a named parameter.
 *
 * @author Tobias Brieden
 */
public class ParameterException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 4464725428196561861L;

    public ParameterException(String message) {
        super(message);
    }
}
