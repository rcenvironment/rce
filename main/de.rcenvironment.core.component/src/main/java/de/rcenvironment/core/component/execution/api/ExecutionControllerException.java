/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;


/**
 * This exception will be thrown if calling an {@link ExecutionController} failed.
 * 
 * @author Doreen Seider
 */
public class ExecutionControllerException extends Exception {

    private static final long serialVersionUID = 1683951052083323625L;

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public ExecutionControllerException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause The cause for this exception.
     */
    public ExecutionControllerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public ExecutionControllerException(String string, Throwable cause) {
        super(string, cause);
    }

}
