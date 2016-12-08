/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.routing.internal.v2;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;

/**
 * Exception class for routing attempts to unreachable nodes.
 * 
 * @author Robert Mischke
 */
public final class NoRouteToNodeException extends Exception {

    private static final long serialVersionUID = 3448576774973336658L;

    private final InstanceNodeSessionId targetNodeId;

    public NoRouteToNodeException(String message, InstanceNodeSessionId targetNodeId) {
        super(message);
        this.targetNodeId = targetNodeId;
    }

    public InstanceNodeSessionId getTargetNodeId() {
        return targetNodeId;
    }
}
