/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

/**
 * This exception can be thrown in case of an invalid filename.
 *
 * @author Tobias Rodehutskors
 */
public class InvalidFilenameException extends Exception {

    /**
     * Message template for the error case.
     */
    public static final String INVALID_FILENAME_MESSAGE_TEMPLATE = "The filename %s is not valid on all supported platforms.";
    
    /**
     * generated serialVersionUID.
     */
    private static final long serialVersionUID = 2419848216121450356L;

    /**
     * Creates an instance of this exception.
     * 
     * @param string A text message describing the error.
     */
    public InvalidFilenameException(String filename) {
        super(StringUtils.format(INVALID_FILENAME_MESSAGE_TEMPLATE, filename));
    }

}
