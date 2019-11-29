/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import de.rcenvironment.core.communication.uplink.network.api.AsyncMessageBlockSender;
import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.communication.uplink.network.internal.UplinkProtocolConstants;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * A wrapper encapsulating the splitting of local known-length byte stream sources into {@link MessageBlock}s, and forwarding them to a
 * {@link AsyncMessageBlockSender}. Currently, each wrapper instance <em>is</em> reusable; however, for future-proofing, it is recommended
 * to use a new instance for each upload.
 *
 * @author Robert Mischke
 */
public class DataStreamUploadWrapper {

    private final AsyncMessageBlockSender messageBlockSender;

    public DataStreamUploadWrapper(AsyncMessageBlockSender messageBlockSender) {
        this.messageBlockSender = messageBlockSender;
    }

    /**
     * Reads all data from the provided {@link SizeValidatedDataSource}, converts them to a sequence of {@link MessageBlock}s of the given
     * type, and passes them to the {@link AsyncMessageBlockSender} passed to the constructor.
     * 
     * @param channelId the channel id to write into the generated {@link MessageBlock}s
     * @param messageType the {@link MessageType} to use for the generated {@link MessageBlock}s
     * @param dataSource the source to read the data from
     * @throws IOException on I/O or encoding protocol errors
     */
    public void uploadFromDataSource(long channelId, MessageType messageType, SizeValidatedDataSource dataSource) throws IOException {
        while (!dataSource.receivedCompletely()) {
            final MessageBlock nextChunk = encodeNextDataBlock(messageType, dataSource);
            messageBlockSender.enqueueMessageBlockForSending(channelId, nextChunk);
        }
    }

    private MessageBlock encodeNextDataBlock(MessageType messageType, SizeValidatedDataSource dataSource)
        throws IOException {
        int bufferSize = (int) Math.min(dataSource.getRemaining(), UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH);
        if (bufferSize < 1) {
            throw new ProtocolException("Attempted to create a message block from an already-finished data stream");
        }
        byte[] buffer = new byte[bufferSize];
        // TODO check timeout handling
        IOUtils.readFully(dataSource.getStream(), buffer);
        return new MessageBlock(messageType.getCode(), buffer);
    }
}
