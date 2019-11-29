/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.model;

import java.io.Serializable;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.transport.spi.MessageChannel;

/**
 * This class represents the minimal initial information that is exchanged between two nodes when a {@link MessageChannel} is established
 * between them. Note that all non-essential public information should be transported via the node properties mechanism instead, which also
 * automatically distributes that information to other nodes in the network.
 * 
 * @author Robert Mischke
 */
public interface InitialNodeInformation extends Serializable {

    /**
     * @return the unique node identifier string
     */
    String getInstanceNodeSessionIdString();

    /**
     * Convenience method that returns the node id as a {@link InstanceNodeSessionId}. This method may or may not return the same object on
     * repeated calls.
     * 
     * @return the wrapped unique node identifier
     */
    InstanceNodeSessionId getInstanceNodeSessionId();

    /**
     * @return the assigned name for this node
     */
    String getDisplayName();

    /**
     * @return the description text to use in log output
     */
    String getLogDescription();
}
