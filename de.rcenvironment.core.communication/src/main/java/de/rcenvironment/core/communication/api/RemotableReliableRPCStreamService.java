/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Remote call interface for creating and disposing Reliable RPC Streams from other nodes, with the local node being the target/receiver of
 * the transmitted RPCs.
 *
 * @author Robert Mischke
 */
@RemotableService
public interface RemotableReliableRPCStreamService {

    /**
     * Creates a Reliable RPC (rRPC) Stream on the local node; the initiating node will be the sending and the local node the receiving
     * side.
     * 
     * @return the id of the generated stream
     * @throws RemoteOperationException on general network errors
     */
    String createReliableRPCStream() throws RemoteOperationException;

    /**
     * Closes/disposes a stream when it is no longer needed by the sending remote node.
     * 
     * @param streamId the previously assigned id of the stream to dispose
     * @throws RemoteOperationException on general network errors
     */
    void disposeReliableRPCStream(String streamId) throws RemoteOperationException;

}
