/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import de.rcenvironment.core.communication.uplink.network.internal.MessageBlock;
import de.rcenvironment.core.utils.common.SizeValidatedDataSource;
import de.rcenvironment.core.utils.common.exception.ProtocolException;

/**
 * Encapsulates the reconstruction of a data stream from a sequence of {@link MessageBlock}s. As instance of this class are stateful, they
 * are <em>not</em> designed to be reusable.
 * 
 * @param <T> the type of the returned wrapper, which must be a {@link SizeValidatedDataSource} or a subclass.
 * 
 * @author Robert Mischke
 */
public abstract class DataStreamDownloadWrapper<T extends SizeValidatedDataSource> {

    private long totalSize;

    private long received;

    private PipedOutputStream localOutputStream;

    private MessageType expectedMessageBlockType;

    /**
     * Initializes the download process once the expected size of the stream is known (typically, from some sort of header or metadata
     * {@link MessageBlock}).
     * 
     * @param size the expected data size in bytes
     * @param messageBlockType the expected type of the following data {@link MessageBlock}s
     * @return the {@link SizeValidatedDataSource} providing size information and an {@link InputStream} from which the received data will
     *         become incrementally available
     * @throws IOException on I/O or {@link ProtocolException}s
     */
    public final synchronized T initialize(long size, MessageType messageBlockType) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (this.totalSize > 0) {
            // note: this does not catch the case of size == 0, but as there is no real state in that case, it does not matter
            throw new IllegalStateException("Already initialized");
        }
        this.totalSize = size;
        this.localOutputStream = new PipedOutputStream();
        this.expectedMessageBlockType = messageBlockType;
        final PipedInputStream inputStream = new PipedInputStream(localOutputStream);
        if (size == 0) {
            // finish immediately, as no data block will be received
            localOutputStream.close();
        }
        return createReturnObject(size, inputStream);
    }

    /**
     * @param input the next received {@link MessageBlock} to process
     * @return true if the processed {@link MessageBlock} completed the expected total size
     * @throws IOException in I/O or {@link ProtocolException}s
     */
    public final synchronized boolean processMessageBlock(MessageBlock input) throws IOException {
        if (input.getType() != expectedMessageBlockType) {
            throw new ProtocolException("Expected message type " + expectedMessageBlockType + " but received type " + input.getType());
        }
        received += input.getDataLength();
        if (received > totalSize) {
            throw new ProtocolException(
                "Expected a total of " + totalSize + " bytes, but the last message's length added up to " + received);
        }
        localOutputStream.write(input.getData());
        final boolean complete = received == totalSize;
        if (complete) {
            localOutputStream.close();
        }
        return complete;
    }

    /**
     * Defines the creation of the returned wrapper.
     * 
     * @param size the data stream size/length
     * @param inputStream the input stream to wrap
     * @return the wrapper object
     */
    public abstract T createReturnObject(long size, PipedInputStream inputStream);
}
