/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.DebugSettings;

/**
 * A class containing utility methods for data upload and download.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public final class DataTransferUtils {

    private static final String SLASH = "/";

    private static final boolean VERBOSE_FILE_TRANSFER_LOGGING_ENABLED = DebugSettings.getVerboseLoggingEnabled("uplink.filetransfers");

    // warn on files above 4GB - 1B
    // note that most likely, our current data management will fail before that; TODO test actual boundary sizes
    private static final long MAXIMUM_EXPECTED_FILE_SIZE = 4L * 1024 * 1024 * 1024 - 1;

    private DataTransferUtils() {}

    /**
     * Upload a directory via a DataUploadContext.
     * 
     * @param directory The directory to upload
     * @param uploadContext the upload context
     * @param remotePath The relative remote path of this file, typically "" for the root folder
     * @param logPrefix a string to prepend to all log output for association
     * @throws IOException on upload error.
     */
    public static void uploadDirectory(File directory, DirectoryUploadContext uploadContext, String remotePath, String logPrefix)
        throws IOException {
        final File[] files = directory.listFiles();
        if (files == null) {
            staticLogger().warn(logPrefix + "Attempted to upload " + directory + ", but it does not seem to be a directory");
            return;
        }

        if (VERBOSE_FILE_TRANSFER_LOGGING_ENABLED) {
            staticLogger()
                .debug(StringUtils.format("%sUploading %d file(s) found in '%s'", logPrefix, files.length, directory.toString()));
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (VERBOSE_FILE_TRANSFER_LOGGING_ENABLED) {
                    staticLogger().debug(StringUtils.format("%sRecursing into directory '%s'", logPrefix, file.toString()));
                }
                uploadDirectory(file, uploadContext, remotePath + SLASH + file.getName(), logPrefix);
            } else {
                Path path = file.toPath();
                long fileSize = Files.size(path);
                if (fileSize < 0) {
                    throw new IOException("Error determining file size of " + path.toString() + ", received " + fileSize);
                }
                if (fileSize > MAXIMUM_EXPECTED_FILE_SIZE) {
                    // log, but proceed anyway
                    staticLogger().warn("Excessive upload file size for " + path.toString() + ": " + fileSize);
                }
                if (VERBOSE_FILE_TRANSFER_LOGGING_ENABLED) {
                    staticLogger()
                        .debug(StringUtils.format("%sUploading local file '%s' of %d bytes", logPrefix, file.toString(), fileSize));
                }
                try (InputStream bufferedFileStream = new BufferedInputStream(Files.newInputStream(path))) {
                    uploadContext.provideFile(new FileDataSource(remotePath + SLASH + file.getName(), fileSize, bufferedFileStream));
                }
                if (VERBOSE_FILE_TRANSFER_LOGGING_ENABLED) {
                    staticLogger().debug(StringUtils.format("%sFinished uploading local file '%s'", logPrefix, file.toString()));
                }
            }
        }
    }

    /**
     * Provide list of directories to transfer before upload.
     * 
     * @param directory The directory to upload
     * @param listOfDirs The list to fill
     * @param remotePath The relative remote path of this file, typically "" for the root folder.
     * @throws IOException on upload error.
     */
    public static void getDirectoryListing(File directory, List<String> listOfDirs, String remotePath) throws IOException {
        final File[] files = directory.listFiles();
        if (files == null) {
            staticLogger()
                .warn("Attempted to upload " + directory + ", but it does not seem to be a directory");
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                listOfDirs.add(remotePath + SLASH + file.getName());

                getDirectoryListing(file, listOfDirs, remotePath + SLASH + file.getName());
            }
        }
    }

    /**
     * Receives a file from an AnnotatedDataStream. The file will be written to relative path given by the dataStream inside the
     * rootDirectory.
     * 
     * @param dataSource the {@link FileDataSource} to read from
     * @param rootDirectory the root directory
     * @throws IOException on download error.
     */
    public static void receiveFile(FileDataSource dataSource, File rootDirectory) throws IOException {
        String relativePath = dataSource.getRelativePath();

        if (relativePath == null) {
            throw new IOException("Relative path is null");
        }
        // Check if the path is inside the root directory to prevent path traversal attacks

        File fileToWrite = new File(rootDirectory, relativePath);

        if (fileToWrite.getCanonicalPath().startsWith(rootDirectory.getCanonicalPath())) {
            fileToWrite.getParentFile().mkdirs();
            final FileOutputStream fileStream = new FileOutputStream(fileToWrite);
            try {
                IOUtils.copy(dataSource.getStream(), fileStream);
            } finally {
                fileStream.close(); // make sure the stream is always closed
            }
            if (!dataSource.receivedCompletely()) {
                throw new IOException("Received incomplete file transfer for relative path " + relativePath);
            }
            dataSource.getStream().close();
        } else {
            throw new IOException("Relative path " + relativePath + " points to a destination output the root directory.");
        }
    }

    /**
     * Creates directories from the received list.
     * 
     * @param dirList the list of directories
     * @param rootDirectory the root directory
     * @throws IOException on download error.
     */
    public static void receiveDirectoryListing(List<String> dirList, File rootDirectory) throws IOException {

        for (String relativePath : dirList) {

            if (relativePath == null) {
                throw new IOException("Relative path is null");
            }
            // Check if the path is inside the root directory to prevent path traversal attacks

            File dirToCreate = new File(rootDirectory, relativePath);

            if (dirToCreate.getCanonicalPath().startsWith(rootDirectory.getCanonicalPath())) {
                dirToCreate.mkdir();
            } else {
                throw new IOException("Relative path " + relativePath + " points to a destination output the root directory.");
            }
        }
    }

    private static Log staticLogger() {
        return LogFactory.getLog(DataTransferUtils.class);
    }

}
