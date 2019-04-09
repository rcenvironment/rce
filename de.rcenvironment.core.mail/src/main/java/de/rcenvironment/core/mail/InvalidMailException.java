/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */
 
package de.rcenvironment.core.mail;

/**
 * Indicates that a mail could not be created.
 *
 * @author Tobias Rodehutskors
 */
public class InvalidMailException extends Exception {

    /**
     * generated serialVersionUID.
     */
    private static final long serialVersionUID = -2633373635357854060L;

    public InvalidMailException(String cause) {
        super(cause);
    }
}
