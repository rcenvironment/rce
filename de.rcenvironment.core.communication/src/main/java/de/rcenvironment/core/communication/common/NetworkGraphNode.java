/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * A disconnected representation of a network node. Changes to the actual live network state will not affect instances of this class.
 * 
 * @author Robert Mischke
 */
public interface NetworkGraphNode {

    /**
     * @return the {@link InstanceNodeSessionId} of this node
     */
    InstanceNodeSessionId getNodeId();

    /**
     * @return the display name of this node
     */
    String getDisplayName();

    /**
     * @return true if this node is the root/local node in the current network
     */
    boolean isLocalNode();
}
