/*
 * Copyright 2006-2020 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.configuration.internal;

import java.io.File;
import java.io.IOException;

import de.rcenvironment.core.configuration.ConfigurationException;
import de.rcenvironment.core.configuration.ConfigurationSegment;

/**
 * Internal service for handling hierarchical configuration data. Data is read as a single snapshot. To change the configuration, modify the
 * snapshot and pass it to the update() method.
 * 
 * @author Robert Mischke
 */
public interface ConfigurationStore {

    /**
     * Retrieves a snapshot of the current configuration.
     * 
     * @return the snapshot
     * @throws IOException on I/O errors (file reading or parsing)
     */
    ConfigurationSegment getSnapshotOfRootSegment() throws IOException;

    /**
     * Creates an empty configuration segment, typically as a placeholder for a broken configuration file.
     * 
     * @return the empty {@link ConfigurationSegment}
     */
    ConfigurationSegment createEmptyPlaceholder();

    /**
     * Updates the current configuration with a complete snapshot or a sub-segment of one.
     * 
     * @param configuration the part of a previously loaded snapshot to merge and write
     * @throws ConfigurationException on errors in the configuration data
     * @throws IOException on I/O errors (general disk error or write conflict)
     */
    void update(ConfigurationSegment configuration) throws ConfigurationException, IOException;

    /**
     * Writes/exports the given {@link ConfigurationSegment} to a separate file.
     * 
     * @param configurationSegment the segment to write
     * @param destination the file to write to
     * @throws IOException on general I/O errors
     */
    void exportToFile(ConfigurationSegment configurationSegment, File destination) throws IOException;

}
