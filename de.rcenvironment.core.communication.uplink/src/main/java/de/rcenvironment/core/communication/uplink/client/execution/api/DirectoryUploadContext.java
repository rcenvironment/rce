/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.IOException;

/**
 * Provides an abstract context for uploading one or more files as data streams.
 *
 * @author Robert Mischke
 */
public interface DirectoryUploadContext {

    /**
     * Uploads a single file as a data stream. This method will block until the stream has either been fully consumed, or an error occurs.
     * The caller of this method creates/opens the stream, and the receiving implementation is responsible for closing it. Nonetheless, the
     * calling code may still wrap the stream in safeguards, either for defensive coding, or to satisfy static code checks.
     * 
     * @param dataSource the {@link FileDataSource} representing the file's relative path, size, and data stream
     * 
     * @throws IOException on upload failure
     */
    void provideFile(FileDataSource dataSource) throws IOException;
}
