/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.remoteaccess.common;

/**
 * Constants used by "remote access".
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public final class RemoteAccessConstants {

    /** Placeholder for an input directory. */
    public static final String WF_PLACEHOLDER_INPUT_DIR = "##RUNTIME_INPUT_DIRECTORY##";

    /**
     * The version of the remote access protocol; used to determine client compatibility.
     */
    public static final String PROTOCOL_VERSION = "7.0.0";

    private RemoteAccessConstants() {}
}
