/*
 * Copyright (C) 2006-2015 DLR, Germany
 * 
 * All rights reserved
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.core.communication.file.service.legacy.internal;

import java.io.IOException;

import de.rcenvironment.core.authentication.User;
import de.rcenvironment.core.communication.fileaccess.api.RemoteFileConnection.FileType;

/**
 * Service for accessing files either existing in the RCE data management or in the file system.
 * 
 * @author Heinrich Wendel
 * @author Doreen Seider
 */
@Deprecated
public interface FileService {

    /**
     * Opens a file.
     * 
     * @param cert The user's certificate.
     * @param type The type of the file. (rce://host/instance/dataReference)
     * @param file The URI of the file. (rce://host/instance/dataReference)
     * @return The UUID representing the open remote input stream.
     * @throws IOException if an I/O error occurs.
     * @see java.io.InputStream#open()
     */
    String open(User cert, FileType type, String file) throws IOException;

    /**
     * Reads from a file.
     * 
     * @param uuid
     *            The id of the file.
     * @return the next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException
     *             if an I/O error occurs.
     * 
     * @see java.io.InputStream#read()
     */
    int read(String uuid) throws IOException;

    /**
     * Reads from a file.
     * 
     * @param uuid
     *            The id of the file.
     * @param len
     *            the maximum number of bytes to read.
     * @return the buffer into which the data is read.
     * @throws IOException
     *             - If the first byte cannot be read for any reason other than end of file, or if
     *             the input stream has been closed, or if some other I/O error occurs.
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    byte[] read(String uuid, Integer len) throws IOException;

    /**
     * Skips over and discards bytes from a file.
     * 
     * @param uuid
     *            the id of the file.
     * @param n
     *            the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException
     *             - if the stream does not support seek, or if some other I/O error occurs.
     * 
     * @see java.io.InputStream#skip(long)
     */
    long skip(String uuid, Long n) throws IOException;

    /**
     * Closes a file.
     * 
     * @param uuid
     *            the id of the file.
     * 
     * @throws IOException
     *             - if an I/O error occurs.
     * 
     * @see java.io.InputStream#close()
     */
    void close(String uuid) throws IOException;
}
