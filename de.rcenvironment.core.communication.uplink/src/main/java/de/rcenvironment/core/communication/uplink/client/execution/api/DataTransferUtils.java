/*
 * Copyright 2006-2021 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.uplink.client.execution.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;

/**
 * A class containing utility methods for data upload and download.
 *
 * @author Brigitte Boden
 * @author Robert Mischke
 */
public final class DataTransferUtils {

    private static final String SLASH = "/";

    private DataTransferUtils() {}

    /**
     * Upload a directory via a DataUploadContext.
     * 
     * @param directory The directory to upload
     * @param uploadContext the upload context
     * @param remotePath The relative remote path of this file, typically "" for the root folder.
     * @throws IOException on upload error.
     */
    public static void uploadDirectory(File directory, DirectoryUploadContext uploadContext, String remotePath) throws IOException {
        final File[] files = directory.listFiles();
        if (files == null) {
            LogFactory.getLog(DataTransferUtils.class)
                .warn("Attempted to upload " + directory + ", but it does not seem to be a directory");
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                uploadDirectory(file, uploadContext, remotePath + SLASH + file.getName());
            } else {
                // TODO consider reworking this to direct streaming, ie without reading into a byte array first
                byte[] fileContentBytes = Files.readAllBytes(file.toPath());
                uploadContext.provideFile(new FileDataSource(remotePath + SLASH + file.getName(), fileContentBytes.length,
                    new ByteArrayInputStream(fileContentBytes)));
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
            LogFactory.getLog(DataTransferUtils.class)
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
}
