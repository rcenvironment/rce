/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
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
