/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * Exception class for errors during serialization or deserialization.
 * 
 * @author Robert Mischke
 */
public class SerializationException extends Exception {

    private static final long serialVersionUID = 6985871603095457656L;

    public SerializationException(Throwable ex) {
        super(ex);
    }

    public SerializationException(String msg) {
        super(msg);
    }

}
