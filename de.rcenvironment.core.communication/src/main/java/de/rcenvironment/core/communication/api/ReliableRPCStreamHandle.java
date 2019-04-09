/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.LogicalNodeSessionId;
import de.rcenvironment.core.communication.common.NetworkDestination;

/**
 * Represents a "Reliable RPC Stream" on the RPC caller's side.
 * 
 * Note that this handle is only supposed to be used on the request sending node, where the stream should also have been initialized from.
 * It is not supposed to be sent over the network, and is therefore intentionally not Serializable.
 *
 * @author Robert Mischke
 */
public interface ReliableRPCStreamHandle extends NetworkDestination {

    /**
     * @return the logical node session to which the RPC stream has been established
     */
    LogicalNodeSessionId getDestinationNodeId();

    /**
     * @return the stream's id, which is supposed to be unique for the target {@link LogicalNodeSessionId}; it is important to not treat it
     *         as globally unique, as this would most likely cause stream mix-ups with potentially confusing results
     */
    String getStreamId();

}
