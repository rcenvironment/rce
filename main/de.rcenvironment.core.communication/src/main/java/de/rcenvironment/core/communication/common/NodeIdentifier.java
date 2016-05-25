/*
 * Copyright (C) 2006-2016 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import java.io.Serializable;

/**
 * A minimal interface that provides a unique identifier for a network node.
 * 
 * This identifier is expected to be "reasonably unique", on a level comparable to a version 4 UUID
 * (see RFC 4122). It is expected to be stable as long as the identified node is connected to the
 * RCE network. Its validity (or invalidity) after a restart and/or reconnect of the node is
 * undefined and should not be relied upon.
 * 
 * @author Robert Mischke
 */
public interface NodeIdentifier extends Serializable {

    /**
     * @return the unique identifier of this network node; refer to the {@link NodeIdentifier}
     *         JavaDoc for details
     */
    String getIdString();

    /**
     * Convenience method for acquiring the display name associated with the identified node.
     * 
     * @return the display name associated with the node, or null if no display name is
     *         available/known
     */
    String getAssociatedDisplayName();

    /**
     * Creates a new {@link NodeIdentifier} with the same node id.
     * 
     * @return the new instance
     */
    NodeIdentifier clone();

}
