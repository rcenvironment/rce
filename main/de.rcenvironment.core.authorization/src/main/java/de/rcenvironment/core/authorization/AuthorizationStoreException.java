/*
 * Copyright (C) 2006-2014 DLR, Germany, 2006-2010 Fraunhofer SCAI, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.authorization;

/**
 * This exception will be thrown if accessing the authorization store fails.
 * 
 * @author Doreen Seider
 */
public class AuthorizationStoreException extends Exception {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 2911996501788218615L;

    /**
     * 
     * Creates an instance of this exception.
     * 
     * @param string
     *            A text message describing the error.
     */
    public AuthorizationStoreException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause
     *            The cause for this exception.
     */
    public AuthorizationStoreException(Throwable cause) {
        super(cause);
    }

    /**
     * 
     * Creates an instance of this exception.
     * 
     * @param string
     *            A text message describing the error.
     * @param cause
     *            The cause of this exception
     */
    public AuthorizationStoreException(String string, Throwable cause) {
        super(string, cause);
    }

}
