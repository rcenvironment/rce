/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;

/**
 * Wraps an {@link InputStream} with additional size information, and provides a convenience method to verify that after reading the whole
 * stream, the number of bytes read was actually the intended size defined at the data stream's origin. Note that the full size of the data
 * must be known before creating a {@link SizeValidatedDataSource} instance.
 * <p>
 * IMPORTANT: Clients reading from such a stream MUST either verify that they have actually read {@link #getSize()} bytes from the stream,
 * or that #receivedCompletely() is true after reading all of the stream's content (which performs the same check internally for
 * convenience).
 * <p>
 * The design rationale behind this is that clients may start reading from a data stream before all data is known to become available (e.g.
 * when reading from remote systems), but the {@link InputStream} API does not provide a proper way to distinguish between normal and
 * abnormal stream termination.
 *
 * @author Robert Mischke
 */
public class SizeValidatedDataSource {

    protected final long size;

    protected final CountingInputStream dataStream;

    public SizeValidatedDataSource(long size, InputStream dataStream) {
        this.size = size;
        this.dataStream = new CountingInputStream(dataStream);
    }

    /**
     * Convenience method to construct a {@link SizeValidatedDataSource} from a byte array. Note that the byte array is not copied but
     * wrapped as-is; it should not be modified while the stream may be read.
     * 
     * @param data the byte array to provides as a {@link SizeValidatedDataSource}
     */
    public SizeValidatedDataSource(byte[] data) {
        this(data.length, new ByteArrayInputStream(data));
    }

    public long getSize() {
        return size;
    }

    public InputStream getStream() {
        return dataStream;
    }

    public long getRemaining() {
        return size - dataStream.getCount();
    }

    /**
     * @return true if exactly {@link #getSize()} bytes have been read from the wrapped stream; should be ALWAYS called after
     *         reading/processing the stream to make sure it actually contained all of the intended data
     */
    public boolean receivedCompletely() {
        return dataStream.getByteCount() == size;
    }

}
