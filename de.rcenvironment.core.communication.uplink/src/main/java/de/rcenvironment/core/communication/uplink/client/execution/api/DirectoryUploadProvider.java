/*
 * Copyright 2019-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.IOException;
import java.util.List;

/**
 * A callback interface that instructs the receiver to provide/upload a set of files using the provided {@link DirectoryUploadContext}.
 *
 * @author Robert Mischke
 */
public interface DirectoryUploadProvider {

    /**
     * Provides a list of the relative paths of all subdirectories within the transfered directory. The root folder should not be included.
     * 
     * @return relativePaths the list of relative directory paths
     * @throws IOException on I/O errors while collecting the list of directories
     */
    List<String> provideDirectoryListing() throws IOException;

    /**
     * Instructs the receiver to provide/upload all related files using the provided {@link DirectoryUploadContext}.
     * 
     * @param uploadContext the context to use to upload individual files
     * @throws IOException on upload failure
     */
    void provideFiles(DirectoryUploadContext uploadContext) throws IOException;

}
