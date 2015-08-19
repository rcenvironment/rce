/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.incubator.xml;

/**
 * This exception will be thrown if an error in handling XML files occurred.
 * 
 * @author Jan Flink
 */
public class XMLException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -7359128994213505980L;

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public XMLException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause The cause for this exception.
     */
    public XMLException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public XMLException(String string, Throwable cause) {
        super(string, cause);
    }
}
