/*
 * Copyright 2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Represents the receiving end of a directory transfer.
 *
 * @author Robert Mischke
 */
public interface DirectoryDownloadReceiver {

    /**
     * Provides a list of the relative paths of all subdirectories within the transfered directory. The root folder should not be included.
     * 
     * @param relativePaths the list of relative directory paths
     * @throws IOException on I/O errors while processing
     */
    void receiveDirectoryListing(List<String> relativePaths) throws IOException;

    /**
     * This method should consume the provided {@link InputStream} and typically close it afterwards. The contained data should be provided
     * to the tool execution environment at the given relative path. If an error occurs, this method should still try to close the provided
     * stream, although the calling code should not rely on this, but implement safeguards to finally close it either way.
     * <p>
     * Design note: At the current time, overlapping stream reads are not intended and hence, not supported; instead, this method must block
     * until the provided stream has been fully processed. The minor expected performance benefit of interleaving does not justify the added
     * complexity at this point. -- misc_ro, Apr 2019
     * <p>
     * SECURITY NOTICE: When implementing this interface to write to an actual file, make sure to check that the resolved path is still
     * within the designated target area, ie make sure to prevent path traversal attacks ("../../../../overwriteSomeSystemFile").
     * 
     * @param dataSource the data stream with attached metadata; should be closed by this method
     * @throws IOException on I/O errors while receiving
     */
    void receiveFile(FileDataSource dataSource) throws IOException;
}
