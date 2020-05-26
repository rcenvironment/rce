/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.common;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;

/**
 * A marker interface to allow conceptually different "network destinations" to be used in method parameters and fields. The two current
 * subinterfaces are {@link ResolvableNodeId}, which is the base class of all network ids, and which are used for traditional non-reliable
 * messaging; and {@link ReliableRPCStreamHandle}, which are used for reliable single-delivery RPC calls instead.
 * 
 * @author Robert Mischke
 * 
 * @since 9.0.0
 */
public interface NetworkDestination {

}
