/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.utils.ssh.jsch;

/**
 * A simple exception class for cases where provided SSH parameters are invalid.
 * 
 * @author Robert Mischke
 */
public class SshParameterException extends Exception {

    private static final long serialVersionUID = -9189903496226228821L;

    public SshParameterException(String message) {
        super(message);
    }

}
