/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

/**
 * Represents a link (an edge) in a {@link NetworkGraph}.
 * 
 * @author Robert Mischke
 */
public interface NetworkGraphLink {

    /**
     * @return the string if of this link; in the standard implementation, this is equal to the id of the underlying {@link MessageChannel}
     */
    String getLinkId();

    /**
     * @return the id of the graph node this link (edge) originates from
     */
    InstanceNodeSessionId getSourceNodeId();

    /**
     * @return the id of the graph node this link (edge) points to
     */
    InstanceNodeSessionId getTargetNodeId();
}
