/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.uplinktoolaccess;

import de.rcenvironment.core.component.api.ComponentConstants;

/**
 * Constants for SSH remote access.
 *
 * @author Brigitte Boden
 */
public final class UplinkToolAccessConstants {
    
    /**
     * Component id.
     */
    public static final String COMPONENT_ID = ComponentConstants.COMPONENT_IDENTIFIER_PREFIX + "integration.common";

    /**
     * Name of the tool.
     */
    public static final String KEY_TOOL_ID = "toolId";
    
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
    public static final String KEY_DESTINATION_ID = "destinationId";
    
    /**
     * Name of the host publishing this tool.
     */
    public static final String KEY_HOST_NAME = "hostName";
    
    /**
     * Name of the host publishing this tool.
     */
    public static final String KEY_AUTH_GROUP_ID = "authGroupId";
    
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
    
    private UplinkToolAccessConstants(){}

}
