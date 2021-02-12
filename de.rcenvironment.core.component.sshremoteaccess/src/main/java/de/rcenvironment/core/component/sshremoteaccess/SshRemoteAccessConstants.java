/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.sshremoteaccess;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for SSH remote access.
 *
 * @author Brigitte Boden
 */
public final class SshRemoteAccessConstants {
    
    /**
     * Component id.
     */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "remoteaccess";

    /**
     * Name of the tool.
     */
    public static final String KEY_TOOL_NAME = "toolName";
    
    /**
     * Whether the component represents a remote workflow.
     */
    public static final String KEY_IS_WORKFLOW = "isWorkflow";
    
    /**
     * Version of the tool.
     */
    public static final String KEY_TOOL_VERSION = "version";

    /**
     * SSH connection over which this tool can be accessed.
     */
    public static final String KEY_CONNECTION = "connection";
    
    /**
     * Id of the host publishing this tool.
     */
    public static final String KEY_HOST_ID = "hostId";
    
    /**
     * Name of the host publishing this tool.
     */
    public static final String KEY_HOST_NAME = "hostName";
    
    /**
     * Name of component output (directory).
     */
    public static final String INPUT_NAME_SHORT_TEXT = "input_text";
    
    /**
     * Name of component output (directory).
     */
    public static final String INPUT_NAME_DIRECTORY = "input_directory";
    
    /**
     * Name of component output (directory).
     */
    public static final String OUTPUT_NAME = "output";
    
    //Keys for endpoint definitions
    /**
     * Key for endpoint definitions.
     */
    public static final String KEY_ENDPOINT_NAME = "name";
    
    /**
     * Key for endpoint definitions.
     */
    public static final String KEY_ENDPOINT_DATA_TYPE = "dataType";
    
    /**
     * Kes for key handling options.
     */
    public static final String KEY_INPUT_HANDLINGS = "inputHandlingOptions";
    
    /**
     * Key for default input handling.
     */
    public static final String KEY_DEFAULT_INPUT_HANDLING = "defaultInputHandling";
    
    /**
     * Key for input execution constraint options.
     */
    public static final String KEY_INPUT_EXEC_CONSTRAINTS = "inputExecutionConstraintOptions";
    
    /**
     * Key for default execution constraint.
     */
    public static final String KEY_DEFAULT_INPUT_EXEC_CONSTRAINT = "defaultInputExecutionConstraint";
    
    /**
     * Key for endpoint meta data.
     */
    public static final String KEY_ENDPOINT_META_DATA = "metaData";
    
    private SshRemoteAccessConstants(){}

}
