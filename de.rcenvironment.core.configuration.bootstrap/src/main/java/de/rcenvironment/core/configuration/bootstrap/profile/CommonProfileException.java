/*
 * Copyright (C) 2006-2017 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.bootstrap.profile;

/**
 * Indicates a problem while handling information stored in the common profile.
 *
 * @author Tobias Brieden
 */
public class CommonProfileException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 7610640953966071986L;

    public CommonProfileException(String message) {
        super(message);
    }
    
    public CommonProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
