/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.rpc.internal;

import de.rcenvironment.core.communication.api.ReliableRPCStreamHandle;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

/**
 * Simple bean implementing {@link ReliableRPCStreamHandle}.
 *
 * @author Robert Mischke
 */
final class ReliableRPCStreamHandleImpl implements ReliableRPCStreamHandle {

    private LogicalNodeSessionId logicalNodeSessionId;

    private String streamId;

    ReliableRPCStreamHandleImpl(LogicalNodeSessionId logicalNodeSessionId, String streamId) {
        this.logicalNodeSessionId = logicalNodeSessionId;
        this.streamId = streamId;
    }

    @Override
    public String getStreamId() {
        return streamId;
    }

    @Override
    public LogicalNodeSessionId getDestinationNodeId() {
        return logicalNodeSessionId;
    }

    @Override
    public String toString() {
        return "ReliableRPCStreamHandle [nodeId=" + logicalNodeSessionId + ", streamId=" + streamId + "]";
    }

}
