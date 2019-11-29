/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.fileaccess.api;

import java.io.IOException;
import java.io.Serializable;

/**
 * 
 * Implementing classes provide access to remote files.
 * 
 * @author Doreen Seider
 */
public interface RemoteFileConnection extends Serializable {

    /**
     * Enumeration to decide between file on file system or in the RCE data management.
     */
    enum FileType {

        /**
         * File in RCE data management.
         */
        RCE_DM
    };

    /**
     * Reads from a file.
     * 
     * @param b the buffer into which the data is read.
     * @param off the start offset in array b at which the data is written.
     * @param len the maximum number of bytes to read.
     * 
     * @return the total number of bytes read into the buffer, or -1 if there is no more data
     *         because the end of the stream has been reached.
     * @throws IOException - If the first byte cannot be read for any reason other than end of file,
     *         or if the input stream has been closed, or if some other I/O error occurs.
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Reads from a file.
     * 
     * @return the next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException if an I/O error occurs.
     * 
     * @see java.io.InputStream#read()
     */
    int read() throws IOException;

    /**
     * Skips over and discards bytes from a file.
     * 
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException - if the stream does not support seek, or if some other I/O error occurs.
     * 
     * @see java.io.InputStream#skip(long)
     */
    long skip(long n) throws IOException;

    /**
     * Closes a file.
     * 
     * @throws IOException - if an I/O error occurs.
     * 
     * @see java.io.InputStream#close()
     */
    void close() throws IOException;

}
