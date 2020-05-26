/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;


/**
 * This exception will be thrown if execution of a component failed.
 * 
 * @author Doreen Seider
 */
public class ComponentExecutionException extends Exception {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 2911996501788218615L;

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public ComponentExecutionException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param cause The cause for this exception.
     */
    public ComponentExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public ComponentExecutionException(String string, Throwable cause) {
        super(string, cause);
    }

}
