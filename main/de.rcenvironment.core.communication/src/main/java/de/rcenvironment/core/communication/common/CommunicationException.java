/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * This exception will be thrown if a communication fails.
 * 
 * @author Doreen Seider
 */
public class CommunicationException extends Exception {

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
    public CommunicationException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause The cause for this exception.
     */
    public CommunicationException(Throwable cause) {
        super(cause);
    }

    /**
     * 
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public CommunicationException(String string, Throwable cause) {
        super(string, cause);
    }

}
