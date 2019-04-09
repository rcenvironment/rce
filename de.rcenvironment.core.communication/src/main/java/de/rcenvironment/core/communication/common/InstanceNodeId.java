/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * An identifier of an abstract instance in a (potentially decentralized) network. {@link InstanceNodeId} are typically persistent, but are
 * not required to be.
 * <p>
 * In an {@link InstanceNodeId}, the "instance" part is always defined and {@link CommonIdBase#INSTANCE_PART_LENGTH} characters long, while
 * the "session" and "logical node" parts are always null.
 * 
 * @author Robert Mischke
 */
public interface InstanceNodeId extends CommonIdBase, ResolvableNodeId {

    /**
     * @return the default {@link LogicalNodeId} for this instance session; may or may not return the same object on repeated calls
     */
    LogicalNodeId convertToDefaultLogicalNodeId();

    /**
     * @return a new {@link LogicalNodeId} with the same instance id part as this {@link InstanceNodeId}, and the given logical node part.
     * @param nodeIdPart the node id part to use
     */
    LogicalNodeId expandToLogicalNodeId(String nodeIdPart);
}
