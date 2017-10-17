/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * Represents a qualified logical part or aspect of an instance. A logical node may be either temporary/transient, or stable; the latter
 * case is used for situations where recognizing that part or aspect across restarts of the instance is relevant. There is always a default
 * logical node for each instance, using the {@link CommonIdBase#DEFAULT_LOGICAL_NODE_PART} logical node part.
 * <p>
 * In a {@link LogicalNodeId}, it is guaranteed that the "instance id" and the "logical node" parts are both defined (ie not null) and of
 * valid lengths ({@link CommonIdBase#INSTANCE_PART_LENGTH} and {@link CommonIdBase#SESSION_PART_LENGTH}, respectively), while the session
 * part is always null.
 * 
 * @author Robert Mischke
 */
public interface LogicalNodeId extends CommonIdBase, ResolvableNodeId {

    /**
     * @return the string form of the session id part; see the main {@link CommonIdBase} JavaDoc for its description
     */
    String getSessionIdPart();

    /**
     * @return the string form of the logical node part; see the main {@link CommonIdBase} JavaDoc for its description
     */
    String getLogicalNodePart();

    /**
     * @return the string form representing this {@link LogicalNodeId}
     */
    String getLogicalNodeIdString();

    /**
     * @return the string portion of the logical node part intended for recognizing logical nodes across instance restarts, if present;
     *         null, if this is a default or a transient logical node id.
     */
    String getLogicalNodeRecognitionPart();

    /**
     * @return the instance node id contained in this id; effectively, this simply discards the logical node part.
     */
    InstanceNodeId convertToInstanceNodeId();

    /**
     * @return the default logical node id for this id's instance; effectively, this substitutes the logical node part with the default
     *         value.
     */
    LogicalNodeId convertToDefaultLogicalNodeId();

    /**
     * Integrates the session information from an {@link InstanceNodeSessionId} into this {@link LogicalNodeId} to create a
     * {@link LogicalNodeSessionId}.
     * 
     * @param instanceSessionId the instance session object to take the session part from; note that the instance parts must be equal
     * @return the combined id object
     */
    LogicalNodeSessionId combineWithInstanceNodeSessionId(InstanceNodeSessionId instanceSessionId);

    /**
     * @return true if this id is "transient", ie not expected to be recognizable after a restart; typically, transient logical node ids do
     *         not have individually assigned display names
     */
    boolean isTransientLogicalNode();

}
