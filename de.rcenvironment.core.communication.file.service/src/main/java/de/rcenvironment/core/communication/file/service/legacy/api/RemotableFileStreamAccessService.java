/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.api;

import java.io.IOException;

import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;
import de.rcenvironment.core.utils.common.rpc.RemotableService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Service for accessing files either existing in the RCE data management or in the file system.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 * @author Robert Mischke (adapted for 7.0.0)
 */
@Deprecated
@RemotableService
public interface RemotableFileStreamAccessService {

    /**
     * Opens a file.
     * 
     * @param type The type of the file. (rce://host/instance/dataReference)
     * @param file The URI of the file. (rce://host/instance/dataReference)
     * @return The UUID representing the open remote input stream.
     * @throws IOException if an I/O error occurs.
     * @throws RemoteOperationException standard remote operation exception
     * @see java.io.InputStream#open()
     */
    String open(FileType type, String file) throws IOException, RemoteOperationException;

    /**
     * Reads from a file.
     * 
     * @param uuid The id of the file.
     * @return the next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException if an I/O error occurs.
     * @throws RemoteOperationException standard remote operation exception
     * 
     * @see java.io.InputStream#read()
     */
    int read(String uuid) throws IOException, RemoteOperationException;

    /**
     * Reads from a file.
     * 
     * @param uuid The id of the file.
     * @param len the maximum number of bytes to read.
     * @return the buffer into which the data is read.
     * @throws IOException - If the first byte cannot be read for any reason other than end of file, or if the input stream has been closed,
     *         or if some other I/O error occurs.
     * @throws RemoteOperationException standard remote operation exception
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    byte[] read(String uuid, Integer len) throws IOException, RemoteOperationException;

    /**
     * Skips over and discards bytes from a file.
     * 
     * @param uuid the id of the file.
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException - if the stream does not support seek, or if some other I/O error occurs.
     * @throws RemoteOperationException standard remote operation exception
     * 
     * @see java.io.InputStream#skip(long)
     */
    long skip(String uuid, Long n) throws IOException, RemoteOperationException;

    /**
     * Closes a file.
     * 
     * @param uuid the id of the file.
     * @throws IOException - if an I/O error occurs.
     * @throws RemoteOperationException standard remote operation exception
     * 
     * @see java.io.InputStream#close()
     */
    void close(String uuid) throws IOException, RemoteOperationException;
}
