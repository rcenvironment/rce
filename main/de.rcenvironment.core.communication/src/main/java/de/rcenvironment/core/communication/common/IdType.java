/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * An enum representing the different identifier types that can be represented by subinterfaces and subclasses of {@link CommonIdBase}.
 * 
 * @author Robert Mischke
 */
public enum IdType {
    /**
     * See {@link InstanceNodeId}.
     */
    INSTANCE_NODE_ID,
    /**
     * See {@link InstanceNodeSessionId}.
     */
    INSTANCE_NODE_SESSION_ID,
    /**
     * See {@link LogicalNodeId}.
     */
    LOGICAL_NODE_ID,
    /**
     * See {@link LogicalNodeSessionId}.
     */
    LOGICAL_NODE_SESSION_ID
}
