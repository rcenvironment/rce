/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.component.toolaccess.api;

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
    public static final String PROTOCOL_VERSION = "6.3.0";

    private RemoteAccessConstants() {}
}
