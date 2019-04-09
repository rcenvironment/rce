/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * An identifier representing a session (an uninterrupted run maintaining its internal state) of a logical node (see {@link LogicalNodeId}).
 * <p>
 * For a {@link LogicalNodeSessionId}, all three id parts (instance, logical node, and session) are guaranteed to be defined. The instance
 * and session parts are {@link CommonIdBase#INSTANCE_PART_LENGTH} and {@link CommonIdBase#SESSION_PART_LENGTH} characters long,
 * respectively. The logical node part can be at most {@link CommonIdBase#MAXIMUM_LOGICAL_NODE_PART_LENGTH} characters long; note that
 * {@link CommonIdBase#DEFAULT_LOGICAL_NODE_PART} has a special meaning, and must not be used accidentally.
 * <p>
 * Note that currently, all session parts of an instance's ids are guaranteed to be equal to allow simple conversion between id types. This
 * concept may be changed or expanded in the future to allow logical nodes to logically "restart" without the actual instance restarting.
 * 
 * @author Robert Mischke
 */
public interface LogicalNodeSessionId extends CommonIdBase, ResolvableNodeId {

    /**
     * @return the string form of the session id part; see the main {@link CommonIdBase} JavaDoc for its description
     */
    String getSessionIdPart();

    /**
     * @return the string form of the logical node part; see the main {@link CommonIdBase} JavaDoc for its description
     */
    String getLogicalNodePart();

    /**
     * @return the string portion of the logical node part intended for recognizing logical nodes across instance restarts, if present;
     *         null, if this session id belongs to a default or a transient logical node id.
     */
    String getLogicalNodeRecognitionPart();

    /**
     * @return the string form representing this {@link LogicalNodeSessionId}
     */
    String getLogicalNodeSessionIdString();

    /**
     * @return the {@link InstanceNodeSessionId} this {@link LogicalNodeSessionId} is running "within" - this relies on the fact, that
     *         currently all session id parts of an instance's is are guaranteed to be equal, which makes this conversion possible without
     *         any kind of resolution
     */
    InstanceNodeSessionId convertToInstanceNodeSessionId();

    /**
     * @return the {@link LogicalNodeId} of this {@link LogicalNodeSessionId}
     */
    LogicalNodeId convertToLogicalNodeId();

    /**
     * @return true if this id is "transient", ie not expected to be recognizable after a restart; typically, transient logical node ids do
     *         not have individually assigned display names
     */
    boolean isTransientLogicalNode();

    /**
     * @param otherId the id to compare with; currently only supports {@link InstanceNodeSessionId}, but may be expanded as needed
     * @return true if this id refers to the same instance session (same instance and session id parts) as the provided id
     */
    boolean isSameInstanceNodeSessionAs(InstanceNodeSessionId otherId);

    /**
     * @return the string form of related logical node; equivalent to {@link LogicalNodeId#getFullIdString()}
     */
    // TODO implement once actually needed
    // String getLogicalNodeIdString();
}
