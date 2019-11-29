/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.authorization;

/**
 * This exception will be thrown if an authorization request fails.
 * 
 * @author Doreen Seider
 */
public class AuthorizationException extends RuntimeException {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 2911996501788218615L;

    /**
     * 
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public AuthorizationException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause The cause for this exception.
     */
    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    /**
     * 
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public AuthorizationException(String string, Throwable cause) {
        super(string, cause);
    }

}
