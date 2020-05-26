/*
 * Copyright 2019-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.InputStream;

import de.rcenvironment.core.utils.common.SizeValidatedDataSource;

/**
 * Represents a data stream with associated file path and size metadata. As a general contract, receivers of such an object SHOULD close the
 * data stream after reading from it, even if an error occurs while processing it. Infrastructure code, however, SHOULD NOT take this
 * behavior as guaranteed. For streams that are not specific to files, for example when no name/path association is needed or appropriate,
 * the generic {@link SizeValidatedDataSource} superclass should be used instead.
 * <p>
 * IMPORTANT: Clients reading from such a stream MUST either verify that they have actually read {@link #getSize()} bytes from the stream,
 * or that #receivedCompletely() is true after reading all of the stream's content (which performs the same check internally for
 * convenience).
 * <p>
 * The design rationale behind is this that clients may start reading from the data stream before it is known to be complete, and the
 * {@link InputStream} API does not provide a proper way to distinguish between normal and abnormal stream termination.
 *
 * @author Robert Mischke
 */
public class FileDataSource extends SizeValidatedDataSource {

    private final String relativePath;

    /**
     * @param relativePath the relative path of this file without a leading slash; sub-directories should be indicated using forward slashes
     *        (e.g. "subdir/file.txt")
     * @param size the size of the data stream, in bytes
     * @param dataStream the data stream; should be closed by this method
     */
    public FileDataSource(String relativePath, long size, InputStream dataStream) {
        super(size, dataStream);
        this.relativePath = relativePath;
    }

    public String getRelativePath() {
        return relativePath;
    }
}
