/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.network.internal;

import java.util.Objects;

import de.rcenvironment.core.communication.uplink.common.internal.MessageType;
import de.rcenvironment.core.communication.uplink.network.api.MessageBlockPriority;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * A wrapper representing a protocol message block.
 *
 * @author Robert Mischke
 */
public class MessageBlock {

    private final MessageType type;

    private final byte[] data;


    public MessageBlock(MessageType type, byte[] data) throws ProtocolException {
        this.type = type;
        this.data = Objects.requireNonNull(data);
        // check after null test
        if (data.length > UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH) {
            throw new ProtocolException(
                StringUtils.format("The message data block of %d bytes exceeds the maximum of %d bytes", data.length,
                    UplinkProtocolConstants.MAX_MESSAGE_BLOCK_DATA_LENGTH));
        }
    }

    public MessageBlock(int type, byte[] data) throws ProtocolException {
        this(MessageType.resolve((byte) type), data);
    }

    /**
     * Constructor for a no-content "signal" message.
     * 
     * @param messageType the {@link MessageBlock} type to generate
     * @throws ProtocolException on encoding error (should never happen)
     */
    public MessageBlock(MessageType messageType) throws ProtocolException {
        this(messageType.getCode(), new byte[0]);
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return data.length;
    }

}
