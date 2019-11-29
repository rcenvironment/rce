/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
     * The supported version (or list of versions) for the remote access protocol; used to determine client compatibility.
     */
    public static final String PROTOCOL_VERSION_STRING = "10.0.0";
    
    /**
     * Default group name for SSH workflow components.
     */
    public static final String DEFAULT_GROUP_NAME_WFS = "SSH Remote Access Workflows";

    private RemoteAccessConstants() {}
}
