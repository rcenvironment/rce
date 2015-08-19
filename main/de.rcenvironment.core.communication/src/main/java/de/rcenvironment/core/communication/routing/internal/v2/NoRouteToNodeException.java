/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import de.rcenvironment.core.communication.common.NodeIdentifier;

/**
 * Exception class for routing attempts to unreachable nodes.
 * 
 * @author Robert Mischke
 */
public final class NoRouteToNodeException extends Exception {

    private final NodeIdentifier targetNodeId;

    public NoRouteToNodeException(String message, NodeIdentifier targetNodeId) {
        super(message);
        this.targetNodeId = targetNodeId;
    }

    public NodeIdentifier getTargetNodeId() {
        return targetNodeId;
    }
}
