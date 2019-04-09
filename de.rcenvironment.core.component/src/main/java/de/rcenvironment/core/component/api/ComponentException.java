/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.api;

import de.rcenvironment.core.component.execution.api.Component;


/**
 * This exception will be thrown if an error within a {@link Component} occurred.
 * 
 * @author Doreen Seider
 */
public class ComponentException extends Exception {

    /**
     * Serial version identifier.
     */
    private static final long serialVersionUID = 2911996501788218615L;

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public ComponentException(String string) {
        super(string);
    }

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     * @param cause The cause of this exception
     */
    public ComponentException(String string, Throwable cause) {
        super(string, cause);
    }

}
