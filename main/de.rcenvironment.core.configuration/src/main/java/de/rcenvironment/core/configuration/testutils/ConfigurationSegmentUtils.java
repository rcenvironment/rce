/*
 * Copyright (C) 2006-2014 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.configuration.testutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import de.rcenvironment.core.configuration.ConfigurationSegment;
import de.rcenvironment.core.configuration.internal.ConfigurationStore;
import de.rcenvironment.core.configuration.internal.ConfigurationStoreImpl;
import de.rcenvironment.core.utils.common.TempFileServiceAccess;

/**
 * Common utilities for internal {@link ConfigurationSegment} handling.
 * 
 * @author Robert Mischke
 */
public final class ConfigurationSegmentUtils {

    private ConfigurationSegmentUtils() {}

    /**
     * Creates an empty configuration object, equal to the return value of {@link ConfigurationStore#createEmptyPlaceholder()}.
     * 
     * @return the configuration object
     */
    public static ConfigurationSegment createEmptySegment() {
        return new ConfigurationStoreImpl(null).createEmptyPlaceholder();
    }

    /**
     * Reads a JSON configuration file.
     * 
     * @param file the file to read
     * @return the configuration object
     * @throws IOException on uncaught errors
     */
    public static ConfigurationSegment readTestConfigurationFile(File file) throws IOException {
        return new ConfigurationStoreImpl(file).getSnapshotOfRootSegment();
    }

    /**
     * Reads a JSON configuration file created from the stream's data.
     * 
     * @param is the stream providing the test file's data
     * @return the configuration object
     * @throws IOException on uncaught errors
     */
    public static ConfigurationSegment readTestConfigurationFromStream(InputStream is) throws IOException {
        if (is == null) {
            throw new IOException("InputStream is null - most likely, a test resource was not found");
        }
        File tempFile = TempFileServiceAccess.getInstance().createTempFileFromPattern("*.json");
        FileUtils.copyInputStreamToFile(is, tempFile);
        return new ConfigurationStoreImpl(tempFile).getSnapshotOfRootSegment();
    }

}
