/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.configuration.bootstrap.profile;


/**
 * Indicates a problem during usage of the {@link Profile} class.
 *
 * @author Tobias Brieden
 */
public class ProfileException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 9180417576456620838L;

    public ProfileException(String message) {
        super(message);
    }
    
    public ProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
