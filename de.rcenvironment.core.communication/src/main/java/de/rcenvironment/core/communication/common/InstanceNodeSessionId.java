/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * An identifier representing a session (an uninterrupted run maintaining its internal state) of an instance.
 * <p>
 * In an {@link InstanceNodeSessionId}, the instance and session parts are always defined (ie not null and
 * {@link CommonIdBase#INSTANCE_PART_LENGTH} and {@link CommonIdBase#SESSION_PART_LENGTH} characters long, respectively). The "logical node
 * part" is always null.
 * 
 * @author Robert Mischke
 */
public interface InstanceNodeSessionId extends CommonIdBase, ResolvableNodeId {

    /**
     * @return the string form of the session id part; see the main {@link CommonIdBase} JavaDoc for its description
     */
    String getSessionIdPart();

    /**
     * @return the string form of the instance session id, derived from the instance id and the session id part; see the main JavaDoc for
     *         its description
     */
    String getInstanceNodeSessionIdString();

    /**
     * @param id the id to compare with
     * @return true if both ids refer to the same instance session
     */
    boolean isSameInstanceNodeSessionAs(InstanceNodeSessionId id);

    /**
     * @return the {@link InstanceNodeId} of this instance session; may or may not return the same object on repeated calls
     */
    InstanceNodeId convertToInstanceNodeId();

    /**
     * @return the default {@link LogicalNodeId} for this instance session; may or may not return the same object on repeated calls
     */
    LogicalNodeId convertToDefaultLogicalNodeId();

    /**
     * @return the default {@link LogicalNodeSessionId} for this instance session; may or may not return the same object on repeated calls
     */
    LogicalNodeSessionId convertToDefaultLogicalNodeSessionId();

    /**
     * @return a new {@link LogicalNodeSessionId} with the same instance id and session parts as this {@link InstanceNodeSessionId}, and the
     *         given logical node part.
     * @param nodeIdPart the node id part to use
     */
    LogicalNodeSessionId expandToLogicalNodeSessionId(String nodeIdPart);

}
