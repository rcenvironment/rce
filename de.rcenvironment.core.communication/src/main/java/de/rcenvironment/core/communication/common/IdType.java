/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
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
